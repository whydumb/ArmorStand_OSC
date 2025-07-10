package top.fifthlight.blazerod.model

import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.model.data.ModelMatricesBuffer
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.model.data.MorphTargetBuffer
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.CowBuffer
import top.fifthlight.blazerod.util.CowBufferList
import top.fifthlight.blazerod.util.copy
import top.fifthlight.blazerod.util.mapToArray
import top.fifthlight.blazerod.util.mapToArrayIndexed
import java.util.function.Consumer

class ModelInstance(val scene: RenderScene) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "model_instance")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    val modelData = ModelData(scene)

    init {
        scene.increaseReferenceCount()
    }

    class ModelData(scene: RenderScene) : AutoCloseable {
        val defaultTransforms = scene.transformNodeIndices.mapToArray { nodeIndex ->
            val node = scene.nodes[nodeIndex] as RenderNode.Transform
            node.defaultTransform?.clone()
        }

        val transforms = Array(scene.transformNodeIndices.size) { transformIndex ->
            defaultTransforms[transformIndex]?.clone()
        }

        val transformsDirty = Array(scene.transformNodeIndices.size) { true }

        val modelMatricesBuffer = run {
            val buffer = ModelMatricesBuffer(scene)
            buffer.clear()
            CowBuffer.acquire(buffer).also { it.increaseReferenceCount() }
        }

        val skinBuffers = scene.skins.mapIndexed { index, skin ->
            val skinBuffer = RenderSkinBuffer(skin)
            skinBuffer.clear()
            CowBuffer.acquire(skinBuffer).also { it.increaseReferenceCount() }
        }

        val targetBuffers = scene.morphedPrimitiveNodeIndices.mapIndexed { index, nodeIndex ->
            val node = scene.nodes[nodeIndex] as RenderNode.Primitive
            val primitive = node.primitive
            val targets = primitive.targets!!
            val targetBuffers = MorphTargetBuffer(targets)
            for (targetGroup in primitive.targetGroups) {
                fun processGroup(index: Int?, channel: MorphTargetBuffer.WeightChannel, weight: Float) =
                    index?.let {
                        channel[index] = weight
                    }
                processGroup(targetGroup.position, targetBuffers.positionChannel, targetGroup.weight)
                processGroup(targetGroup.color, targetBuffers.colorChannel, targetGroup.weight)
                processGroup(targetGroup.texCoord, targetBuffers.texCoordChannel, targetGroup.weight)
            }
            CowBuffer.acquire(targetBuffers).also { it.increaseReferenceCount() }
        }

        val cameraTransforms = scene.cameras.map { CameraTransform(it.camera) }

        override fun close() {
            modelMatricesBuffer.decreaseReferenceCount()
            skinBuffers.forEach { it.decreaseReferenceCount() }
            targetBuffers.forEach { it.decreaseReferenceCount() }
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

    fun setTransformDecomposed(index: Int, updater: Consumer<NodeTransform.Decomposed>) =
        setTransformDecomposed(index) { updater.accept(this) }

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

    inline fun setRelativeTransformDecomposed(index: Int, crossinline updater: NodeTransform.Decomposed.() -> Unit) {
        val defaultTransform = when (val transform = modelData.defaultTransforms[index]) {
            is NodeTransform.Decomposed -> {
                transform
            }

            is NodeTransform.Matrix -> {
                val prevMatrix = transform.matrix
                val newTransform = NodeTransform.Decomposed(
                    translation = Vector3f().also { prevMatrix.getTranslation(it) },
                    scale = Vector3f().also { prevMatrix.getScale(it) },
                    rotation = Quaternionf().also { prevMatrix.getNormalizedRotation(it) },
                )
                modelData.defaultTransforms[index] = newTransform
                newTransform
            }

            null -> {
                val newTransform = NodeTransform.Decomposed()
                modelData.defaultTransforms[index] = newTransform
                newTransform
            }
        }

        when (val currentTransform = modelData.transforms[index]) {
            is NodeTransform.Decomposed -> {
                currentTransform.set(defaultTransform)
                updater(currentTransform)
            }

            else -> {
                val newTransform = defaultTransform.clone()
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
        val weightsIndex =
            requireNotNull(node.morphedPrimitiveIndex) { "Node $nodeIndex don't have target? Check model loader" }
        val weights = modelData.targetBuffers[weightsIndex]
        weights.edit {
            group.position?.let { positionChannel[it] = weight }
            group.color?.let { colorChannel[it] = weight }
            group.texCoord?.let { texCoordChannel[it] = weight }
        }
    }

    private val updateMatrixStack = Matrix4fStack(BlazeRod.MAX_TRANSFORM_DEPTH)

    fun updateCamera() {
        scene.updateCamera(this, updateMatrixStack)
    }

    fun debugRender(matrixStack: MatrixStack, consumers: VertexConsumerProvider) {
        scene.debugRender(this, matrixStack, consumers)
    }

    fun update() {
        scene.update(this, updateMatrixStack)
    }

    fun render(modelViewMatrix: Matrix4fc, light: Int) {
        // Upload indices don't change the actual data
        modelData.targetBuffers.forEach { it.content.uploadIndices() }
        scene.render(
            modelViewMatrix = modelViewMatrix,
            light = light,
            modelMatricesBuffer = modelData.modelMatricesBuffer.content,
            skinBuffer = CowBufferList(modelData.skinBuffers),
            morphTargetBuffer = CowBufferList(modelData.targetBuffers),
        )
    }

    fun schedule(modelViewMatrix: Matrix4fc, light: Int): RenderTask = RenderTask.acquire(
        instance = this,
        modelViewMatrix = modelViewMatrix,
        light = light,
        modelMatricesBuffer = modelData.modelMatricesBuffer.copy(),
        skinBuffer = modelData.skinBuffers.copy(),
        morphTargetBuffer = modelData.targetBuffers.copy().also { buffer ->
            // Upload indices don't change the actual data
            buffer.forEach {
                it.content.uploadIndices()
            }
        },
    )

    override fun onClosed() {
        scene.decreaseReferenceCount()
        modelData.close()
    }
}
