package top.fifthlight.blazerod.util

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.GpuFence
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.util.Identifier
import java.util.TreeSet
import kotlin.AutoCloseable
import kotlin.Boolean
import kotlin.Int
import kotlin.also
import kotlin.collections.ArrayDeque
import kotlin.collections.MutableSet
import kotlin.collections.forEach
import kotlin.collections.mutableListOf
import kotlin.collections.mutableSetOf
import kotlin.error
import kotlin.require

sealed class GpuShaderDataPool : AutoCloseable {
    companion object {
        @JvmStatic
        fun create(alignment: Int, supportSlicing: Boolean) = if (supportSlicing) {
            Sliced(alignment = alignment)
        } else {
            Pooled()
        }
    }

    abstract val supportSlicing: Boolean

    abstract fun allocate(size: Int): GpuBufferSlice
    abstract fun rotate()

    class Sliced(
        initialCapacity: Int = 512 * 1024,
        private val alignment: Int,
    ) : GpuShaderDataPool() {
        override val supportSlicing: Boolean
            get() = true

        private val oldBuffers = mutableListOf<SlicedMappableRingBuffer>()
        private var size = 0
        private var capacity = initialCapacity
        private var buffer: SlicedMappableRingBuffer? = null

        private fun growCapacity(newCapacity: Int) {
            val buffer = buffer ?: error("Gpu shader data pool not initialized")
            val newBuffer = SlicedMappableRingBuffer(
                nameSupplier = { "Sliced GPU buffer pool" },
                usage = GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER or GpuBuffer.USAGE_MAP_WRITE,
                size = newCapacity,
                alignment = alignment,
            )
            oldBuffers.add(buffer)
            this.buffer = newBuffer
            size = 0
            capacity = newCapacity
        }

        override fun allocate(size: Int): GpuBufferSlice {
            require(size > 0) { "Size must be positive" }
            val buffer = buffer ?: run {
                // Initialize a new buffer
                capacity = maxOf(capacity, size)
                SlicedMappableRingBuffer(
                    nameSupplier = { "Sliced GPU buffer pool" },
                    usage = GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER or GpuBuffer.USAGE_MAP_WRITE,
                    size = capacity,
                    alignment = alignment,
                ).also {
                    this.buffer = it
                }
            }

            val offset = if (alignment == 0 || alignment == 1) {
                this.size
            } else {
                this.size roundUpToMultiple alignment
            }
            return if (offset + size > capacity) {
                growCapacity(maxOf(capacity * 2, size))
                this.size = size
                this.buffer!!.getBlocking().slice(0, size)
            } else {
                this.size = offset + size
                buffer.getBlocking().slice(offset, size)
            }
        }

        override fun rotate() {
            oldBuffers.forEach { it.close() }
            oldBuffers.clear()
            buffer?.rotate()
            size = 0
        }

        override fun close() {
            oldBuffers.forEach { it.close() }
            oldBuffers.clear()
            buffer?.close()
        }
    }

    class Pooled() : GpuShaderDataPool() {
        override val supportSlicing: Boolean
            get() = false

        class BufferItem private constructor() : AutoCloseable {
            private var _buffer: GpuBuffer? = null
            val buffer
                get() = _buffer ?: error("BufferItem without buffer")
            var bufferSize: Int = -1
                private set
            var lastUsedFrame: Int = -1
            private var recycled: Boolean = false

            companion object {
                private val POOL = ObjectPool(
                    identifier = Identifier.of("blazerod", "shader_data_buffer_item"),
                    create = ::BufferItem,
                    onAcquired = {
                        recycled = false
                    },
                    onReleased = {
                        _buffer = null
                        bufferSize = -1
                        lastUsedFrame = -1
                    }
                )

                fun acquire(buffer: GpuBuffer, lastUsedFrame: Int) = POOL.acquire().apply {
                    this._buffer = buffer
                    this.bufferSize = buffer.size()
                    this.lastUsedFrame = lastUsedFrame
                }

                fun acquire(bufferSize: Int) = POOL.acquire().apply {
                    this.bufferSize = bufferSize
                }
            }

            override fun close() {
                if (recycled) {
                    return
                }
                recycled = true
                _buffer?.close()
                POOL.release(this)
            }
        }

        companion object {
            private const val MAX_BUFFER_KEEP_FRAMES = 600
        }

        private val availableBuffers = TreeSet<BufferItem>(compareBy { it.bufferSize })

        private class FrameData(
            var fence: GpuFence? = null,
            val buffers: MutableSet<BufferItem> = mutableSetOf(),
        )

        private val allFrameData = ArrayDeque<FrameData>().also { it.add(FrameData()) }
        private val waitingFrameData = ArrayDeque<FrameData>()

        private var currentFrame: Int = 0
        private fun getFrameData(frameNumber: Int) = allFrameData.getOrNull(allFrameData.lastIndex - (frameNumber - currentFrame))

        override fun allocate(size: Int): GpuBufferSlice {
            require(size > 0) { "Size must be positive" }
            val frameData = getFrameData(currentFrame) ?: error("No frame data of frame $currentFrame, this should not happen!")

            val foundBuffer = BufferItem.acquire(size).use {
                availableBuffers.ceiling(it)
            }
            val allocatedBuffer = if (foundBuffer != null) {
                availableBuffers.remove(foundBuffer)
                val frame = getFrameData(foundBuffer.lastUsedFrame)
                frame?.buffers?.remove(foundBuffer)
                foundBuffer.lastUsedFrame = currentFrame
                foundBuffer
            } else {
                BufferItem.acquire(
                    buffer = RenderSystem.getDevice().createBuffer(
                        { "Pooled GPU buffer" },
                        GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER or GpuBuffer.USAGE_MAP_WRITE,
                        size
                    ),
                    lastUsedFrame = currentFrame,
                )
            }

            frameData.buffers.add(allocatedBuffer)
            return allocatedBuffer.buffer.slice(0, size)
        }

        override fun rotate() {
            // Insert a fence at current frame
            allFrameData.lastOrNull()?.let {
                it.fence = RenderSystem.getDevice().createCommandEncoder().createFence()
                waitingFrameData.addLast(it)
            }

            // Advance current frame
            currentFrame++
            FrameData().also { allFrameData.addLast(it) }

            // Wait fences and add buffer to available
            while (waitingFrameData.size >= 3) {
                val frameData = waitingFrameData.removeFirst()
                frameData.fence?.use {
                    it.awaitCompletion(Long.MAX_VALUE)
                    it.close()
                    frameData.fence = null
                }
                availableBuffers.addAll(frameData.buffers)
            }

            // Clean oldest frame data
            while (allFrameData.size > MAX_BUFFER_KEEP_FRAMES) {
                val frameData = allFrameData.removeFirst()
                frameData.fence?.close()
                frameData.buffers.forEach { it.close() }
                frameData.buffers.clear()
            }
        }

        override fun close() {
            allFrameData.forEach { frameData ->
                frameData.buffers.forEach { it.close() }
                frameData.fence?.close()
            }
            allFrameData.clear()

            availableBuffers.forEach { it.close() }
            availableBuffers.clear()
        }
    }
}