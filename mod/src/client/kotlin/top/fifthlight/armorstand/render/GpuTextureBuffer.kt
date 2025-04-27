package top.fifthlight.armorstand.render

interface GpuTextureBuffer: AutoCloseable {
    val format: TextureBufferFormat
    val label: String?
    val closed: Boolean
    override fun close()
}