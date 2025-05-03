package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import java.util.function.Consumer

class LinearLayout(
    override var left: Int = 0,
    override var top: Int = 0,
    override var widthKt: Int = 0,
    override var heightKt: Int = 0,
    var direction: Direction = Direction.HORIZONTAL,
    val align: Align = Align.START,
    var gap: Int = 0,
    var padding: Insets = Insets.ZERO,
    val surface: Surface = Surface.empty,
): WrapperLayoutWidget, Drawable {
    enum class Direction {
        HORIZONTAL,
        VERTICAL,
    }

    enum class Align {
        START,
        CENTER,
        END,
    }

    private val elements = mutableListOf<WrapperLayoutWidget.WrappedElement>()

    fun add(element: Widget, positioner: Positioner) {
        elements.add(WrapperLayoutWidget.WrappedElement(element, positioner))
    }

    fun removeAt(index: Int) = elements.removeAt(index).widget

    fun setAt(index: Int, element: Widget, positioner: Positioner) = elements[index].widget.also {
        elements[index] = WrapperLayoutWidget.WrappedElement(element, positioner)
    }

    fun clear() = elements.map { it.widget }.also { elements.clear() }

    override fun refreshPositions() {
        val sizes = elements.map {
            when (direction) {
                Direction.HORIZONTAL -> it.width
                Direction.VERTICAL -> it.height
            }
        }
        val totalSize = sizes.sum() + gap * (sizes.size - 1)
        val initial = when(direction) {
            Direction.HORIZONTAL -> left + padding.left
            Direction.VERTICAL -> top + padding.top
        }
        val total = when (direction) {
            Direction.HORIZONTAL -> widthKt - padding.left - padding.right
            Direction.VERTICAL -> heightKt - padding.top - padding.bottom
        }
        var pos = initial + when(align) {
            Align.START -> 0
            Align.CENTER -> (total - totalSize) / 2
            Align.END -> total - totalSize
        }
        for ((index, size) in sizes.withIndex()) {
            val element = elements[index]
            when (direction) {
                Direction.HORIZONTAL -> {
                    val availableHeight = heightKt - padding.top - padding.bottom
                    element.setX(pos, element.width)
                    element.setY(top + padding.top, availableHeight)
                }
                Direction.VERTICAL -> {
                    val availableWidth = widthKt - padding.left - padding.right
                    element.setX(left + padding.left, availableWidth)
                    element.setY(pos, element.height)
                }
            }
            pos += size + gap
        }
        super.refreshPositions()
    }

    override fun forEachElement(consumer: Consumer<Widget>) {
        elements.forEach { consumer.accept(it.widget) }
    }

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        surface.draw(context, left, top, widthKt, heightKt)
    }
}