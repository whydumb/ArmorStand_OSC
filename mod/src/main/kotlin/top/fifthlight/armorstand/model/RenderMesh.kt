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

    fun render(matrixStack: MatrixStack, light: Int) {
        for (primitive in primitives) {
            primitive.render(matrixStack, light)
        }
    }

    override fun onClosed() {
        primitives.forEach { it.decreaseReferenceCount() }
    }
}
