package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.extension.BufferTypeExt
import top.fifthlight.armorstand.util.ObjectPool
import java.nio.ByteBuffer
import java.nio.ByteOrder

class InstanceDataBuffer : AutoCloseable {
    private var recycled: Boolean = true

    private var dataBuffer: ByteBuffer? = null
    private var gpuBuffer: GpuBuffer? = null

    fun upload(device: GpuDevice, commandEncoder: CommandEncoder, tasks: List<RenderTask.Primitive>) {
        if (RenderPassImpl.IS_DEVELOPMENT) {
            require(!recycled) { "Using recycled InstanceDataBuffer" }
            require(tasks.isNotEmpty()) { "Tasks is empty" }
        }

        // TODO: split instances to group?
        val totalSize = (tasks.size * ITEM_SIZE).coerceAtLeast(MIN_SIZE)
        val dataBuffer = (dataBuffer
            ?.takeIf { it.capacity() >= totalSize }
            ?: ByteBuffer.allocateDirect(totalSize).also {
                it.order(ByteOrder.nativeOrder())
                dataBuffer = it
            }).also {
                it.clear()
                it.limit(totalSize)
            }

        for ((index, task) in tasks.withIndex()) {
            val baseOffset = index * ITEM_SIZE
            task.modelViewProjMatrix.get(baseOffset, dataBuffer)
            val light = task.light
            dataBuffer.putInt(baseOffset + 64, light and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F))
            dataBuffer.putInt(baseOffset + 68, (light shr 16) and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F))
        }

        gpuBuffer?.takeIf { it.size() >= totalSize }?.let {
            commandEncoder.writeToBuffer(gpuBuffer!!, dataBuffer, 0)
        } ?: run {
            val newBuffer = device.createBuffer(
                { "Instance buffer" },
                BufferTypeExt.TEXTURE_BUFFER,
                BufferUsage.DYNAMIC_WRITE,
                dataBuffer,
            )
            gpuBuffer?.close()
            gpuBuffer = newBuffer
        }
    }

    fun getBuffer(): GpuBuffer = requireNotNull(gpuBuffer) { "Buffer is null, did you upload it?" }

    override fun close() {
        POOL.release(this)
    }

    companion object {
        private const val ITEM_SIZE = 80
        private val MIN_SIZE = ArmorStandClient.INSTANCE_SIZE * ITEM_SIZE

        private val POOL = ObjectPool<InstanceDataBuffer>(
            identifier = Identifier.of("armorstand", "instance_data"),
            create = ::InstanceDataBuffer,
            onAcquired = {
                recycled = false
            },
            onReleased = {
                recycled = true
            },
        )

        fun acquire() = POOL.acquire()
    }
}