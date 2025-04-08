package top.fifthlight.renderer.model.util

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

fun ReadableByteChannel.readAll(buffer: ByteBuffer): Int {
    var length = 0
    while (buffer.hasRemaining()) {
        when (val len = read(buffer)) {
            -1 -> break
            0 -> throw IllegalStateException("Buffer is not blocking")
            else -> {
                length += len
            }
        }
    }
    return length
}