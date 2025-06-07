package top.fifthlight.armorstand.manage

import top.fifthlight.armorstand.util.ModelHash
import java.nio.file.Path

data class ModelItem(
    val path: Path,
    val name: String,
    val lastChanged: Long,
    val hash: ModelHash,
)