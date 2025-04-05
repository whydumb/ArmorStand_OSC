package top.fifthlight.armorstand.render

import com.mojang.blaze3d.textures.TextureFormat

abstract class GpuTextureBuffer {
    abstract val format: TextureFormat
    abstract val label: String?
    abstract val closed: Boolean
    abstract fun close()
}