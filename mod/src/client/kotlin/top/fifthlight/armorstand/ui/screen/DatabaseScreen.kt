package top.fifthlight.armorstand.ui.screen

import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.dsl.ScreenContext
import top.fifthlight.armorstand.ui.model.DatabaseViewModel

class DatabaseScreen(parent: Screen? = null) : ArmorStandScreen<DatabaseScreen, DatabaseViewModel>(
    parent = parent,
    viewModelFactory = ::DatabaseViewModel,
    title = Text.translatable("armorstand.debug_screen.database")
) {
    override fun ScreenContext<DatabaseScreen>.createLayout() {
        TODO("Not yet implemented")
    }
}
