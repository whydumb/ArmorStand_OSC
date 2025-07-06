package top.fifthlight.blazerod.model

import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import org.joml.*
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.SlottedGpuBuffer
import top.fifthlight.blazerod.util.drawBox
import top.fifthlight.blazerod.util.iteratorOf

sealed class RenderNode : AbstractRefCount(), Iterable<RenderNode> {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "node")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    var parent: RenderNode? = null

    open val containsCamera: Boolean
        get() = false
    open val doesPreUpdate: Boolean
        get() = false

    open fun updateCamera(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) = Unit

    open fun debugRender(
        instance: ModelInstance,
        matrixStack: MatrixStack,
        consumers: VertexConsumerProvider,
    ) = Unit

    open fun preUpdate(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) = Unit

    open fun update(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) = Unit

    class Group(val children: List<RenderNode>) : RenderNode() {
        override val doesPreUpdate = children.any { it.doesPreUpdate }

        init {
            children.forEach { it.increaseReferenceCount() }
        }

        override fun onClosed() {
            children.forEach { it.decreaseReferenceCount() }
        }

        override val containsCamera: Boolean = children.any { it.containsCamera }

        override fun updateCamera(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) =
            children.forEach { it.updateCamera(instance, matrixStack, updateTransform) }

        override fun debugRender(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            consumers: VertexConsumerProvider,
        ) = children.forEach { it.debugRender(instance, matrixStack, consumers) }

        override fun preUpdate(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) =
            children.forEach {
                if (it.doesPreUpdate) {
                    it.preUpdate(instance, matrixStack, updateTransform)
                }
            }

        override fun update(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) =
            children.forEach { it.update(instance, matrixStack, updateTransform) }

        override fun iterator(): Iterator<RenderNode> = children.iterator()
    }

    class Primitive(
        val primitiveIndex: Int,
        val primitive: RenderPrimitive,
        val skinIndex: Int?,
        val morphedPrimitiveIndex: Int?,
    ) : RenderNode() {
        init {
            primitive.increaseReferenceCount()
        }

        override fun onClosed() {
            primitive.decreaseReferenceCount()
        }

        override fun update(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            if (!updateTransform) {
                return
            }
            if (skinIndex == null) {
                instance.modelData.modelMatricesBuffer.setMatrix(primitiveIndex, matrixStack)
            }
        }

        fun render(instance: ModelInstance, modelViewMatrix: Matrix4fc, light: Int) {
            val skinBuffer = skinIndex?.let { instance.modelData.skinBuffers[it] }
            val targetBuffer = morphedPrimitiveIndex?.let { instance.modelData.targetBuffers[it] }
            primitive.render(instance, primitiveIndex, modelViewMatrix, light, skinBuffer, targetBuffer)
        }

        fun renderInstanced(tasks: List<RenderTask.Instance>) {
            if (RenderPassImpl.IS_DEVELOPMENT) {
                val firstInstance = tasks.first().instance
                skinIndex?.let {
                    val firstBufferSlot = firstInstance.modelData.skinBuffers[it].slot
                    require(firstBufferSlot is SlottedGpuBuffer.Slotted)
                    val buffer = firstBufferSlot.buffer
                    for (task in tasks) {
                        val bufferSlot = task.instance.modelData.skinBuffers[it].slot
                        require(bufferSlot is SlottedGpuBuffer.Slotted)
                        require(bufferSlot.buffer == buffer)
                    }
                }
                morphedPrimitiveIndex?.let {
                    val firstWeightsBufferSlot = firstInstance.modelData.targetBuffers[it].weightsSlot
                    val firstIndicesBufferSlot = firstInstance.modelData.targetBuffers[it].indicesSlot
                    require(firstWeightsBufferSlot is SlottedGpuBuffer.Slotted)
                    require(firstIndicesBufferSlot is SlottedGpuBuffer.Slotted)
                    val weightsBuffer = firstWeightsBufferSlot.buffer
                    val indicesBuffer = firstIndicesBufferSlot.buffer
                    for (task in tasks) {
                        val weightsBufferSlot = task.instance.modelData.targetBuffers[it].weightsSlot
                        val indicesBufferSlot = task.instance.modelData.targetBuffers[it].indicesSlot
                        require(weightsBufferSlot is SlottedGpuBuffer.Slotted)
                        require(indicesBufferSlot is SlottedGpuBuffer.Slotted)
                        require(weightsBufferSlot.buffer == weightsBuffer)
                        require(indicesBufferSlot.buffer == indicesBuffer)
                    }
                }
            }
            primitive.renderInstanced(tasks, this)
        }

        override fun iterator() = iteratorOf<RenderNode>()
    }

    class Transform(
        val transformIndex: Int,
        val defaultTransform: NodeTransform?,
        val child: RenderNode,
    ) : RenderNode() {
        init {
            child.increaseReferenceCount()
        }

        override val containsCamera
            get() = child.containsCamera

        override fun updateCamera(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            val transformsDirty = instance.modelData.transformsDirty[transformIndex]
            val updateTransform = updateTransform || transformsDirty
            val transform = instance.modelData.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.pushMatrix()
                matrixStack.mul(matrix)
                child.updateCamera(instance, matrixStack, updateTransform)
                matrixStack.popMatrix()
            } ?: run {
                child.updateCamera(instance, matrixStack, updateTransform)
            }
        }

        override val doesPreUpdate: Boolean
            get() = child.doesPreUpdate

        override fun debugRender(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            consumers: VertexConsumerProvider,
        ) {
            val transform = instance.modelData.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.push()
                matrixStack.multiplyPositionMatrix(matrix)
                child.debugRender(instance, matrixStack, consumers)
                matrixStack.pop()
            } ?: run {
                child.debugRender(instance, matrixStack, consumers)
            }
        }

        override fun preUpdate(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            if (!child.doesPreUpdate) {
                return
            }

            val transformsDirty = instance.modelData.transformsDirty[transformIndex]
            val updateTransform = updateTransform || transformsDirty
            val transform = instance.modelData.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.pushMatrix()
                matrixStack.mul(matrix)
                child.preUpdate(instance, matrixStack, updateTransform)
                matrixStack.popMatrix()
            } ?: run {
                child.preUpdate(instance, matrixStack, updateTransform)
            }
        }

        override fun update(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            val transformsDirty = instance.modelData.transformsDirty[transformIndex]
            if (transformsDirty) {
                instance.modelData.transformsDirty[transformIndex] = false
            }
            val updateTransform = updateTransform || transformsDirty
            val transform = instance.modelData.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.pushMatrix()
                matrixStack.mul(matrix)
                child.update(instance, matrixStack, updateTransform)
                matrixStack.popMatrix()
            } ?: run {
                child.update(instance, matrixStack, updateTransform)
            }
        }

        override fun iterator() = iteratorOf(child)

        override fun onClosed() {
            child.decreaseReferenceCount()
        }
    }

    class Joint(
        val skinIndex: Int,
        val jointIndex: Int,
    ) : RenderNode() {
        private val cacheMatrix = Matrix4f()

        private val parentJoint: Joint? by lazy {
            var current = parent
            while (current != null) {
                when (current) {
                    is Transform, is InfluenceTransform -> {
                        current = current.parent
                        continue
                    }

                    is Group -> {
                        for (sibling in current.children) {
                            if (sibling is Joint && sibling != this && sibling.skinIndex == skinIndex) {
                                return@lazy sibling
                            }
                        }
                    }

                    else -> return@lazy null
                }
                current = current.parent
            }
            null
        }
        private val debugMatrix = Matrix4f()
        override fun debugRender(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            consumers: VertexConsumerProvider,
        ) {
            matrixStack.peek().positionMatrix.get(debugMatrix)
            parentJoint?.let { parentJoint ->
                val buffer = consumers.getBuffer(RenderLayer.getDebugLineStrip(1.0))
                buffer.vertex(parentJoint.debugMatrix, 0f, 0f, 0f).color(Colors.YELLOW)
                buffer.vertex(matrixStack.peek().positionMatrix, 0f, 0f, 0f).color(Colors.RED)
            }
        }

        override fun update(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            if (!updateTransform) {
                return
            }

            val matrix = cacheMatrix
            matrix.set(matrixStack)
            val skinBuffer = instance.modelData.skinBuffers[skinIndex]

            val inverseMatrix = skinBuffer.skin.inverseBindMatrices?.get(jointIndex)
            inverseMatrix?.let { matrix.mul(it) }
            skinBuffer.setMatrix(jointIndex, matrix)
        }

        override fun iterator() = iteratorOf<RenderNode>()

        override fun onClosed() = Unit
    }

    class Ik(
        val loopCount: Int,
        val limitRadian: Float,
        val ikLinks: List<IkLinkItem>,
    ) : RenderNode() {
        init {
            require(ikLinks.isNotEmpty()) { "IK bone without links" }
        }

        data class IkLinkItem(
            val index: NodeId,
            val position: Vector3f,
            val limit: IkTarget.IkLink.Limits?,
        ) {
            constructor(link: IkTarget.IkLink) : this(
                index = link.index,
                position = Vector3f(),
                limit = link.limit,
            )
        }

        override val doesPreUpdate: Boolean
            get() = true

        override fun debugRender(instance: ModelInstance, matrixStack: MatrixStack, consumers: VertexConsumerProvider) {
            val buffer = consumers.getBuffer(RenderLayer.getDebugFilledBox())
            val matrix = matrixStack.peek().positionMatrix
            buffer.drawBox(matrix, 0.15f, Colors.CYAN)
        }

        private val targetPos = Vector3f()
        override fun preUpdate(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            matrixStack.getTranslation(targetPos)

            for (i in 0 until loopCount) {
                val last = ikLinks.last()
            }
        }

        override fun iterator() = iteratorOf<RenderNode>()

        override fun onClosed() = Unit
    }

    class InfluenceSource : RenderNode() {
        override fun onClosed() = Unit

        override fun iterator() = iteratorOf<RenderNode>()

        override fun debugRender(instance: ModelInstance, matrixStack: MatrixStack, consumers: VertexConsumerProvider) {
            matrixStack.push()
            val matrix = matrixStack.peek().positionMatrix
            val buffer = consumers.getBuffer(RenderLayer.getDebugFilledBox())
            buffer.drawBox(matrix, 0.1f, Colors.RED)
            matrixStack.pop()
        }
    }

    class InfluenceTransform(
        val child: RenderNode,
        val sourceNodeIds: List<NodeId>,
        val influence: Float,
        val relative: Boolean,
        val influenceRotation: Boolean = false,
        val influenceTranslation: Boolean = false,
    ) : RenderNode() {
        init {
            child.increaseReferenceCount()
        }

        override fun onClosed() {
            child.decreaseReferenceCount()
        }

        override val doesPreUpdate: Boolean
            get() = true

        override val containsCamera: Boolean
            get() = child.containsCamera

        private val currentRotation = Quaternionf()
        private val currentTranslation = Vector3f()
        private val defaultRotation = Quaternionf()
        private val defaultTranslation = Vector3f()
        private val targetRotation = Quaternionf()
        private val targetTranslation = Vector3f()
        private fun applyTransform(instance: ModelInstance, matrix4f: Matrix4f) {
            targetTranslation.set(0f)
            targetRotation.identity()
            for (sourceIndex in sourceNodeIds) {
                val transformIndex = instance.scene.nodeIdToTransformMap.getInt(sourceIndex)
                val sourceCurrent = instance.modelData.transforms[transformIndex]
                val sourceDefault = instance.modelData.defaultTransforms[transformIndex]
                if (influenceTranslation) {
                    sourceCurrent?.getTranslation(currentTranslation) ?: currentTranslation.set(0f)
                    if (relative) {
                        sourceDefault?.let { sourceDefault ->
                            sourceDefault.getTranslation(defaultTranslation)
                            currentTranslation.sub(defaultTranslation)
                        }
                    }
                    targetTranslation.mulAdd(influence, currentTranslation)
                }
                if (influenceRotation) {
                    sourceCurrent?.getRotation(currentRotation) ?: currentRotation.identity()
                    if (relative) {
                        sourceDefault?.let { sourceDefault ->
                            sourceDefault.getRotation(defaultRotation)
                            currentRotation.mul(defaultRotation.invert())
                        }
                    }
                    targetRotation.mul(currentRotation, targetRotation)
                }
            }
            matrix4f.rotate(targetRotation)
            matrix4f.translate(targetTranslation)
        }

        override fun updateCamera(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            if (!child.containsCamera) {
                return
            }

            matrixStack.pushMatrix()
            applyTransform(instance, matrixStack)
            child.updateCamera(instance, matrixStack, true)
            matrixStack.popMatrix()
        }

        override fun preUpdate(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            matrixStack.pushMatrix()
            applyTransform(instance, matrixStack)
            child.preUpdate(instance, matrixStack, true)
            matrixStack.popMatrix()
        }

        override fun update(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            matrixStack.pushMatrix()
            applyTransform(instance, matrixStack)
            child.update(instance, matrixStack, true)
            matrixStack.popMatrix()
        }

        override fun debugRender(instance: ModelInstance, matrixStack: MatrixStack, consumers: VertexConsumerProvider) {
            matrixStack.push()
            val matrix = matrixStack.peek().positionMatrix
            applyTransform(instance, matrix)
            val buffer = consumers.getBuffer(RenderLayer.getDebugFilledBox())
            buffer.drawBox(matrix, 0.1f, Colors.BLUE)
            child.debugRender(instance, matrixStack, consumers)
            matrixStack.pop()
        }

        override fun iterator() = iteratorOf(child)
    }

    class Camera(
        val cameraTransformIndex: Int,
    ) : RenderNode() {
        override val doesPreUpdate: Boolean
            get() = false

        override val containsCamera: Boolean
            get() = true

        override fun onClosed() = Unit

        override fun updateCamera(instance: ModelInstance, matrixStack: Matrix4fStack, updateTransform: Boolean) {
            val cameraTransform = instance.modelData.cameraTransforms[cameraTransformIndex]
            cameraTransform.update(matrixStack)
        }

        override fun iterator() = iteratorOf<RenderNode>()
    }
}
