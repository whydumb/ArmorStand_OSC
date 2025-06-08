package top.fifthlight.blazerod.render.gl

import org.lwjgl.system.MemoryUtil
import top.fifthlight.blazerod.extension.CommandEncoderExt
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BufferClearer {
    @JvmStatic
    fun clear(clearType: CommandEncoderExt.ClearType, buffer: ByteBuffer) {
        when (clearType) {
            CommandEncoderExt.ClearType.ZERO_FILLED -> {
                MemoryUtil.memSet(buffer, 0)
            }

            CommandEncoderExt.ClearType.BYTE_ONE_FILLED -> {
                MemoryUtil.memSet(buffer, 0xFF)
            }

            CommandEncoderExt.ClearType.FLOAT_ONE_FILLED -> {
                require(buffer.remaining() % 4 == 0) { "Bad required byte length: ${buffer.remaining()}" }
                buffer.order(ByteOrder.nativeOrder())
                var index = buffer.position()
                repeat(buffer.remaining() / 4) {
                    buffer.putFloat(index, 1f)
                    index += 4
                }
            }
        }
    }
}