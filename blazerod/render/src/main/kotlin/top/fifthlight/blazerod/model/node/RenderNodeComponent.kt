package top.fifthlight.blazerod.model.node

import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.Mesh
import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.model.RenderTask
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.model.data.ModelMatricesBuffer
import top.fifthlight.blazerod.model.data.MorphTargetBuffer
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.model.resource.RenderPrimitive
import top.fifthlight.blazerod.util.AbstractRefCount

sealed class RenderNodeComponent<C : RenderNodeComponent<C>> : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "node")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    sealed class Type<C : RenderNodeComponent<C>> {
        object Primitive : Type<RenderNodeComponent.Primitive>()
        object Joint : Type<RenderNodeComponent.Joint>()
        object InfluenceTarget : Type<RenderNodeComponent.InfluenceTarget>()
        object Camera : Type<RenderNodeComponent.Camera>()
    }

    abstract val type: Type<C>

    abstract val updatePhases: List<UpdatePhase.Type>
    abstract fun update(phase: UpdatePhase, node: RenderNode, instance: ModelInstance)

    class Primitive(
        val primitiveIndex: Int,
        val primitive: RenderPrimitive,
        val skinIndex: Int?,
        val morphedPrimitiveIndex: Int?,
        val firstPersonFlag: Mesh.FirstPersonFlag = Mesh.FirstPersonFlag.BOTH,
    ) : RenderNodeComponent<Primitive>() {
        init {
            primitive.increaseReferenceCount()
        }

        override fun onClosed() {
            primitive.decreaseReferenceCount()
        }

        override val type: Type<Primitive>
            get() = Type.Primitive

        companion object {
            private val updatePhases = listOf(UpdatePhase.Type.RENDER_DATA_UPDATE)
        }

        override val updatePhases
            get() = Companion.updatePhases

        override fun update(
            phase: UpdatePhase,
            node: RenderNode,
            instance: ModelInstance,
        ) {
            if (phase is UpdatePhase.RenderDataUpdate) {
                if (skinIndex != null) {
                    return
                }
                instance.modelData.modelMatricesBuffer.edit {
                    setMatrix(primitiveIndex, instance.getWorldTransform(node))
                }
            }
        }

        fun render(
            scene: RenderScene,
            modelViewMatrix: Matrix4fc,
            light: Int,
            modelMatricesBuffer: ModelMatricesBuffer,
            skinBuffer: List<RenderSkinBuffer>?,
            morphTargetBuffer: List<MorphTargetBuffer>?,
        ) {
            primitive.render(
                scene = scene,
                primitiveIndex = primitiveIndex,
                viewModelMatrix = modelViewMatrix,
                light = light,
                modelMatricesBuffer = modelMatricesBuffer,
                skinBuffer = skinIndex?.let {
                    (skinBuffer ?: error("Has skin but no skin buffer"))[it]
                },
                targetBuffer = morphedPrimitiveIndex?.let {
                    (morphTargetBuffer ?: error("Has morph targets but no morph target buffer"))[it]
                }
            )
        }

        fun renderInstanced(tasks: List<RenderTask>) {
            primitive.renderInstanced(tasks, this)
        }
    }

    class Joint(
        val skinIndex: Int,
        val jointIndex: Int,
    ) : RenderNodeComponent<Joint>() {
        override fun onClosed() {}

        override val type: Type<Joint>
            get() = Type.Joint

        companion object {
            private val updatePhases = listOf(UpdatePhase.Type.RENDER_DATA_UPDATE, UpdatePhase.Type.DEBUG_RENDER)
        }

        override val updatePhases
            get() = Companion.updatePhases

        private val cacheMatrix = Matrix4f()

        override fun update(phase: UpdatePhase, node: RenderNode, instance: ModelInstance) {
            when (phase) {
                is UpdatePhase.RenderDataUpdate -> {
                    val cacheMatrix = cacheMatrix
                    cacheMatrix.set(instance.getWorldTransform(node))
                    val skin = instance.scene.skins[skinIndex]
                    val skinBuffer = instance.modelData.skinBuffers[skinIndex]
                    val inverseMatrix = skin.inverseBindMatrices?.get(jointIndex)
                    skinBuffer.edit {
                        inverseMatrix?.let { cacheMatrix.mul(it) }
                        setMatrix(jointIndex, cacheMatrix)
                    }
                }

                is UpdatePhase.DebugRender -> {
                    val consumers = phase.vertexConsumerProvider
                    // TODO: find the real parent joint
                    node.parent?.let { parentJoint ->
                        val buffer = consumers.getBuffer(RenderLayer.getDebugLineStrip(1.0))

                        val parent = phase.viewProjectionMatrix.mul(instance.getWorldTransform(parentJoint), phase.cacheMatrix)
                        buffer.vertex(parent, 0f, 0f, 0f).color(Colors.YELLOW)
                        val self = phase.viewProjectionMatrix.mul(instance.getWorldTransform(node), phase.cacheMatrix)
                        buffer.vertex(self, 0f, 0f, 0f).color(Colors.RED)
                    }
                }

                else -> {}
            }
        }
    }

    class Camera(val cameraIndex: Int) : RenderNodeComponent<Camera>() {
        override fun onClosed() {}

        override val type: Type<Camera>
            get() = Type.Camera

        companion object {
            private val updatePhases = listOf(UpdatePhase.Type.CAMERA_UPDATE)
        }

        override val updatePhases
            get() = Companion.updatePhases

        override fun update(phase: UpdatePhase, node: RenderNode, instance: ModelInstance) {
            if (phase is UpdatePhase.CameraUpdate) {
                val cameraTransform = instance.modelData.cameraTransforms[cameraIndex]
                cameraTransform.update(instance.getWorldTransform(node))
            }
        }
    }

    class InfluenceTarget(
        val target: TransformId,
        val sourceNodeIndex: Int,
        val influence: Float,
        val influenceRotation: Boolean = false,
        val influenceTranslation: Boolean = false,
    ) : RenderNodeComponent<InfluenceTarget>() {
        override fun onClosed() {}

        override val type: Type<InfluenceTarget>
            get() = Type.InfluenceTarget

        companion object {
            private val updatePhases =
                listOf(UpdatePhase.Type.INFLUENCE_TRANSFORM_UPDATE)
        }

        override val updatePhases
            get() = Companion.updatePhases

        override fun update(phase: UpdatePhase, node: RenderNode, instance: ModelInstance) {
            if (phase is UpdatePhase.InfluenceTransformUpdate) {
                val transformMap = instance.getTransformMap(node)
                val sourceTransformMap = instance.modelData.transformMaps[sourceNodeIndex]
                val sourceTransform = sourceTransformMap.getSum(TransformId.RELATIVE_ANIMATION)
                transformMap.updateDecomposed(target) {
                    if (influenceRotation) {
                        sourceTransform.getUnnormalizedRotation(rotation)
                        rotation.mul(influence)
                    }
                    if (influenceTranslation) {
                        sourceTransform.getTranslation(translation)
                        translation.mul(influence)
                    }
                }
            }
        }
    }
}