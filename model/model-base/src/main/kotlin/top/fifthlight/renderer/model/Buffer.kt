package top.fifthlight.renderer.model

import java.nio.ByteBuffer

class Buffer(
    val name: String? = null,
    val buffer: ByteBuffer,
) {
    init {
        require(buffer.isDirect) { "Buffer content for $name is not direct buffer!" }
    }
}