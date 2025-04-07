package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
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
    }

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

    private var dirty = false

    private var gpuBuffer: GpuBuffer? = null
    private var textureBuffer: GpuTextureBuffer? = null

    fun setMatrix(index: Int, matrix4f: Matrix4f) {
        matricesBuffer.position(index * 16 * 4)
        matrix4f.get(matricesBuffer)
    }

    fun getBuffer(device: GpuDevice, commandEncoder: CommandEncoder): GpuTextureBuffer = textureBuffer?.let {
        matricesBuffer.position(0)
        if (dirty) {
            commandEncoder.writeToBuffer(gpuBuffer!!, matricesBuffer, 0)
            dirty = false
        }
        textureBuffer
    } ?: run {
        dirty = false
        matricesBuffer.position(0)
        val buffer = device.createBuffer(
            { "Skin matrix buffer for ${skin.name}" },
            BufferTypeExt.TEXTURE_BUFFER,
            BufferUsage.DYNAMIC_WRITE,
            matricesBuffer,
        )
        gpuBuffer = buffer
        device.createTextureBuffer("Skin matrix texture buffer for ${skin.name}", TextureBufferFormat.RGBA32F, buffer).also {
            textureBuffer = it
        }
    }
}