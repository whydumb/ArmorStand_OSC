package top.fifthlight.blazerod.util

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.extension.copyBuffer
import java.lang.AutoCloseable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.max

class SlottedGpuBuffer(
    initialCapacity: Int = 1,
    val itemSize: Int,
    private val label: String?,
    private val usage: Int,
) : AutoCloseable {
    init {
        require(initialCapacity > 0) { "Capacity should be larger than 0" }
        require(usage and GpuBuffer.USAGE_COPY_DST != 0) { "Buffer should have USAGE_COPY_DST bit set" }
        require(usage and GpuBuffer.USAGE_COPY_SRC != 0) { "Buffer should have USAGE_COPY_SRC bit set" }
        require(usage and GpuBuffer.USAGE_MAP_READ != 0) { "Buffer should have USAGE_MAP_READ bit set" }
        require(usage and GpuBuffer.USAGE_MAP_WRITE != 0) { "Buffer should have USAGE_MAP_WRITE bit set" }
    }

    private var capacity = initialCapacity
    private var highestSlot = 0
    private val allocatedSlotBitmap = BitSet(initialCapacity)
    private val freeSlots = IntHeapPriorityQueue(initialCapacity)
    private var gpuBuffer =
        RenderSystem.getDevice().createBuffer(label?.let { { it } }, usage, initialCapacity * itemSize)
    private var mappedBuffer: GpuBuffer.MappedView? = null

    private fun enlargeBuffer(newCapacity: Int) {
        if (newCapacity <= capacity) {
            throw IllegalStateException("Unable to shrink buffer")
        }
        capacity = newCapacity
        val newSize = newCapacity * itemSize

        mappedBuffer?.close()
        mappedBuffer = null

        val device = RenderSystem.getDevice()
        val newBuffer = device.createBuffer(label?.let { { it } }, usage, newSize)
        val commandEncoder = device.createCommandEncoder()
        commandEncoder.copyBuffer(newBuffer.slice(0, gpuBuffer.size), gpuBuffer.slice())
        gpuBuffer.close()
        gpuBuffer = newBuffer
    }

    private fun editSlot(index: Int, func: ByteBuffer.() -> Unit) {
        val mappedBuffer = this.mappedBuffer ?: run {
            val device = RenderSystem.getDevice()
            val commandEncoder = device.createCommandEncoder()
            commandEncoder.mapBuffer(gpuBuffer, false, true).also { this.mappedBuffer = it }
        }

        val startPos = index * itemSize
        val buffer = mappedBuffer.data().slice(startPos, itemSize).order(ByteOrder.nativeOrder())
        func(buffer)
    }

    private fun allocateSlotIndex() = if (freeSlots.isEmpty) {
        highestSlot++
    } else {
        freeSlots.dequeueInt()
    }.also { index ->
        highestSlot = max(highestSlot, index)
        allocatedSlotBitmap.set(index)
        if (index >= capacity) {
            enlargeBuffer(capacity * 2)
        }
    }

    private fun freeSlotIndex(index: Int) = require(allocatedSlotBitmap.get(index)) {
        "Trying to free unallocated slot $index"
    }.also {
        allocatedSlotBitmap.clear(index)
        freeSlots.enqueue(index)
    }

    sealed interface Slot : AutoCloseable {
        val size: Int
        fun edit(block: ByteBuffer.() -> Unit)
    }

    class Slotted private constructor(var index: Int = -1) : Slot {
        private var _buffer: SlottedGpuBuffer? = null
        var buffer
            get() = _buffer ?: error("Buffer is not initialized or recycled")
            set(value) {
                _buffer = value
            }

        private var recycled: Boolean = false

        override val size: Int
            get() = buffer.itemSize

        override fun edit(block: ByteBuffer.() -> Unit) = buffer.editSlot(index, block)

        override fun close() {
            if (recycled) {
                return
            }
            recycled = true
            buffer.freeSlotIndex(index)
            POOL.release(this)
        }

        companion object {
            val POOL = ObjectPool<Slotted>(
                identifier = Identifier.of("blazerod", "gpu_buffer_slot"),
                create = ::Slotted,
                onAcquired = { recycled = false },
            )
        }
    }

    class Unslotted(
        override val size: Int,
        usage: Int,
    ) : Slot {
        private val gpuBuffer: GpuBuffer = RenderSystem.getDevice().createBuffer(
            { "Skin data buffer" },
            usage,
            size
        )
        private var mappedBuffer: GpuBuffer.MappedView? = null

        override fun edit(block: ByteBuffer.() -> Unit) {
            val device = RenderSystem.getDevice()
            val commandEncoder = device.createCommandEncoder()
            val mappedBuffer = commandEncoder.mapBuffer(gpuBuffer, false, true).also { this.mappedBuffer = it }
            val buffer = mappedBuffer.data().order(ByteOrder.nativeOrder())
            buffer.clear()
            block(buffer)
        }

        fun getBuffer() = gpuBuffer.also {
            mappedBuffer?.let {
                it.close()
                mappedBuffer = null
            }
        }

        override fun close() {
            mappedBuffer?.close()
            mappedBuffer = null
            gpuBuffer.close()
        }
    }

    fun allocateSlot(): Slot = allocateSlotIndex().let {
        Slotted.POOL.acquire().apply {
            buffer = this@SlottedGpuBuffer
            index = it
        }
    }

    fun getBuffer(): GpuBuffer {
        mappedBuffer?.close()
        mappedBuffer = null
        return gpuBuffer
    }

    override fun close() {
        if (RenderPassImpl.IS_DEVELOPMENT) {
            check(allocatedSlotBitmap.isEmpty) { "SlottedGpuBuffer closed with item not freed" }
        }
        mappedBuffer?.close()
        mappedBuffer = null
        gpuBuffer.close()
    }
}