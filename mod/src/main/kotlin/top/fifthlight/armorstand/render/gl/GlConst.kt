package top.fifthlight.armorstand.render.gl

import org.lwjgl.opengl.GL30C
import top.fifthlight.armorstand.render.TextureBufferFormat

fun TextureBufferFormat.toGlInternal() = when(this) {
    TextureBufferFormat.R8I -> GL30C.GL_R8I
    TextureBufferFormat.R16I -> GL30C.GL_R16I
    TextureBufferFormat.R32I -> GL30C.GL_R32I
    TextureBufferFormat.R8UI -> GL30C.GL_R8UI
    TextureBufferFormat.R16UI -> GL30C.GL_R16UI
    TextureBufferFormat.R32UI -> GL30C.GL_R32UI
    TextureBufferFormat.R16F -> GL30C.GL_R16F
    TextureBufferFormat.R32F -> GL30C.GL_R32F
    TextureBufferFormat.RG8I -> GL30C.GL_RG8I
    TextureBufferFormat.RG16I -> GL30C.GL_RG16I
    TextureBufferFormat.RG32I -> GL30C.GL_RG32I
    TextureBufferFormat.RG8UI -> GL30C.GL_RG8UI
    TextureBufferFormat.RG16UI -> GL30C.GL_RG16UI
    TextureBufferFormat.RG32UI -> GL30C.GL_RG32UI
    TextureBufferFormat.RG16F -> GL30C.GL_RG16F
    TextureBufferFormat.RG32F -> GL30C.GL_RG32F
    TextureBufferFormat.RGB8I -> GL30C.GL_RGB8I
    TextureBufferFormat.RGB16I -> GL30C.GL_RGB16I
    TextureBufferFormat.RGB32I -> GL30C.GL_RGB32I
    TextureBufferFormat.RGB8UI -> GL30C.GL_RGB8UI
    TextureBufferFormat.RGB16UI -> GL30C.GL_RGB16UI
    TextureBufferFormat.RGB32UI -> GL30C.GL_RGB32UI
    TextureBufferFormat.RGB16F -> GL30C.GL_RGB16F
    TextureBufferFormat.RGB32F -> GL30C.GL_RGB32F
    TextureBufferFormat.RGBA8I -> GL30C.GL_RGBA8I
    TextureBufferFormat.RGBA16I -> GL30C.GL_RGBA16I
    TextureBufferFormat.RGBA32I -> GL30C.GL_RGBA32I
    TextureBufferFormat.RGBA8UI -> GL30C.GL_RGBA8UI
    TextureBufferFormat.RGBA16UI -> GL30C.GL_RGBA16UI
    TextureBufferFormat.RGBA32UI -> GL30C.GL_RGBA32UI
    TextureBufferFormat.RGBA16F -> GL30C.GL_RGBA16F
    TextureBufferFormat.RGBA32F -> GL30C.GL_RGBA32F
}
