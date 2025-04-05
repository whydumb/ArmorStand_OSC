package top.fifthlight.armorstand.model

import net.minecraft.client.util.math.MatrixStack
import org.joml.Vector3f
import org.joml.Vector4f
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.NodeTransform
import kotlin.math.max
import kotlin.math.min

class RenderNode(
    val children: List<RenderNode> = listOf(),
    val transform: NodeTransform? = null,
    val mesh: RenderMesh? = null,
) : AbstractRefCount() {
    init {
        mesh?.increaseReferenceCount()
        children.forEach { it.increaseReferenceCount() }
    }

    val positionMin: Vector3f? = run {
        var x = Float.POSITIVE_INFINITY
        var y = Float.POSITIVE_INFINITY
        var z = Float.POSITIVE_INFINITY
        var havePoint = false
        fun point(vec: Vector3f) {
            havePoint = true
            x = min(vec.x(), x)
            y = min(vec.y(), y)
            z = min(vec.z(), z)
        }
        for (child in children) {
            child.positionMin?.let { point(it) }
        }
        mesh?.positionMin?.let { point(it) }
        if (havePoint) {
            transform?.matrix?.let { matrix ->
                Vector4f(x, y, z, 0f).mul(matrix).xyz(Vector3f())
            } ?: run {
                Vector3f(x, y, z)
            }
        } else {
            null
        }
    }

    val positionMax: Vector3f? = run {
        var x = Float.NEGATIVE_INFINITY
        var y = Float.NEGATIVE_INFINITY
        var z = Float.NEGATIVE_INFINITY
        var havePoint = false
        fun point(vec: Vector3f) {
            havePoint = true
            x = max(vec.x(), x)
            y = max(vec.y(), y)
            z = max(vec.z(), z)
        }
        for (child in children) {
            child.positionMax?.let { point(it) }
        }
        mesh?.positionMax?.let { point(it) }
        if (havePoint) {
            transform?.matrix?.let { matrix ->
                Vector4f(x, y, z, 0f).mul(matrix).xyz(Vector3f())
            } ?: run {
                Vector3f(x, y, z)
            }
        } else {
            null
        }
    }

    private fun renderMeshAndChildren(matrixStack: MatrixStack, light: Int) {
        mesh?.render(matrixStack, light)
        for (node in children) {
            node.render(matrixStack, light)
        }
    }

    fun render(matrixStack: MatrixStack, light: Int) {
        transform?.let {
            matrixStack.push()
            matrixStack.multiplyPositionMatrix(transform.matrix)
        }
        renderMeshAndChildren(matrixStack, light)
        transform?.let {
            matrixStack.pop()
        }
    }

    override fun onClosed() {
        mesh?.decreaseReferenceCount()
        children.forEach { it.decreaseReferenceCount() }
    }
}
