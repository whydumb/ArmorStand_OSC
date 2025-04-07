package top.fifthlight.armorstand.model

import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.NodeTransform
import top.fifthlight.renderer.model.Skin

class RenderScene(
    val rootNode: RenderNode,
    val updatableNodes: List<RenderNode.Updatable>,
    val defaultTransforms: Array<NodeTransform?>,
    val skins: List<RenderSkin>,
): AbstractRefCount() {
    init {
        rootNode.increaseReferenceCount()
    }

    fun update() {

    }

    fun render(modelInstance: ModelInstance, matrixStack: MatrixStack, light: Int) {
        val globalMatrix = Matrix4f(matrixStack.peek().positionMatrix)
        rootNode.render(modelInstance, matrixStack, globalMatrix, light)
    }

    override fun onClosed() {
        rootNode.decreaseReferenceCount()
    }
}