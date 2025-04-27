package top.fifthlight.renderer.model.gltf

import top.fifthlight.renderer.model.ModelFileLoader
import top.fifthlight.renderer.model.util.readAll
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object GltfBinaryLoader: ModelFileLoader {
    override val extensions = listOf("glb", "vrm")
    override val abilities = listOf(ModelFileLoader.Ability.MODEL, ModelFileLoader.Ability.ANIMATION)

    private const val GLTF_BINARY_MAGIC = 0x46546c67
    override val probeLength = 4
    override fun probe(buffer: ByteBuffer): Boolean {
        if (buffer.remaining() < 4) return false
        val slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        return slice.getInt() == 0x46546c67
    }

    private enum class ChunkType(
        val id: UInt?,
    ) {
        JSON(0x4E4F534Au),
        BINARY(0x004E4942u),
        UNKNOWN(null),
    }

    override fun load(path: Path, basePath: Path) = FileChannel.open(path, StandardOpenOption.READ).use { channel ->
        val readBuffer = ByteBuffer.allocate(16)
        readBuffer.order(ByteOrder.LITTLE_ENDIAN)

        fun readBytes(
            len: Int,
            fail: (Int) -> String,
        ) {
            readBuffer.clear()
            readBuffer.limit(len)
            channel.readAll(readBuffer)
            if (readBuffer.limit() < len) {
                throw GltfLoadException(fail(readBuffer.limit()))
            }
            readBuffer.flip()
        }

        readBytes(4) { "Want to read 4 bytes of magic, but only got $it bytes" }
        val magic = readBuffer.getInt()
        if (magic != 0x46546c67) {
            throw GltfLoadException("Bad magic: want 0x46546c67, but got 0x${magic.toString(16).padStart(8, '0')}")
        }

        readBytes(8) { "Bad GLTF binary header" }
        val version = readBuffer.getInt()
        if (version != 2) {
            throw GltfLoadException("Bad GLTF version: want 2, but get $version")
        }
        val totalLength = readBuffer.getInt().toUInt()
        var readLength = 0u

        fun readChunk(wantedType: ChunkType, sizeLimit: Int): ByteBuffer? {
            if (readLength + 8u > totalLength) {
                throw GltfLoadException("No available space for chunk header: current read $readLength, total length $totalLength")
            }
            readBuffer.clear()
            readBuffer.limit(8)
            channel.readAll(readBuffer)
            when (readBuffer.limit()) {
                0 -> return null
                8 -> Unit
                else -> throw GltfLoadException("Bad chunk header: want 8 bytes, but only read ${readBuffer.limit()} bytes")
            }
            readBuffer.flip()
            readLength += 8u
            val length = readBuffer.getInt().toUInt()
            val type = readBuffer.getInt().toUInt()
            if (readLength + length > totalLength) {
                throw GltfLoadException("Bad chunk length: total length is $totalLength, current location is at $readLength, but want to read $length")
            }
            readLength += length

            val chunkType = ChunkType.entries.firstOrNull { it.id == type } ?: ChunkType.UNKNOWN
            if (chunkType != wantedType) {
                throw GltfLoadException("Bad chunk type: $chunkType, want $wantedType")
            }

            // First, let's try mapping into memory
            try {
                val length = length.toLong()
                val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, channel.position(), length)
                channel.position(channel.position() + length)
                return mappedBuffer
            } catch (_: IOException) {
            } catch (_: UnsupportedOperationException) { // For ZipFileSystem
            }

            // If mapping failed, just read it to memory
            if (length > sizeLimit.toUInt()) {
                throw GltfLoadException("Chunk is too large: maximum is $sizeLimit, but got $length")
            }
            val binaryBuffer = ByteBuffer.allocateDirect(length.toInt())
            binaryBuffer.limit(length.toInt())
            channel.readAll(binaryBuffer)
            binaryBuffer.flip()
            if (binaryBuffer.limit() < length.toInt()) {
                throw GltfLoadException("Chunk's size not correct: want to read $length, but only got ${binaryBuffer.limit()}")
            }
            return binaryBuffer
        }

        val jsonChunk =
            readChunk(ChunkType.JSON, 4 * 1024 * 1024) ?: throw GltfLoadException("Missing JSON chunk in binary GLTF")
        val binaryChunk = readChunk(ChunkType.BINARY, 256 * 1024 * 1024)

        val context = GltfLoader(
            buffer = binaryChunk,
            filePath = path,
            basePath = path.parent,
        )
        context.load(StandardCharsets.UTF_8.decode(jsonChunk).toString())
    }
}