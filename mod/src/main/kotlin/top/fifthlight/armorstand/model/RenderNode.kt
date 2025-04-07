package top.fifthlight.armorstand.model

import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.iteratorOf

sealed class RenderNode : AbstractRefCount(), Iterable<RenderNode> {
    var parent: RenderNode? = null

    open fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) = Unit
    open fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) = Unit

    class Group(val children: List<RenderNode>) : RenderNode() {
        init {
            children.forEach { it.increaseReferenceCount() }
        }

        override fun onClosed() {
            children.forEach { it.decreaseReferenceCount() }
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) =
            children.forEach { it.render(instance, matrixStack, globalMatrix, light) }

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) =
            children.forEach { it.update(instance, matrixStack, updateTransform) }

        override fun iterator(): Iterator<RenderNode> = children.iterator()
    }

    class Mesh(
        val mesh: RenderMesh,
        val skinIndex: Int?,
        val ignoreGlobalTransform: Boolean,
    ) : RenderNode() {
        init {
            mesh.increaseReferenceCount()
        }

        override fun onClosed() {
            mesh.decreaseReferenceCount()
        }

        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) {
            val skinData = skinIndex?.let { instance.skinData[it] }
            if (ignoreGlobalTransform) {
                mesh.render(globalMatrix, light, skinData)
            } else {
                mesh.render(matrixStack.peek().positionMatrix, light, skinData)
            }
        }

        override fun iterator() = iteratorOf<RenderNode>()
    }

    class Transform(
        val transformIndex: Int,
        val child: RenderNode,
    ) : RenderNode() {
        override fun render(instance: ModelInstance, matrixStack: MatrixStack, globalMatrix: Matrix4fc, light: Int) {
            val transform = instance.transforms[transformIndex]
            matrixStack.push()
            matrixStack.multiplyPositionMatrix(transform)
            child.render(instance, matrixStack, globalMatrix, light)
            matrixStack.pop()
        }

        override fun update(instance: ModelInstance, matrixStack: MatrixStack, updateTransform: Boolean) {
            val transformsDirty = instance.transformsDirty[transformIndex]
            if (transformsDirty) {
                instance.transformsDirty[transformIndex] = false
            }
            val updateTransform = updateTransform || transformsDirty
            val transform = instance.transforms[transformIndex]
            matrixStack.push()
            matrixStack.multiplyPositionMatrix(transform)
            child.update(instance, matrixStack, updateTransform)
            matrixStack.pop()
        }

        override fun iterator() = iteratorOf<RenderNode>(child)
    }

    class Joint(
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
    }
}
