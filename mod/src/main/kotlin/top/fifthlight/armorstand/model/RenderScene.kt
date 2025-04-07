package top.fifthlight.armorstand.model

import it.unimi.dsi.fastutil.objects.Reference2IntMap
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.HumanoidTag
import top.fifthlight.renderer.model.NodeTransform
import top.fifthlight.renderer.model.Skin

class RenderScene(
    val rootNode: RenderNode,
    val defaultTransforms: Array<NodeTransform?>,
    val skins: List<RenderSkin>,
    val humanoidJointTransformIndices: Reference2IntMap<HumanoidTag>,
): AbstractRefCount() {
    init {
        rootNode.increaseReferenceCount()
        humanoidJointTransformIndices.defaultReturnValue(-1)
    }

    fun update(modelInstance: ModelInstance, matrixStack: MatrixStack) = rootNode.update(modelInstance, matrixStack, false)

    fun render(modelInstance: ModelInstance, matrixStack: MatrixStack, light: Int) {
        val globalMatrix = Matrix4f(matrixStack.peek().positionMatrix)
        rootNode.render(modelInstance, matrixStack, globalMatrix, light)
    }

    override fun onClosed() {
        rootNode.decreaseReferenceCount()
    }
}