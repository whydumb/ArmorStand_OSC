package top.fifthlight.armorstand.ui.state

import top.fifthlight.armorstand.manage.ModelItem
import top.fifthlight.renderer.model.Metadata
import java.nio.file.Path

data class ConfigScreenState(
    val modelItems: List<ModelItem> = listOf(),
    val currentModel: Path? = null,
    val showOtherPlayerModel: Boolean = true,
    val sendModelData: Boolean = true,
    val modelScale: Double = 1.0,
    val currentModelMetadata: Metadata? = null,
    val selectedTab: SelectedTab = SelectedTab.PREVIEW,
) {
    enum class SelectedTab {
        PREVIEW,
        METADATA,
    }
}