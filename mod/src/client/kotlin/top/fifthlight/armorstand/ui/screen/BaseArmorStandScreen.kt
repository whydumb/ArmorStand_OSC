package top.fifthlight.armorstand.ui.screen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.dsl.ScreenContext
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher

abstract class BaseArmorStandScreen<T: BaseArmorStandScreen<T>>(
    protected val parent: Screen? = null,
    title: Text,
): Screen(title) {
    private var _viewScope: CoroutineScope? = null

    override fun init() {
        val client = MinecraftClient.getInstance()
        val scope = CoroutineScope(ThreadExecutorDispatcher(client) + SupervisorJob())
        _viewScope = scope
        @Suppress("UNCHECKED_CAST")
        val context = ScreenContext(
            screen = this as T,
            client = client,
            viewScope = scope,
        )
        with(context) {
            createLayout()
        }
    }

    abstract fun ScreenContext<T>.createLayout()

    override fun close() {
        _viewScope?.cancel()
        client?.setScreen(parent)
    }
}