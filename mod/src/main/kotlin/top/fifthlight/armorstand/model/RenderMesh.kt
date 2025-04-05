package top.fifthlight.armorstand.model

import net.minecraft.client.util.math.MatrixStack
import org.joml.Vector3f
import top.fifthlight.armorstand.util.AbstractRefCount
import kotlin.math.max
import kotlin.math.min

class RenderMesh(
    val primitives: List<RenderPrimitive>,
) : AbstractRefCount() {
    init {
        primitives.forEach { it.increaseReferenceCount() }
    }

    val positionMin: Vector3f? = run {
        if (primitives.isEmpty()) {
            return@run null
        }
        var x = Float.POSITIVE_INFINITY
        var y = Float.POSITIVE_INFINITY
        var z = Float.POSITIVE_INFINITY
        for (primitive in primitives) {
            x = min(primitive.positionMin.x(), x)
            y = min(primitive.positionMin.y(), y)
            z = min(primitive.positionMin.z(), z)
        }
        Vector3f(x, y, z)
    }

    val positionMax: Vector3f? = run {
        if (primitives.isEmpty()) {
            return@run null
        }
        var x = Float.NEGATIVE_INFINITY
        var y = Float.NEGATIVE_INFINITY
        var z = Float.NEGATIVE_INFINITY
        for (primitive in primitives) {
            x = max(primitive.positionMax.x(), x)
            y = max(primitive.positionMax.y(), y)
            z = max(primitive.positionMax.z(), z)
        }
        Vector3f(x, y, z)
    }

    fun render(matrixStack: MatrixStack, light: Int) {
        for (primitive in primitives) {
            primitive.render(matrixStack, light)
        }
    }

    override fun onClosed() {
        primitives.forEach { it.decreaseReferenceCount() }
    }
}
