package top.fifthlight.armorstand.render.gl

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.textures.TextureFormat
import top.fifthlight.armorstand.render.GpuTextureBuffer

class GlTextureBuffer(
    val glId: Int,
    override val label: String?,
    override val format: TextureFormat,
): GpuTextureBuffer() {
    override var closed: Boolean = false
        private set

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        GlStateManager._deleteTexture(glId)
    }
}