package top.fifthlight.armorstand.ui.screen

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.core.ParentComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.model.ViewModel
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher

abstract class BaseArmorStandScreen<R: ParentComponent>(
    protected val parent: Screen? = null,
    title: Text,
): BaseOwoScreen<R>(title) {
    override fun close() {
        client?.setScreen(parent)
    }
}