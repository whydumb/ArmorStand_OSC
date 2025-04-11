package top.fifthlight.renderer.model

import java.nio.ByteBuffer
import java.nio.file.Path

interface ModelFileLoader {
    enum class Ability {
        MODEL,
        ANIMATION,
    }
    
    val extensions: List<String>
    val abilities: List<Ability>
    val probeLength: Int
    fun probe(buffer: ByteBuffer): Boolean

    fun load(path: Path, basePath: Path = path.parent): Result

    data class Result(
        val metadata: Metadata?,
        val scene: Scene? = null,
        val animations: List<Animation>?,
    )
}