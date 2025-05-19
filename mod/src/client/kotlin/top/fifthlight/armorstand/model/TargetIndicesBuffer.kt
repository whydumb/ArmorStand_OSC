package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import it.unimi.dsi.fastutil.ints.IntIterator
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.extension.BufferTypeExt
import top.fifthlight.armorstand.util.FramedObjectPool
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TargetIndicesBuffer : AutoCloseable {
    private var recycled: Boolean = true
    private var indicesBuffer: ByteBuffer? = null
    private var gpuBuffer: GpuBuffer? = null

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

        val indicesBuffer = (this@TargetIndicesBuffer.indicesBuffer
            ?: ByteBuffer.allocateDirect(TOTAL_SIZE).also {
                it.order(ByteOrder.nativeOrder())
                indicesBuffer = it
            }).also {
            it.clear()
        }

        fun ByteBuffer.padInt() {
            position(position() + 4)
        }

        for (weight in targetWeights) {
            indicesBuffer.putInt(weight.position.enabledIndices.size)
            indicesBuffer.putInt(weight.color.enabledIndices.size)
            indicesBuffer.putInt(weight.texCoord.enabledIndices.size)
            indicesBuffer.padInt()
            val positionIterator = weight.position.enabledIndices.iterator()
            val colorIterator = weight.color.enabledIndices.iterator()
            val texCoordIterator = weight.texCoord.enabledIndices.iterator()
            repeat(ArmorStandClient.MAX_ENABLED_MORPH_TARGETS) {
                fun IntIterator.write() {
                    if (hasNext()) {
                        indicesBuffer.putInt(nextInt())
                    } else {
                        indicesBuffer.padInt()
                    }
                }
                positionIterator.write()
                colorIterator.write()
                texCoordIterator.write()
                indicesBuffer.padInt()
            }
        }
        indicesBuffer.clear()

        gpuBuffer?.let {
            commandEncoder.writeToBuffer(gpuBuffer!!, indicesBuffer, 0)
        } ?: run {
            val newBuffer = device.createBuffer(
                { "Merged target indices buffer" },
                BufferTypeExt.TEXTURE_BUFFER,
                BufferUsage.DYNAMIC_WRITE,
                indicesBuffer,
            )
            gpuBuffer?.close()
            gpuBuffer = newBuffer
        }
    }

    fun getBuffer(): GpuBuffer = requireNotNull(gpuBuffer) { "Target indices buffer is null, did you upload it?" }

    override fun close() {
        POOL.release(this)
    }

    companion object {
        private const val TOTAL_SIZE =
            (16 + ArmorStandClient.MAX_ENABLED_MORPH_TARGETS * 16) * ArmorStandClient.INSTANCE_SIZE

        private val POOL = FramedObjectPool<TargetIndicesBuffer>(
            identifier = Identifier.of("armorstand", "target_indices_buffer"),
            create = ::TargetIndicesBuffer,
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