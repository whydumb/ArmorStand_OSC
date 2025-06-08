package top.fifthlight.armorstand.model

import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import net.minecraft.util.Identifier
import org.joml.Matrix4fStack
import org.joml.Matrix4fc
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.blazerod.model.HumanoidTag
import top.fifthlight.blazerod.model.NodeId

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
): AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "scene")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    init {
        rootNode.increaseReferenceCount()
        humanoidTagToTransformMap.defaultReturnValue(-1)
        nodeNameToTransformMap.defaultReturnValue(-1)
        nodeIdToTransformMap.defaultReturnValue(-1)
    }

    fun update(modelInstance: ModelInstance, matrixStack: Matrix4fStack) = rootNode.update(modelInstance, matrixStack, false)

    fun render(modelInstance: ModelInstance, viewModelMatrix: Matrix4fc, light: Int) {
        for (node in primitiveNodes) {
            node.render(modelInstance, viewModelMatrix, light)
        }
    }

    fun renderInstanced(tasks: List<RenderTask.Instance>) {
        when (tasks.size) {
            0 -> return
            1 -> {
                val task = tasks.first()
                render(task.instance, task.viewModelMatrix, task.light)
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
