package top.fifthlight.blazerod.model.node

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import org.joml.*
import top.fifthlight.blazerod.model.IkTarget.IkJoint.Limits
import top.fifthlight.blazerod.model.IkTarget.IkJoint.Limits.Axis.*
import top.fifthlight.blazerod.model.Mesh
import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.model.resource.RenderPrimitive
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.drawBox
import java.util.*
import kotlin.math.*

sealed class RenderNodeComponent<C : RenderNodeComponent<C>> : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "node")

        private val DEBUG_RENDER_LAYER = RenderLayer.of(
            "blazerod_joint_debug_lines",
            1536,
            RenderPipelines.LINES,
            RenderLayer.MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(1.0)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .target(RenderPhase.ITEM_ENTITY_TARGET)
                .build(false)
        )
    }

    override val typeId: Identifier
        get() = TYPE_ID

    sealed class Type<C : RenderNodeComponent<C>> {
        object Primitive : Type<RenderNodeComponent.Primitive>()
        object Joint : Type<RenderNodeComponent.Joint>()
        object InfluenceSource : Type<RenderNodeComponent.InfluenceSource>()
        object Camera : Type<RenderNodeComponent.Camera>()
        object IkTarget : Type<RenderNodeComponent.IkTarget>()
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
                    if (node.hasComponentOfType(Type.InfluenceSource)) {
                        return
                    }
                    val consumers = phase.vertexConsumerProvider
                    // TODO: find the real parent joint
                    node.parent?.let { parentJoint ->
                        val buffer = consumers.getBuffer(DEBUG_RENDER_LAYER)

                        val parent =
                            phase.viewProjectionMatrix.mul(instance.getWorldTransform(parentJoint), phase.cacheMatrix)
                        buffer.vertex(parent, 0f, 0f, 0f).color(Colors.YELLOW).normal(0f, 1f, 0f)
                        val self = phase.viewProjectionMatrix.mul(instance.getWorldTransform(node), phase.cacheMatrix)
                        buffer.vertex(self, 0f, 0f, 0f).color(Colors.RED).normal(0f, 1f, 0f)
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

    class InfluenceSource(
        val target: TransformId,
        val targetNodeIndex: Int,
        val influence: Float,
        val influenceRotation: Boolean = false,
        val influenceTranslation: Boolean = false,
    ) : RenderNodeComponent<InfluenceSource>() {
        override fun onClosed() {}

        override val type: Type<InfluenceSource>
            get() = Type.InfluenceSource

        companion object {
            private val updatePhases =
                listOf(UpdatePhase.Type.INFLUENCE_TRANSFORM_UPDATE)
        }

        override val updatePhases
            get() = Companion.updatePhases

        override fun update(phase: UpdatePhase, node: RenderNode, instance: ModelInstance) {
            if (phase is UpdatePhase.InfluenceTransformUpdate) {
                val sourceTransformMap = instance.modelData.transformMaps[node.nodeIndex]
                val sourceTransform = sourceTransformMap.getSum(TransformId.LAST)
                instance.setTransformDecomposed(targetNodeIndex, target) {
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

    class IkTarget(
        val limitRadian: Float,
        val loopCount: Int,
        val chains: List<Chain>,
        val effectorNodeIndex: Int,
        val transformId: TransformId,
    ) : RenderNodeComponent<IkTarget>() {
        override fun onClosed() {}

        override val type: Type<IkTarget>
            get() = Type.IkTarget

        companion object {
            private val updatePhases = listOf(
                UpdatePhase.Type.IK_UPDATE,
                UpdatePhase.Type.DEBUG_RENDER,
            )

            private const val FLOAT_PI = PI.toFloat()
            private const val FLOAT_TWO_PI = FLOAT_PI * 2

            private val decomposeTests = listOf(
                Vector3f(FLOAT_PI, FLOAT_PI, FLOAT_PI),   // + + +
                Vector3f(FLOAT_PI, FLOAT_PI, -FLOAT_PI),  // + + -
                Vector3f(FLOAT_PI, -FLOAT_PI, FLOAT_PI),  // + - +
                Vector3f(FLOAT_PI, -FLOAT_PI, -FLOAT_PI), // + - -
                Vector3f(-FLOAT_PI, FLOAT_PI, FLOAT_PI),  // - + +
                Vector3f(-FLOAT_PI, FLOAT_PI, -FLOAT_PI), // - + -
                Vector3f(-FLOAT_PI, -FLOAT_PI, FLOAT_PI), // - - +
                Vector3f(-FLOAT_PI, -FLOAT_PI, -FLOAT_PI) // - - -
            )
        }

        override val updatePhases: List<UpdatePhase.Type>
            get() = Companion.updatePhases

        class Chain(
            val nodeIndex: Int,
            val limit: Limits?,
        ) {
            val prevAngle = Vector3f()
            val saveIKRot = Quaternionf()
            var planeModeAngle: Float = 0f
        }

        // I took the algorithm from https://github.com/benikabocha/saba/blob/master/src/Saba/Model/MMD/MMDIkSolver.cpp
        private fun normalizeAngle(angle: Float): Float {
            var ret = angle
            while (ret >= FLOAT_TWO_PI) {
                ret -= FLOAT_TWO_PI
            }
            while (ret < 0) {
                ret += FLOAT_TWO_PI
            }

            return ret
        }

        private fun diffAngle(a: Float, b: Float): Float = (normalizeAngle(a) - normalizeAngle(b)).let { diff ->
            when {
                diff > FLOAT_PI -> diff - FLOAT_TWO_PI
                diff < -FLOAT_PI -> diff + FLOAT_TWO_PI
                else -> diff
            }
        }

        private val testVec = Vector3f()
        private fun decompose(m: Matrix3fc, before: Vector3fc, r: Vector3f): Vector3f {
            val sy = -m.m02()
            val e = 1e-6f
            if ((1f - abs(sy)) < e) {
                r.y = asin(sy)
                // Find the closest angle to 180 degrees
                val sx = sin(before.x())
                val sz = sin(before.z())

                if (abs(sx) < abs(sz)) {
                    // X is closer to 0 or 180
                    val cx = cos(before.x())
                    if (cx > 0) {
                        r.x = 0f
                        r.z = asin(-m.m10())
                    } else {
                        r.x = FLOAT_PI
                        r.z = asin(m.m10())
                    }
                } else {
                    val cz = cos(before.z())
                    if (cz > 0) {
                        r.z = 0f
                        r.x = asin(-m.m21())
                    } else {
                        r.z = FLOAT_PI
                        r.x = asin(m.m21())
                    }
                }
            } else {
                r.x = atan2(m.m12(), m.m22())
                r.y = asin(-m.m02())
                r.z = atan2(m.m01(), m.m00())
            }

            val errX = abs(diffAngle(r.x, before.x()))
            val errY = abs(diffAngle(r.y, before.y()))
            val errZ = abs(diffAngle(r.z, before.z()))
            var minErr = errX + errY + errZ
            for (testDiff in decomposeTests) {
                testVec.set(r.x, -r.y, r.z).add(testDiff)
                val err = abs(diffAngle(testVec.x(), before.x())) +
                        abs(diffAngle(testVec.y(), before.y())) +
                        abs(diffAngle(testVec.z(), before.z()))
                if (err < minErr) {
                    minErr = err
                    r.set(testVec)
                }
            }
            return r
        }

        private fun Vector3fc.getAxis(axis: Limits.Axis) = when (axis) {
            X -> x()
            Y -> y()
            Z -> z()
        }

        private fun Vector3f.coerceIn(min: Vector3fc, max: Vector3fc): Vector3f = set(
            x.coerceIn(min.x(), max.x()),
            y.coerceIn(min.y(), max.y()),
            z.coerceIn(min.z(), max.z()),
        )

        private fun Vector3f.coerceIn(min: Float, max: Float): Vector3f = set(
            x.coerceIn(min, max),
            y.coerceIn(min, max),
            z.coerceIn(min, max),
        )

        private val targetPos = Vector3f()
        private val ikPos = Vector3f()
        private val invChain = Matrix4f()
        private val chainIkPos = Vector3f()
        private val chainTargetPos = Vector3f()
        private val prevRotationInv = Quaternionf()

        private val cross = Vector3f()
        private val rot = Quaternionf()
        private val chainRot = Quaternionf()
        private val chainRotM = Matrix3f()
        private val rotXYZ = Vector3f()
        private fun solveCore(
            node: RenderNode,
            instance: ModelInstance,
            iterateCount: Int,
        ) {
            val ikPos = instance.getWorldTransform(effectorNodeIndex).getTranslation(ikPos)
            for (chain in chains) {
                if (chain.nodeIndex == node.nodeIndex) {
                    // Avoid zero result, and NaN
                    continue
                }
                val limit = chain.limit
                val axis = limit?.singleAxis
                if (axis != null) {
                    solvePlane(node, instance, iterateCount, chain, chain.limit, axis)
                    continue
                }

                val targetPos = instance.getWorldTransform(node).getTranslation(targetPos)
                val invChain = instance.getWorldTransform(chain.nodeIndex).invert(invChain)

                val chainIkPos = ikPos.mulPosition(invChain, chainIkPos)
                val chainTargetPos = targetPos.mulPosition(invChain, chainTargetPos)

                // Unnormalized vector seems never used then, so directly overwrite them
                val chainIkVec = chainIkPos.normalize()
                val chainTargetVec = chainTargetPos.normalize()

                val dot = chainTargetVec.dot(chainIkVec).coerceIn(-1f, 1f)

                var angle = acos(dot)
                // Why convert to degrees to compare? Just use radians is enough
                if (angle < 1e-5f) {
                    continue
                }
                angle = angle.coerceIn(-limitRadian, limitRadian)
                val cross = chainTargetVec.cross(chainIkVec, cross).normalize()
                val rot = rot.rotationAxis(angle, cross)

                val chainRot = instance.getTransformMap(chain.nodeIndex)
                    .getSum(transformId)
                    .getUnnormalizedRotation(chainRot)
                    .mul(rot)
                if (limit != null) {
                    val chainRotM = chainRotM.rotation(chainRot)
                    val rotXYZ = decompose(chainRotM, chain.prevAngle, rotXYZ)
                    val clampXYZ = rotXYZ.coerceIn(limit.min, limit.max)
                        .sub(chain.prevAngle).coerceIn(-limitRadian, limitRadian).add(chain.prevAngle)
                    // Don't introduce a temp r
                    chainRotM.rotationXYZ(clampXYZ.x, clampXYZ.y, clampXYZ.z)
                    chain.prevAngle.set(clampXYZ)

                    chainRotM.getUnnormalizedRotation(chainRot)
                }

                val prevRotationInv = instance.getTransformMap(chain.nodeIndex)
                    .getSum(transformId.prev)
                    .getUnnormalizedRotation(prevRotationInv).invert()
                instance.setTransformDecomposed(chain.nodeIndex, transformId) {
                    rotation.set(chainRot).mul(prevRotationInv)
                }
                instance.updateNodeTransform(chain.nodeIndex)
            }
        }

        private val rot1 = Quaternionf()
        private val targetVec1 = Vector3f()
        private val rot2 = Quaternionf()
        private val targetVec2 = Vector3f()
        private fun solvePlane(
            node: RenderNode,
            instance: ModelInstance,
            iterateCount: Int,
            chain: Chain,
            limits: Limits,
            axis: Limits.Axis,
        ) {
            val rotateAxis = axis.axis
            // Plane seems unused, so I removed it

            val ikPos = instance.getWorldTransform(effectorNodeIndex).getTranslation(ikPos)
            val targetPos = instance.getWorldTransform(node).getTranslation(targetPos)

            val invChain = instance.getWorldTransform(chain.nodeIndex).invert(invChain)

            val chainIkPos = ikPos.mulPosition(invChain, chainIkPos)
            val chainTargetPos = targetPos.mulPosition(invChain, chainTargetPos)

            // Unnormalized vector seems never used then, so directly overwrite them
            val chainIkVec = chainIkPos.normalize()
            val chainTargetVec = chainTargetPos.normalize()

            val dot = chainTargetVec.dot(chainIkVec).coerceIn(-1f, 1f)

            val angle = acos(dot).coerceIn(-limitRadian, limitRadian)
            // angleDeg is also unused

            val rot1 = rot1.rotationAxis(angle, rotateAxis)
            val targetVec1 = chainTargetVec.rotate(rot1, targetVec1)
            val dot1 = targetVec1.dot(chainIkVec)

            val rot2 = rot2.rotationAxis(-angle, rotateAxis)
            val targetVec2 = chainTargetVec.rotate(rot2, targetVec2)
            val dot2 = targetVec2.dot(chainIkVec)

            var newAngle = chain.planeModeAngle
            if (dot1 > dot2) {
                newAngle += angle
            } else {
                newAngle -= angle
            }

            val limitRange = limits.min.getAxis(axis)..limits.max.getAxis(axis)
            if (iterateCount == 0) {
                if (newAngle !in limitRange) {
                    if (-newAngle in limitRange) {
                        newAngle = -newAngle
                    } else {
                        val halfRad = (limitRange.start + limitRange.endInclusive) * .5f
                        if (abs(halfRad - newAngle) > abs(halfRad + newAngle)) {
                            newAngle = -newAngle
                        }
                    }
                }
            }

            newAngle = newAngle.coerceIn(limitRange)
            chain.planeModeAngle = newAngle

            val prevRotationInv = instance.getTransformMap(chain.nodeIndex)
                .getSum(transformId.prev)
                .getUnnormalizedRotation(prevRotationInv).invert()
            instance.setTransformDecomposed(chain.nodeIndex, transformId) {
                rotation.rotationAxis(newAngle, rotateAxis).mul(prevRotationInv)
            }
            instance.updateNodeTransform(chain.nodeIndex)
        }

        override fun update(
            phase: UpdatePhase,
            node: RenderNode,
            instance: ModelInstance,
        ) {
            when (phase) {
                is UpdatePhase.IkUpdate -> {
                    if (chains.isEmpty()) {
                        return
                    }
                    for (chain in chains) {
                        chain.prevAngle.set(0f)
                        instance.setTransformDecomposed(chain.nodeIndex, transformId) {
                            rotation.identity()
                        }
                        chain.planeModeAngle = 0f
                    }
                    instance.updateNodeTransform(chains.last().nodeIndex)

                    var maxDist = Float.MAX_VALUE
                    for (i in 0 until loopCount) {
                        solveCore(node, instance, i)

                        val targetPos = instance.getWorldTransform(node).getTranslation(targetPos)
                        val ikPos = instance.getWorldTransform(effectorNodeIndex).getTranslation(ikPos)
                        // We use distanceSquared() here, unlike original code
                        val dist = targetPos.distanceSquared(ikPos)

                        if (dist < maxDist) {
                            maxDist = dist
                            for (chain in chains) {
                                val matrix = instance.getTransformMap(chain.nodeIndex).get(transformId)
                                if (matrix != null) {
                                    matrix.getRotation(chain.saveIKRot)
                                } else {
                                    chain.saveIKRot.identity()
                                }
                            }
                        } else {
                            for (chain in chains) {
                                instance.setTransformDecomposed(chain.nodeIndex, transformId) {
                                    rotation.set(chain.saveIKRot)
                                }
                            }
                            instance.updateNodeTransform(chains.last().nodeIndex)
                            break
                        }
                    }
                }

                is UpdatePhase.DebugRender -> {
                    val consumers = phase.vertexConsumerProvider

                    val boxBuffer = consumers.getBuffer(RenderLayer.getDebugQuads())
                    for (joint in chains) {
                        val jointMatrix = phase.viewProjectionMatrix.mul(
                            instance.getWorldTransform(joint.nodeIndex),
                            phase.cacheMatrix
                        )
                        boxBuffer.drawBox(jointMatrix, 0.05f, Colors.BLUE)
                    }

                    val effectorMatrix =
                        phase.viewProjectionMatrix.mul(instance.getWorldTransform(effectorNodeIndex), phase.cacheMatrix)
                    boxBuffer.drawBox(effectorMatrix, 0.1f, Colors.RED)

                    val targetMatrix =
                        phase.viewProjectionMatrix.mul(instance.getWorldTransform(node), phase.cacheMatrix)
                    boxBuffer.drawBox(targetMatrix, 0.1f, Colors.GREEN)

                    val lineBuffer = consumers.getBuffer(DEBUG_RENDER_LAYER)
                    for (joint in chains) {
                        val jointMatrix = phase.viewProjectionMatrix.mul(
                            instance.getWorldTransform(joint.nodeIndex),
                            phase.cacheMatrix
                        )
                        val lineSize = .5f
                        lineBuffer.vertex(jointMatrix, 0f, 0f, 0f).color(Colors.RED).normal(0f, 1f, 0f)
                        lineBuffer.vertex(jointMatrix, lineSize, 0f, 0f).color(Colors.RED).normal(0f, 1f, 0f)
                        lineBuffer.vertex(jointMatrix, 0f, 0f, 0f).color(Colors.GREEN).normal(0f, 1f, 0f)
                        lineBuffer.vertex(jointMatrix, 0f, lineSize, 0f).color(Colors.GREEN).normal(0f, 1f, 0f)
                        lineBuffer.vertex(jointMatrix, 0f, 0f, 0f).color(Colors.BLUE).normal(0f, 1f, 0f)
                        lineBuffer.vertex(jointMatrix, 0f, 0f, lineSize).color(Colors.BLUE).normal(0f, 1f, 0f)
                    }
                }

                else -> {}
            }
        }
    }
}