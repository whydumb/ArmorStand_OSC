package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.extension.BufferTypeExt
import top.fifthlight.armorstand.extension.createTextureBuffer
import top.fifthlight.armorstand.render.GpuTextureBuffer
import top.fifthlight.armorstand.render.TextureBufferFormat
import top.fifthlight.armorstand.util.FramedObjectPool
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TargetWeightsBuffer : AutoCloseable {
    private var recycled: Boolean = true
    private var weightsBuffer: ByteBuffer? = null
    private var gpuBuffer: GpuBuffer? = null
    private var textureBuffer: GpuTextureBuffer? = null

    fun upload(device: GpuDevice, commandEncoder: CommandEncoder, targetWeights: List<RenderPrimitive.TargetWeights>) {
        if (RenderPassImpl.IS_DEVELOPMENT) {
            require(!recycled) { "Using recycled TargetWeightsBuffer" }
            require(targetWeights.isNotEmpty()) { "Target weights is empty" }
            require(targetWeights.all {
                it.position.size == targetWeights.first().position.size &&
                        it.color.size == targetWeights.first().color.size &&
                        it.texCoord.size == targetWeights.first().texCoord.size
            }) { "Target weights size not correct" }
        }
        val totalSize = targetWeights.first().let { it.position.size + it.color.size + it.texCoord.size } * 4 * targetWeights.size

        val weightsBuffer = (this@TargetWeightsBuffer.weightsBuffer
            ?.takeIf { it.capacity() >= totalSize }
            ?: ByteBuffer.allocateDirect(totalSize).also {
                it.order(ByteOrder.nativeOrder())
                weightsBuffer = it
            }).also {
            it.clear()
            it.limit(totalSize)
        }
        for (weights in targetWeights) {
            fun RenderPrimitive.TargetWeight.put() {
                buffer.clear()
                weightsBuffer.put(buffer)
                buffer.clear()
            }
            weights.position.put()
            weights.color.put()
            weights.texCoord.put()
        }
        if (RenderPassImpl.IS_DEVELOPMENT) {
            require(!weightsBuffer.hasRemaining()) { "Still has remaining space for weights buffer" }
        }
        weightsBuffer.clear()

        gpuBuffer?.takeIf { it.size() >= totalSize }?.let {
            commandEncoder.writeToBuffer(gpuBuffer!!, weightsBuffer, 0)
        } ?: run {
            val newBuffer = device.createBuffer(
                { "Merged target weights buffer" },
                BufferTypeExt.TEXTURE_BUFFER,
                BufferUsage.DYNAMIC_WRITE,
                weightsBuffer,
            )
            gpuBuffer?.close()
            textureBuffer?.close()
            gpuBuffer = newBuffer
            textureBuffer = device.createTextureBuffer(
                label = "Merged target weights texture buffer",
                format = TextureBufferFormat.R32F,
                buffer = newBuffer,
            )
        }
    }

    fun getBuffer(): GpuTextureBuffer = requireNotNull(textureBuffer) { "Target weights buffer is null, did you upload it?" }

    override fun close() {
        POOL.release(this)
    }

    companion object {
        private val POOL = FramedObjectPool<TargetWeightsBuffer>(
            identifier = Identifier.of("armorstand", "target_weights_buffer"),
            create = ::TargetWeightsBuffer,
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