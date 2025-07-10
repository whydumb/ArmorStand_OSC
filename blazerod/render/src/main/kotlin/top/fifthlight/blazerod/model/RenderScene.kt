package top.fifthlight.blazerod.model

import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.joml.Matrix4fStack
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.data.ModelMatricesBuffer
import top.fifthlight.blazerod.model.data.MorphTargetBuffer
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.CowBufferList

class RenderScene(
    val rootNode: RenderNode,
    val nodes: List<RenderNode>,
    val primitiveNodes: List<RenderNode.Primitive>,
    val transformNodeIndices: IntList,
    val morphedPrimitiveNodeIndices: IntList,
    val rootTransformNodeIndex: Int,
    val skins: List<RenderSkin>,
    val nodeIdToTransformMap: Object2IntMap<NodeId>,
    val nodeNameToTransformMap: Object2IntMap<String>,
    val humanoidTagToTransformMap: Reference2IntMap<HumanoidTag>,
    val expressions: List<RenderExpression>,
    val expressionGroups: List<RenderExpressionGroup>,
    val cameras: List<RenderCamera>,
) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "scene")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    init {
        rootNode.increaseReferenceCount()
        humanoidTagToTransformMap.defaultReturnValue(-1)
        nodeNameToTransformMap.defaultReturnValue(-1)
        nodeIdToTransformMap.defaultReturnValue(-1)
    }

    fun updateCamera(modelInstance: ModelInstance, matrixStack: Matrix4fStack) {
        if (cameras.isEmpty()) {
            return
        }
        rootNode.updateCamera(modelInstance, matrixStack, false)
    }

    fun debugRender(instance: ModelInstance, matrixStack: MatrixStack, consumers: VertexConsumerProvider) {
        rootNode.debugRender(instance, matrixStack, consumers)
    }

    fun update(modelInstance: ModelInstance, matrixStack: Matrix4fStack) {
        rootNode.preUpdate(modelInstance, matrixStack, false)
        rootNode.update(modelInstance, matrixStack, false)
    }

    fun render(
        modelViewMatrix: Matrix4fc,
        light: Int,
        modelMatricesBuffer: ModelMatricesBuffer,
        skinBuffer: List<RenderSkinBuffer>?,
        morphTargetBuffer: List<MorphTargetBuffer>?,
    ) {
        for (node in primitiveNodes) {
            node.render(
                scene = this,
                modelViewMatrix = modelViewMatrix,
                light = light,
                modelMatricesBuffer = modelMatricesBuffer,
                skinBuffer = skinBuffer,
                morphTargetBuffer = morphTargetBuffer,
            )
        }
    }

    fun renderInstanced(tasks: List<RenderTask>) {
        when (tasks.size) {
            0 -> return
            1 -> {
                val task = tasks.first()
                render(
                    modelViewMatrix = task.modelViewMatrix,
                    light = task.light,
                    modelMatricesBuffer = task.modelMatricesBuffer.content,
                    skinBuffer = CowBufferList(task.skinBuffer),
                    morphTargetBuffer = CowBufferList(task.morphTargetBuffer),
                )
            }

            else -> for (node in primitiveNodes) {
                node.renderInstanced(tasks)
            }
        }
    }

    override fun onClosed() {
        rootNode.decreaseReferenceCount()
    }
}
