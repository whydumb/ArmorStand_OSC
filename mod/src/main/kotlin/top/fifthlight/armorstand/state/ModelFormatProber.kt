package top.fifthlight.armorstand.state

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object ModelFormatProber {
    enum class Result {
        GLTF_BINARY,
        PMX,
        LIKELY_GLTF_TEXT, // JSON
        UNKNOWN
    }

    private val GLTF_BINARY_MAGIC = byteArrayOf(0x67, 0x6C, 0x54, 0x46)
    private val PMX_SIGNATURE = byteArrayOf(0x50, 0x4D, 0x58, 0x20)

    fun probe(path: Path): Result {
        return FileChannel.open(path, StandardOpenOption.READ).use { channel ->
            val buffer = ByteBuffer.allocate(32)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            channel.read(buffer)
            buffer.flip()

            when {
                isGltfBinary(buffer) -> Result.GLTF_BINARY
                isPmx(buffer) -> Result.PMX
                isLikelyJson(buffer) -> Result.LIKELY_GLTF_TEXT
                else -> Result.UNKNOWN
            }
        }
    }

    private fun isGltfBinary(buffer: ByteBuffer): Boolean {
        buffer.position(0)
        if (buffer.remaining() < 4) return false
        val magicBytes = ByteArray(4)
        buffer.get(magicBytes, 0, 4)
        return magicBytes.contentEquals(GLTF_BINARY_MAGIC)
    }

    private fun isPmx(buffer: ByteBuffer): Boolean {
        buffer.position(0)
        if (buffer.remaining() < 4) return false
        val signatureBytes = ByteArray(4)
        buffer.get(signatureBytes, 0, 4)
        return signatureBytes.contentEquals(PMX_SIGNATURE)
    }

    private fun isLikelyJson(buffer: ByteBuffer): Boolean {
        buffer.position(0)
        while (buffer.hasRemaining()) {
            val ch = buffer.get()
            return when (ch.toInt().toChar()) {
                ' ', '\t', '\n', '\r' -> continue
                '{' -> true // JSON starts with {
                else -> false
            }
        }
        return false
    }
}
