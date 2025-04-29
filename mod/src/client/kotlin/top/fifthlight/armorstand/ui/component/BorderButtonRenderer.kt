package top.fifthlight.armorstand.ui.component

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.core.OwoUIDrawContext

class BorderButtonRenderer(val color: Int): ButtonComponent.Renderer {
    override fun draw(
        context: OwoUIDrawContext,
        button: ButtonComponent,
        delta: Float,
    ) {
        context.drawBorder(button.x, button.y, button.width, button.height, color)
    }
}