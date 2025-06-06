package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.gui.widget.WrapperWidget
import java.util.function.Consumer

class SpreadLayout(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    var direction: Direction = Direction.HORIZONTAL,
    var gap: Int = 0,
    var padding: Insets = Insets.ZERO,
) : WrapperWidget(x, y, width, height), ResizableLayout {

    enum class Direction {
        HORIZONTAL,
        VERTICAL,
    }

    private class Element<T : Widget>(
        private val inner: T,
        positioner: Positioner,
        private val onSizeChanged: (widget: T, width: Int, height: Int) -> Unit = { _, _, _ -> },
    ) : WrappedElement(inner, positioner) {
        fun setSize(width: Int, height: Int) = onSizeChanged(
            inner,
            width - positioner.marginLeft - positioner.marginRight,
            height - positioner.marginTop - positioner.marginBottom
        )
    }

    private val elements = mutableListOf<Element<*>>()

    override fun forEachElement(consumer: Consumer<Widget>) {
        elements.forEach { consumer.accept(it.widget) }
    }

    override fun setDimensions(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun <T : Widget> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit
    ) {
        elements.add(Element(widget, positioner, onSizeChanged))
    }

    fun <T : ClickableWidget> add(widget: T, positioner: Positioner = Positioner.create()) {
        add(widget, positioner) { w, width, height -> w.setDimensions(width, height) }
    }

    fun <T> add(widget: T, positioner: Positioner = Positioner.create())
            where T : ResizableLayout, T : Widget {
        add(widget, positioner) { w, width, height -> w.setDimensions(width, height) }
    }

    override fun refreshPositions() {
        val count = elements.size
        if (count == 0) return

        when (direction) {
            Direction.HORIZONTAL -> {
                val horizontalPadding = padding.left + padding.right

                val availableWidth = width - horizontalPadding - gap * (count - 1)
                val availableHeight = height - padding.top - padding.bottom

                val elementWidth = availableWidth / count

                var currentX = x + padding.left
                elements.forEach { element ->
                    element.setSize(elementWidth, availableHeight)
                    element.setX(currentX, currentX + elementWidth)
                    element.setY(y + padding.top, y + height - padding.bottom)

                    currentX += elementWidth + gap
                }
            }
            Direction.VERTICAL -> {
                val verticalPadding = padding.top + padding.bottom

                val availableWidth = width - padding.left - padding.right
                val availableHeight = height - verticalPadding - gap * (count - 1)

                val elementHeight = availableHeight / count

                var currentY = y + padding.top
                elements.forEach { element ->
                    element.setSize(availableWidth, elementHeight)
                    element.setX(x + padding.left, x + width - padding.right)
                    element.setY(currentY, currentY + elementHeight)

                    currentY += elementHeight + gap
                }
            }
        }

        super.refreshPositions()
    }
}
