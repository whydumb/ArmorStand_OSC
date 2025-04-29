package top.fifthlight.armorstand.ui.component

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.core.OwoUIDrawContext

fun ButtonComponent.stackedRenderer(vararg renderers: ButtonComponent.Renderer) = also {
    renderer(StackButtonRenderer(listOf(*renderers)))
}

class StackButtonRenderer(private val renderers: List<ButtonComponent.Renderer>) : ButtonComponent.Renderer {
    override fun draw(
        context: OwoUIDrawContext,
        button: ButtonComponent,
        delta: Float,
    ) {
        for (renderer in renderers) {
            renderer.draw(context, button, delta)
        }
    }
}