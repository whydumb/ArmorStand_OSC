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

abstract class ArmorStandScreen<R: ParentComponent, M: ViewModel>(
    protected val parent: Screen? = null,
    viewModelFactory: (CoroutineScope) -> M,
    title: Text,
): BaseOwoScreen<R>(title) {
    protected val scope = CoroutineScope(ThreadExecutorDispatcher(MinecraftClient.getInstance()))
    protected val viewModel = viewModelFactory(scope)

    override fun close() {
        scope.cancel()
        client?.setScreen(parent)
    }
}