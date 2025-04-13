package top.fifthlight.armorstand.model

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.HumanoidTag
import top.fifthlight.renderer.model.NodeId
import top.fifthlight.renderer.model.NodeTransform

class RenderScene(
    val rootNode: RenderNode,
    val defaultTransforms: Array<NodeTransform?>,
    val skins: List<RenderSkin>,
    val rootTransformId: Int,
    val nodeIdTransformMap: Object2IntMap<NodeId>,
    val nodeNameTransformMap: Object2IntMap<String>,
    val humanoidTagTransformMap: Reference2IntMap<HumanoidTag>,
): AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "scene")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    init {
        rootNode.increaseReferenceCount()
        humanoidTagTransformMap.defaultReturnValue(-1)
        nodeNameTransformMap.defaultReturnValue(-1)
        nodeIdTransformMap.defaultReturnValue(-1)
    }

    fun update(modelInstance: ModelInstance, matrixStack: MatrixStack) = rootNode.update(modelInstance, matrixStack, false)

    fun render(modelInstance: ModelInstance, matrixStack: MatrixStack, light: Int) {
        val globalMatrix = Matrix4f(matrixStack.peek().positionMatrix)
        rootNode.render(modelInstance, matrixStack, globalMatrix, light)
    }

    fun renderDebug(
        modelInstance: ModelInstance,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        textRenderer: TextRenderer,
        dispatcher: EntityRenderDispatcher,
        light: Int,
    ) {
        val globalMatrix = Matrix4f(matrixStack.peek().positionMatrix)
        rootNode.renderDebug(modelInstance, matrixStack, globalMatrix, vertexConsumerProvider, textRenderer, dispatcher, light)
    }

    override fun onClosed() {
        rootNode.decreaseReferenceCount()
    }
}