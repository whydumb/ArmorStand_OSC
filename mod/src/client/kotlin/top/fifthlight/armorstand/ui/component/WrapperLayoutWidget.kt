package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.widget.LayoutWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.util.math.MathHelper

interface WrapperLayoutWidget : LayoutWidget {
    var left: Int
    var top: Int
    var widthKt: Int
    var heightKt: Int

    fun setDimension(width: Int, height: Int) {
        this.widthKt = width
        this.heightKt = height
    }

    override fun setX(x: Int) {
        this.forEachElement { element ->
            val j = element!!.x + (x - this.left)
            element.x = j
        }
        this.left = x
    }

    override fun setY(y: Int) {
        this.forEachElement { element ->
            val j = element!!.y + (y - this.top)
            element.y = j
        }
        this.top = y
    }

    override fun getX(): Int = this.left

    override fun getY(): Int = this.top

    override fun getWidth(): Int = this.widthKt

    override fun getHeight(): Int = this.heightKt

    open class WrappedElement(val widget: Widget, positioner: Positioner) {
        val positioner: Positioner.Impl = positioner.toImpl()

        val height
            get() = this.widget.height + this.positioner.marginTop + this.positioner.marginBottom

        val width
            get() = this.widget.width + this.positioner.marginLeft + this.positioner.marginRight

        fun setX(left: Int, width: Int) {
            val start = this.positioner.marginLeft
            val end = width - this.widget.width - this.positioner.marginRight
            val i = MathHelper.lerp(this.positioner.relativeX, start, end)
            this.widget.x = i + left
        }

        fun setY(top: Int, height: Int) {
            val start = this.positioner.marginTop
            val end = height - this.widget.height - this.positioner.marginBottom
            val i = MathHelper.lerp(this.positioner.relativeY, start, end)
            this.widget.y = i + top
        }
    }
}
