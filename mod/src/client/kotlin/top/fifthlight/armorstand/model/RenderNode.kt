package top.fifthlight.armorstand.model

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.iteratorOf

sealed class RenderNode : AbstractRefCount(), Iterable<RenderNode> {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "node")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    var parent: RenderNode? = null

    open fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) = Unit
    open fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) = Unit
    open fun schedule(
        instance: ModelInstance,
        matrixStack: MatrixStack,
        globalMatrix: Matrix4fc,
        light: Int,
        onTaskScheduled: (RenderTask<*, *>) -> Unit
    ) = Unit

    class Group(val children: List<RenderNode>) : RenderNode() {
        init {
            children.forEach { it.increaseReferenceCount() }
        }

        override fun onClosed() {
            children.forEach { it.decreaseReferenceCount() }
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) =
            children.forEach { it.render(instance, matrixStack, globalMatrix, light) }

        override fun schedule(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            globalMatrix: Matrix4fc,
            light: Int,
            onTaskScheduled: (RenderTask<*, *>) -> Unit
        ) = children.forEach { it.schedule(instance, matrixStack, globalMatrix, light, onTaskScheduled) }

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) =
            children.forEach { it.update(instance, matrixStack, updateTransform) }

        override fun iterator(): Iterator<RenderNode> = children.iterator()
    }

    class Mesh(
        val mesh: RenderMesh,
        val skinIndex: Int?,
    ) : RenderNode() {
        init {
            mesh.increaseReferenceCount()
        }

        override fun onClosed() {
            mesh.decreaseReferenceCount()
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) {
            val skinData = skinIndex?.let { instance.skinData[it] }
            if (skinData != null) {
                mesh.render(globalMatrix, light, skinData)
            } else {
                mesh.render(matrixStack.peek().positionMatrix, light, skinData)
            }
        }

        override fun schedule(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            globalMatrix: Matrix4fc,
            light: Int,
            onTaskScheduled: (RenderTask<*, *>) -> Unit
        ) {
            val skinData = skinIndex?.let { instance.skinData[it] }
            val matrix = if (skinData != null) {
                globalMatrix
            } else {
                matrixStack.peek().positionMatrix
            }
            mesh.schedule(matrix, light, skinData, onTaskScheduled)
        }

        override fun iterator() = iteratorOf<RenderNode>()
    }

    class Transform(
        val transformIndex: Int,
        val child: RenderNode,
    ) : RenderNode() {
        init {
            child.increaseReferenceCount()
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) {
            val transform = instance.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.push()
                matrixStack.multiplyPositionMatrix(matrix)
                child.render(instance, matrixStack, globalMatrix, light)
                matrixStack.pop()
            } ?: run {
                child.render(instance, matrixStack, globalMatrix, light)
            }
        }

        override fun schedule(
            instance: ModelInstance,
            matrixStack: MatrixStack,
            globalMatrix: Matrix4fc,
            light: Int,
            onTaskScheduled: (RenderTask<*, *>) -> Unit
        ) {
            val transform = instance.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.push()
                matrixStack.multiplyPositionMatrix(matrix)
                child.schedule(instance, matrixStack, globalMatrix, light, onTaskScheduled)
                matrixStack.pop()
            } ?: run {
                child.schedule(instance, matrixStack, globalMatrix, light, onTaskScheduled)
            }
        }

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) {
            val transformsDirty = instance.transformsDirty[transformIndex]
            if (transformsDirty) {
                instance.transformsDirty[transformIndex] = false
            }
            val updateTransform = updateTransform || transformsDirty
            val transform = instance.transforms[transformIndex]
            transform?.matrix?.let { matrix ->
                matrixStack.push()
                matrixStack.multiplyPositionMatrix(matrix)
                child.update(instance, matrixStack, updateTransform)
                matrixStack.pop()
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
        val name: String?,
        val skinIndex: Int,
        val jointIndex: Int,
    ) : RenderNode() {
        private val cacheMatrix = Matrix4f()

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) {
            if (!updateTransform) {
                return
            }

            val matrix = cacheMatrix
            matrix.set(matrixStack.peek().positionMatrix)
            val skinData = instance.skinData[skinIndex]

            val inverseMatrix = skinData.skin.inverseBindMatrices?.get(jointIndex)
            inverseMatrix?.let { matrix.mul(it) }
            skinData.setMatrix(jointIndex, matrix)
        }

        override fun iterator() = iteratorOf<RenderNode>()

        override fun onClosed() {}
    }
}
