package top.fifthlight.armorstand.ui.state

import java.nio.file.Path

data class ConfigScreenState(
    val modelPaths: List<Path> = listOf(),
    val currentModel: Path? = null,
    val showOtherPlayerModel: Boolean = true,
    val sendModelData: Boolean = true,
    val modelScale: Double = 1.0,
)