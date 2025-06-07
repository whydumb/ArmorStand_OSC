package top.fifthlight.armorstand.manage

import java.nio.file.Path

data class ModelItem(
    val path: Path,
    val name: String,
    val lastChanged: Long,
    val sha256: ByteArray,
) {
    init {
        require(sha256.size == 32) { "Bad sha256 size: ${sha256.size}" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelItem

        if (lastChanged != other.lastChanged) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (!sha256.contentEquals(other.sha256)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lastChanged.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + sha256.contentHashCode()
        return result
    }
}