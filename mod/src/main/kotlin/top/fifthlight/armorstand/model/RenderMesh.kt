package top.fifthlight.armorstand.model

import org.joml.Matrix4fc
import top.fifthlight.armorstand.util.AbstractRefCount

class RenderMesh(
    val primitives: List<RenderPrimitive>,
) : AbstractRefCount() {
    init {
        primitives.forEach { it.increaseReferenceCount() }
    }

    fun render(matrix: Matrix4fc, light: Int, skin: RenderSkinData?) {
        for (primitive in primitives) {
            primitive.render(matrix, light, skin)
        }
    }

    override fun onClosed() {
        primitives.forEach { it.decreaseReferenceCount() }
    }
}
