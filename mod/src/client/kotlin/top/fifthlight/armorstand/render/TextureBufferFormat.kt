package top.fifthlight.armorstand.render

enum class TextureBufferFormat(
    val channels: Int,
    val channelSize: Int,
    val normalize: Boolean,
) {
    R8I(1, 1, true),
    R16I(1, 2, true),
    R32I(1, 4, true),
    R8UI(1, 1, false),
    R16UI(1, 2, false),
    R32UI(1, 4, false),
    R16F(1, 2, false),
    R32F(1, 4, false),
    RG8I(2, 1, true),
    RG16I(2, 2, true),
    RG32I(2, 4, true),
    RG8UI(2, 1, false),
    RG16UI(2, 2, false),
    RG32UI(2, 4, false),
    RG16F(2, 2, false),
    RG32F(2, 4, false),
    RGB8I(3, 1, true),
    RGB16I(3, 2, true),
    RGB32I(3, 4, true),
    RGB8UI(3, 1, false),
    RGB16UI(3, 2, false),
    RGB32UI(3, 4, false),
    RGB16F(3, 2, false),
    RGB32F(3, 4, false),
    RGBA8I(4, 1, true),
    RGBA16I(4, 2, true),
    RGBA32I(4, 4, true),
    RGBA8UI(4, 1, false),
    RGBA16UI(4, 2, false),
    RGBA32UI(4, 4, false),
    RGBA16F(4, 2, false),
    RGBA32F(4, 4, false);

    val pixelSize = channels * channelSize
}
