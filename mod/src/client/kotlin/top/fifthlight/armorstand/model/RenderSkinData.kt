package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import top.fifthlight.armorstand.helper.BufferTypeExt
import top.fifthlight.armorstand.render.GpuTextureBuffer
import top.fifthlight.armorstand.render.TextureBufferFormat
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.createTextureBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RenderSkinData(
    val skin: RenderSkin,
) : AbstractRefCount() {
    companion object {
        private val IDENTITY = Matrix4f()
        private val TYPE_ID = Identifier.of("armorstand", "skin_data")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    private val matricesBuffer = ByteBuffer.allocateDirect(skin.jointSize * 16 * 4).apply {
        order(ByteOrder.nativeOrder())
        var position = 0
        repeat(skin.jointSize) {
            IDENTITY.get(this)
            position += 16 * 4
            position(position)
        }
        flip()
    }

    private var dirty = true

    private var gpuBuffer: GpuBuffer? = null
    private var textureBuffer: GpuTextureBuffer? = null

    fun setMatrix(index: Int, matrix4f: Matrix4f) {
        dirty = true
        matricesBuffer.position(index * 16 * 4)
        matrix4f.get(matricesBuffer)
    }

    fun upload(device: GpuDevice, commandEncoder: CommandEncoder) {
        if (!dirty) {
            return
        }
        dirty = false
        matricesBuffer.position(0)
        if (textureBuffer == null) {
            val buffer = device.createBuffer(
                { "Skin matrix buffer for ${skin.name}" },
                BufferTypeExt.TEXTURE_BUFFER,
                BufferUsage.DYNAMIC_WRITE,
                matricesBuffer,
            )
            gpuBuffer = buffer
            device.createTextureBuffer(
                "Skin matrix texture buffer for ${skin.name}",
                TextureBufferFormat.RGBA32F,
                buffer
            ).also {
                textureBuffer = it
            }
        } else {
            commandEncoder.writeToBuffer(gpuBuffer!!, matricesBuffer, 0)
        }
    }

    fun getBuffer(): GpuTextureBuffer {
        require(!dirty) { "Skin data is dirty, please upload before getting buffer!" }
        return requireNotNull(textureBuffer) { "Texture buffer is null, did you upload it?" }
    }

    override fun onClosed() {
        textureBuffer?.close()
        gpuBuffer?.close()
    }
}