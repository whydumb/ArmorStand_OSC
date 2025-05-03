package top.fifthlight.armorstand.ui.dsl

import kotlinx.coroutines.CoroutineScope
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import top.fifthlight.armorstand.ui.util.Dimensions

@DslMarker
@Target(allowedTargets = [AnnotationTarget.CLASS])
annotation class ContextDsl

@ContextDsl
interface Context<T, S: Screen> {
    val screen: S
    val element: T
    val client: MinecraftClient
    val viewScope: CoroutineScope
}

data class ScreenContext<T: Screen>(
    override val screen: T,
    override val client: MinecraftClient = MinecraftClient.getInstance(),
    override val viewScope: CoroutineScope,
): Context<T, T> {
    override val element: T
        get() = screen

    fun dimension() = Dimensions(
        x = 0,
        y = 0,
        width = element.width,
        height = element.height,
    )
}

data class LayoutContext<T, S: Screen>(
    override val element: T,
    override val screen: S,
    override val client: MinecraftClient,
    override val viewScope: CoroutineScope,
): Context<T, S>