package top.fifthlight.armorstand.ui.component

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors

class ModelButton(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    message: Text,
    var checked: Boolean = false,
    private val textRenderer: TextRenderer,
    private val padding: Insets = Insets(),
    onPressAction: () -> Unit,
) : ButtonWidget(
    x,
    y,
    width,
    height,
    message,
    { onPressAction.invoke() },
    DEFAULT_NARRATION_SUPPLIER,
) {
    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        if (active && checked) {
            context.fill(
                x,
                y,
                x + width,
                y + height,
                0x66000000u.toInt(),
            )
        }
        if (active && hovered) {
            context.drawBorder(
                x,
                y,
                width,
                height,
                0x99000000u.toInt(),
            )
        } else if (isSelected) {
            context.drawBorder(
                x,
                y,
                width,
                height,
                0x44000000u.toInt(),
            )
        }
        val top = y + padding.top
        val bottom = y + height - padding.bottom
        val left = x + padding.left
        val right = x + width - padding.right
        drawScrollableText(
            context,
            textRenderer,
            message,
            left,
            bottom - textRenderer.fontHeight,
            right,
            bottom,
            Colors.WHITE,
        )
    }
}
