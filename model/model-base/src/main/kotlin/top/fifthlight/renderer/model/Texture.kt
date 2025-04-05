package top.fifthlight.renderer.model

data class Texture(
    val name: String? = null,
    val bufferView: BufferView? = null,
    val type: TextureType? = null,
    val sampler: Sampler,
) {
    enum class TextureType(
        val mimeType: String,
    ) {
        PNG("image/png"),
        JPEG("image/jpeg"),
    }

    data class Sampler(
        val magFilter: MagFilter = MagFilter.LINEAR,
        val minFilter: MinFilter = MinFilter.NEAREST,
        val wrapS: WrapMode = WrapMode.REPEAT,
        val wrapT: WrapMode = WrapMode.REPEAT,
    ) {
        enum class MagFilter {
            NEAREST,
            LINEAR
        }

        enum class MinFilter {
            NEAREST,
            LINEAR,
            NEAREST_MIPMAP_NEAREST,
            LINEAR_MIPMAP_NEAREST,
            NEAREST_MIPMAP_LINEAR,
            LINEAR_MIPMAP_LINEAR,
        }

        enum class WrapMode {
            REPEAT,
            MIRRORED_REPEAT,
            CLAMP_TO_EDGE
        }
    }
}