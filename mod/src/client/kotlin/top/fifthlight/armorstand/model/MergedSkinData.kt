package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.extension.BufferTypeExt
import top.fifthlight.armorstand.extension.createTextureBuffer
import top.fifthlight.armorstand.model.RenderSkinData.Companion.MAT4X4_SIZE
import top.fifthlight.armorstand.render.GpuTextureBuffer
import top.fifthlight.armorstand.render.TextureBufferFormat
import top.fifthlight.armorstand.util.FramedObjectPool
import top.fifthlight.renderer.model.util.putWorkaround
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MergedSkinData : AutoCloseable {
    private var recycled: Boolean = true
    private var gpuBuffer: GpuBuffer? = null
    private var textureBuffer: GpuTextureBuffer? = null
    private var matricesBuffer: ByteBuffer? = null

    fun upload(device: GpuDevice, commandEncoder: CommandEncoder, skinData: List<RenderSkinData>) {
        if (RenderPassImpl.IS_DEVELOPMENT) {
            require(!recycled) { "Using recycled MergedSkinData" }
            require(skinData.isNotEmpty()) { "Skin data is empty" }
            require(skinData.all { it.skin.jointSize == skinData.first().skin.jointSize }) { "Skin data size not correct" }
        }
        val joints = skinData.first().skin.jointSize
        val jointSize = joints * MAT4X4_SIZE
        val totalSize = jointSize * skinData.size

        val matricesBuffer = (matricesBuffer
            ?.takeIf { it.capacity() >= totalSize }
            ?: ByteBuffer.allocateDirect(totalSize).also {
                it.order(ByteOrder.nativeOrder())
                matricesBuffer = it
            }).also {
                it.clear()
                it.limit(totalSize)
            }
        for ((index, skin) in skinData.withIndex()) {
            matricesBuffer.putWorkaround(index * jointSize, skin.matricesBuffer, 0, jointSize)
        }

        gpuBuffer?.takeIf { it.size() >= totalSize }?.let {
            commandEncoder.writeToBuffer(gpuBuffer!!, matricesBuffer, 0)
        } ?: run {
            val newBuffer = device.createBuffer(
                { "Merged skin matrix buffer" },
                BufferTypeExt.TEXTURE_BUFFER,
                BufferUsage.DYNAMIC_WRITE,
                matricesBuffer,
            )
            gpuBuffer?.close()
            textureBuffer?.close()
            gpuBuffer = newBuffer
            textureBuffer = device.createTextureBuffer(
                label = "Merged skin matrix texture buffer",
                format = TextureBufferFormat.RGBA32F,
                buffer = newBuffer,
            )
        }
    }

    fun getBuffer(): GpuTextureBuffer = requireNotNull(textureBuffer) { "Texture buffer is null, did you upload it?" }

    override fun close() {
        POOL.release(this)
    }

    companion object {
        private val POOL = FramedObjectPool<MergedSkinData>(
            identifier = Identifier.of("armorstand", "merged_skin_data"),
            create = ::MergedSkinData,
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