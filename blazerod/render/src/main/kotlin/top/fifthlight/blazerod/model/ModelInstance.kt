package top.fifthlight.blazerod.model

import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.data.ModelMatricesBuffer
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.model.data.MorphTargetBuffer
import top.fifthlight.blazerod.model.node.RenderNode
import top.fifthlight.blazerod.model.node.TransformMap
import top.fifthlight.blazerod.model.resource.CameraTransform
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.CowBuffer
import top.fifthlight.blazerod.util.CowBufferList
import top.fifthlight.blazerod.util.copy
import top.fifthlight.blazerod.util.mapToArray
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
        val transformMaps = scene.nodes.mapToArray { node ->
            TransformMap(node.absoluteTransform)
        }

        val worldTransforms = Array(scene.nodes.size) { Matrix4f() }

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

        val targetBuffers = scene.morphedPrimitiveComponents.mapIndexed { index, component ->
            val primitive = component.primitive
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

    fun setTransformMatrix(nodeIndex: Int, transformId: TransformId, matrix: Matrix4f) {
        val transform = modelData.transformMaps[nodeIndex]
        transform.setMatrix(transformId, matrix)
    }

    fun setTransformDecomposed(nodeIndex: Int, transformId: TransformId, decomposed: NodeTransformView.Decomposed) {
        val transform = modelData.transformMaps[nodeIndex]
        transform.setMatrix(transformId, decomposed)
    }

    fun setTransformDecomposed(index: Int, transformId: TransformId, updater: Consumer<NodeTransform.Decomposed>) =
        setTransformDecomposed(index, transformId) { updater.accept(this) }

    fun setTransformDecomposed(index: Int, transformId: TransformId, updater: NodeTransform.Decomposed.() -> Unit) {
        val transform = modelData.transformMaps[index]
        transform.updateDecomposed(transformId, updater)
    }

    fun setGroupWeight(morphedPrimitiveIndex: Int, targetGroupIndex: Int, weight: Float) {
        val primitiveComponent = scene.morphedPrimitiveComponents[morphedPrimitiveIndex]
        val group = primitiveComponent.primitive.targetGroups[targetGroupIndex]
        val weightsIndex = requireNotNull(primitiveComponent.morphedPrimitiveIndex) {
            "Component $primitiveComponent don't have target? Check model loader"
        }
        val weights = modelData.targetBuffers[weightsIndex]
        weights.edit {
            group.position?.let { positionChannel[it] = weight }
            group.color?.let { colorChannel[it] = weight }
            group.texCoord?.let { texCoordChannel[it] = weight }
        }
    }

    fun updateCamera() {
        scene.updateCamera(this)
    }

    fun debugRender(viewProjectionMatrix: Matrix4fc, consumers: VertexConsumerProvider) {
        scene.debugRender(this, viewProjectionMatrix, consumers)
    }

    fun updateRenderData() {
        scene.updateRenderData(this)
    }

    fun render(
        modelViewMatrix: Matrix4fc,
        light: Int,
        colorFrameBuffer: GpuTextureView,
        depthFrameBuffer: GpuTextureView?,
    ) {
        // Upload indices don't change the actual data
        modelData.targetBuffers.forEach { it.content.uploadIndices() }
        scene.render(
            modelViewMatrix = modelViewMatrix,
            light = light,
            modelMatricesBuffer = modelData.modelMatricesBuffer.content,
            skinBuffer = CowBufferList(modelData.skinBuffers),
            morphTargetBuffer = CowBufferList(modelData.targetBuffers),
            colorFrameBuffer = colorFrameBuffer,
            depthFrameBuffer = depthFrameBuffer,
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
