package top.fifthlight.renderer.model

import java.nio.ByteBuffer
import java.nio.file.Path

interface ModelFileLoader {
    val extensions: List<String>
    val probeLength: Int
    fun probe(buffer: ByteBuffer): Boolean

    fun load(path: Path, basePath: Path = path.parent): Result

    data class Result(
        val metadata: Metadata?,
        val scene: Scene? = null,
        val animations: List<Animation>?,
    )
}