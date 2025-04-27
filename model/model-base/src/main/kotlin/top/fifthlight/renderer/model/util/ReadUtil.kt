package top.fifthlight.renderer.model.util

import java.nio.BufferOverflowException
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

inline fun ByteBuffer.withRelativeLimit(relativeLimit: Int, crossinline func: () -> Unit) {
    val oldLimit = limit()
    val newLimit = position() + relativeLimit
    if (newLimit > oldLimit) {
        throw BufferOverflowException()
    }
    limit(newLimit)
    try {
        func()
    } finally {
        limit(oldLimit)
    }
}

fun ByteBuffer.getUByteNormalized() = get().toFloat() / 255f
fun ByteBuffer.getSByteNormalized() = (get().toFloat() / 127f).coerceAtLeast(-1f)
fun ByteBuffer.getUShortNormalized() = getShort().toFloat() / 65535f
fun ByteBuffer.getSShortNormalized() = (getShort().toFloat() / 32767f).coerceAtLeast(-1f)
