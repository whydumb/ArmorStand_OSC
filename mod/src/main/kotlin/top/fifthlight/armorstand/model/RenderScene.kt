package top.fifthlight.armorstand.model

import net.minecraft.client.util.math.MatrixStack
import top.fifthlight.armorstand.util.AbstractRefCount

class RenderScene(
    val rootNode: RenderNode,
): AbstractRefCount() {
    init {
        rootNode.increaseReferenceCount()
    }

    fun update() {

    }

    fun render(matrixStack: MatrixStack, light: Int) = rootNode.render(matrixStack, light)

    override fun onClosed() {
        rootNode.decreaseReferenceCount()
    }
}