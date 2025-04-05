package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import net.minecraft.client.util.math.MatrixStack
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.iteratorOf
import top.fifthlight.renderer.model.NodeTransform

sealed class RenderNode: AbstractRefCount(), Iterable<RenderNode> {
    var parent: RenderNode? = null

    open fun render(matrixStack: MatrixStack, light: Int) = Unit

    interface Updatable {
        fun update(device: GpuDevice, commandEncoder: CommandEncoder)
    }

    class Group(val children: List<RenderNode>): RenderNode() {
        init {
            children.forEach { it.increaseReferenceCount() }
        }

        override fun onClosed() {
            children.forEach { it.decreaseReferenceCount() }
        }

        override fun render(matrixStack: MatrixStack, light: Int) = children.forEach { it.render(matrixStack, light) }

        override fun iterator(): Iterator<RenderNode> = children.iterator()
    }

    class Mesh(val mesh: RenderMesh): RenderNode() {
        init {
            mesh.increaseReferenceCount()
        }

        override fun onClosed() {
            mesh.decreaseReferenceCount()
        }

        override fun render(matrixStack: MatrixStack, light: Int) = mesh.render(matrixStack, light)

        override fun iterator() = iteratorOf<RenderNode>()
    }

    class Transform(
        val transform: NodeTransform,
        val child: RenderNode,
    ): RenderNode() {
        override fun render(matrixStack: MatrixStack, light: Int) {
            matrixStack.push()
            matrixStack.multiplyPositionMatrix(transform.matrix)
            child.render(matrixStack, light)
            matrixStack.pop()
        }

        override fun iterator() = iteratorOf<RenderNode>(child)
    }

    class Joint(
        val skin: RenderSkin,
        val jointIndex: Int,
    ): RenderNode() {
        override fun iterator() = iteratorOf<RenderNode>()
    }
}
