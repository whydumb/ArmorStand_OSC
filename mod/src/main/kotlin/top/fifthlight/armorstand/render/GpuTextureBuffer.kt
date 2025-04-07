package top.fifthlight.armorstand.render

abstract class GpuTextureBuffer {
    abstract val format: TextureBufferFormat
    abstract val label: String?
    abstract val closed: Boolean
    abstract fun close()
}