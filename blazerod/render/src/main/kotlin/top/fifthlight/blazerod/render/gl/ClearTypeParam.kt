package top.fifthlight.blazerod.render.gl

import org.lwjgl.opengl.GL32C
import top.fifthlight.blazerod.extension.CommandEncoderExt
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ClearTypeParam(
    val internalFormat: Int,
    val format: Int,
    val type: Int,
    val data: ByteBuffer?,
    val align: Int,
) {
    ZERO_FILLED(
        internalFormat = GL32C.GL_R8UI,
        format = GL32C.GL_RED_INTEGER,
        type = GL32C.GL_UNSIGNED_BYTE,
        data = null,
        align = 1,
    ),
    BYTE_ONE_FILLED(
        internalFormat = GL32C.GL_R8UI,
        format = GL32C.GL_RED_INTEGER,
        type = GL32C.GL_UNSIGNED_BYTE,
        data = ByteBuffer.allocateDirect(1).order(ByteOrder.nativeOrder()).apply {
            put(0xFF.toByte())
            flip()
        },
        align = 1,
    ),
    FLOAT_ONE_FILLED(
        internalFormat = GL32C.GL_R32F,
        format = GL32C.GL_RED,
        type = GL32C.GL_FLOAT,
        data = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).apply {
            putFloat(0, 1f)
        },
        align = 4,
    );

    companion object {
        @JvmStatic
        fun fromClearType(clearType: CommandEncoderExt.ClearType) = when(clearType) {
            CommandEncoderExt.ClearType.ZERO_FILLED -> ZERO_FILLED
            CommandEncoderExt.ClearType.BYTE_ONE_FILLED -> BYTE_ONE_FILLED
            CommandEncoderExt.ClearType.FLOAT_ONE_FILLED -> FLOAT_ONE_FILLED
        }
    }
}
