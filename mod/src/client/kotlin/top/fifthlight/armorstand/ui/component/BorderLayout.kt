package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import java.util.function.Consumer

class BorderLayout(
    override var left: Int = 0,
    override var top: Int = 0,
    override var widthKt: Int = 0,
    override var heightKt: Int = 0,
    var direction: Direction = Direction.HORIZONTAL
) : WrapperLayoutWidget {
    enum class Direction {
        HORIZONTAL,
        VERTICAL
    }

    private class Element<T: Widget>(
        private val inner: T,
        positioner: Positioner,
        private val onSizeChanged: (widget: T, width: Int, height: Int) -> Unit = { _, _, _ -> }
    ) : WrapperLayoutWidget.WrappedElement(inner, positioner) {
        fun setSize(width: Int, height: Int) = onSizeChanged(
            inner,
            width - positioner.marginLeft - positioner.marginRight,
            height - positioner.marginTop - positioner.marginBottom
        )
    }

    private var firstElement: Element<*>? = null
    private var secondElement: Element<*>? = null
    private var centerElement: Element<*>? = null

    override fun forEachElement(consumer: Consumer<Widget>) {
        firstElement?.let { consumer.accept(it.widget) }
        centerElement?.let { consumer.accept(it.widget) }
        secondElement?.let { consumer.accept(it.widget) }
    }

    fun <T: Widget> setFirstElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit
    ) {
        firstElement = Element(widget, positioner, onSizeChanged)
    }

    fun <T: Widget> setSecondElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit
    ) {
        secondElement = Element(widget, positioner, onSizeChanged)
    }

    fun <T: Widget> setCenterElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit
    ) {
        centerElement = Element(widget, positioner, onSizeChanged)
    }

    override fun refreshPositions() {
        val first = firstElement
        val second = secondElement
        val center = centerElement

        when (direction) {
            Direction.HORIZONTAL -> {
                val leftWidth = first?.width ?: 0
                val rightWidth = second?.width ?: 0
                val centerWidth = widthKt - leftWidth - rightWidth
                first?.let {
                    it.setSize(leftWidth, heightKt)
                    it.setX(left, left + leftWidth)
                    it.setY(top, top + heightKt)
                }
                center?.let {
                    it.setSize(centerWidth, heightKt)
                    it.setX(left + leftWidth, left + widthKt - rightWidth)
                    it.setY(top, top + heightKt)
                }
                second?.let {
                    it.setSize(rightWidth, heightKt)
                    it.setX(left + widthKt - rightWidth, left + widthKt)
                    it.setY(top, top + heightKt)
                }
            }

            Direction.VERTICAL -> {
                val topHeight = first?.height ?: 0
                val bottomHeight = second?.height ?: 0
                val centerHeight = heightKt - topHeight - bottomHeight
                first?.let {
                    it.setSize(widthKt, topHeight)
                    it.setX(left, widthKt)
                    it.setY(top, topHeight)
                }
                center?.let {
                    it.setSize(widthKt, centerHeight)
                    it.setX(left, widthKt)
                    it.setY(top + topHeight, heightKt - bottomHeight)
                }
                second?.let {
                    it.setSize(widthKt, bottomHeight)
                    it.setX(left, widthKt)
                    it.setY(top + heightKt - bottomHeight, heightKt)
                }
            }
        }
        super.refreshPositions()
    }
}