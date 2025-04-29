package top.fifthlight.armorstand.ui.component

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing

class EditableFlowLayout(
    horizontalSizing: Sizing,
    verticalSizing: Sizing,
    algorithm: Algorithm
) : FlowLayout(horizontalSizing, verticalSizing, algorithm) {
    fun setChild(index: Int, child: Component) = also {
        children[index] = child
        updateLayout()
    }
}
