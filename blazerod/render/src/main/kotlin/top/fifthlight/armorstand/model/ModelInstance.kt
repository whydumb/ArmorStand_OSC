package top.fifthlight.armorstand.model

import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.armorstand.BlazeRod
import top.fifthlight.armorstand.model.data.ModelMatricesBuffer
import top.fifthlight.armorstand.model.data.RenderSkinBuffer
import top.fifthlight.armorstand.model.data.RenderTargetBuffer
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.mapToArray
import top.fifthlight.armorstand.util.mapToArrayIndexed
import top.fifthlight.blazerod.model.NodeTransform

class ModelInstance(
    val scene: RenderScene,
    bufferEntry: ModelBufferManager.BufferEntry,
) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "model_instance")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    val modelData = ModelData(
        scene = scene,
        bufferEntry = bufferEntry,
    )

    init {
        scene.increaseReferenceCount()
    }

    class ModelData(
        scene: RenderScene,
        private val bufferEntry: ModelBufferManager.BufferEntry,
    ) : AutoCloseable {
        init {
            bufferEntry.increaseReferenceCount()
        }

        val transforms = scene.transformNodeIndices.mapToArray { nodeIndex ->
            val node = scene.nodes[nodeIndex] as RenderNode.Transform
            node.defaultTransform?.clone()
        }

        val transformsDirty = Array(scene.transformNodeIndices.size) { true }

        val modelMatricesBuffer = ModelMatricesBuffer(scene, bufferEntry.modelMatricesBuffers.allocateSlot()).also {
            it.clear()
        }

        val skinBuffers = scene.skins.mapToArrayIndexed { index, skin ->
            RenderSkinBuffer(skin, bufferEntry.skinBuffers[index].allocateSlot()).also {
                it.clear()
            }
        }

        val targetBuffers = scene.morphedPrimitiveNodeIndices.mapToArrayIndexed { index, nodeIndex ->
            val node = scene.nodes[nodeIndex] as RenderNode.Primitive
            val primitive = node.primitive
            val targets = primitive.targets!!
            val targetBuffers = RenderTargetBuffer(
                targets = targets,
                weightsSlot = bufferEntry.morphWeightBuffers[index].allocateSlot(),
                indicesSlot = bufferEntry.morphIndicesBuffers[index].allocateSlot(),
            )
            for (targetGroup in primitive.targetGroups) {
                fun processGroup(index: Int?, channel: RenderTargetBuffer.WeightChannel, weight: Float) =
                    index?.let {
                        channel[index] = weight
                    }
                processGroup(targetGroup.position, targetBuffers.positionChannel, targetGroup.weight)
                processGroup(targetGroup.color, targetBuffers.colorChannel, targetGroup.weight)
                processGroup(targetGroup.texCoord, targetBuffers.texCoordChannel, targetGroup.weight)
            }
            targetBuffers
        }

        override fun close() {
            // release slots
            modelMatricesBuffer.close()
            skinBuffers.forEach { it.close() }
            targetBuffers.forEach { it.close() }
            // release underlying slot buffer
            bufferEntry.decreaseReferenceCount()
        }
    }

    fun setTransformMatrix(transformIndex: Int, matrix: Matrix4f) {
        val transform = modelData.transforms[transformIndex]
        when (transform) {
            is NodeTransform.Matrix -> {
                transform.matrix.set(matrix)
            }

            else -> {
                modelData.transforms[transformIndex] = NodeTransform.Matrix(matrix)
            }
        }
        modelData.transformsDirty[transformIndex] = true
    }

    inline fun setTransformDecomposed(index: Int, crossinline updater: NodeTransform.Decomposed.() -> Unit) {
        val transform = modelData.transforms[index]
        when (transform) {
            is NodeTransform.Decomposed -> {
                updater(transform)
            }

            is NodeTransform.Matrix -> {
                val prevMatrix = transform.matrix
                val newTransform = NodeTransform.Decomposed(
                    translation = Vector3f().also { prevMatrix.getTranslation(it) },
                    scale = Vector3f().also { prevMatrix.getScale(it) },
                    rotation = Quaternionf().also { prevMatrix.getNormalizedRotation(it) },
                )
                updater(newTransform)
                modelData.transforms[index] = newTransform
            }

            null -> {
                val newTransform = NodeTransform.Decomposed()
                updater(newTransform)
                modelData.transforms[index] = newTransform
            }
        }
        modelData.transformsDirty[index] = true
    }

    fun setGroupWeight(morphedPrimitiveIndex: Int, targetGroupIndex: Int, weight: Float) {
        val nodeIndex = scene.morphedPrimitiveNodeIndices.getInt(morphedPrimitiveIndex)
        val node = scene.nodes[nodeIndex]
        require(node is RenderNode.Primitive) { "Node id $morphedPrimitiveIndex is not primitive" }
        val group = node.primitive.targetGroups[targetGroupIndex]
        val weightsIndex = requireNotNull(node.morphedPrimitiveIndex) { "Node $nodeIndex don't have target? Check model loader" }
        val weights = modelData.targetBuffers[weightsIndex]
        group.position?.let { weights.positionChannel[it] = weight }
        group.color?.let { weights.colorChannel[it] = weight }
        group.texCoord?.let { weights.texCoordChannel[it] = weight }
    }

    private val updateMatrixStack = Matrix4fStack(BlazeRod.MAX_TRANSFORM_DEPTH)

    fun update() {
        scene.update(this, updateMatrixStack)
    }

    fun render(modelMatrix: Matrix4fc, light: Int) {
        scene.render(this, modelMatrix, light)
    }

    fun schedule(modelMatrix: Matrix4fc, light: Int) = RenderTask.Instance.acquire(
        instance = this,
        modelMatrix = modelMatrix,
        light = light,
    )

    override fun onClosed() {
        scene.decreaseReferenceCount()
        modelData.close()
    }
}
