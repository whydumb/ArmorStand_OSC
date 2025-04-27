package top.fifthlight.armorstand.model

import net.minecraft.util.Identifier
import org.joml.Matrix4fc
import top.fifthlight.armorstand.util.AbstractRefCount

class RenderMesh(
    val primitives: List<RenderPrimitive>,
) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "mesh")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    init {
        primitives.forEach { it.increaseReferenceCount() }
    }

    fun render(matrix: Matrix4fc, light: Int, skin: RenderSkinData?) {
        for (primitive in primitives) {
            primitive.render(matrix, light, skin)
        }
    }

    fun schedule(matrix: Matrix4fc, light: Int, skin: RenderSkinData?, onTaskScheduled: (RenderTask<*, *>) -> Unit) {
        for (primitive in primitives) {
            primitive.schedule(matrix, light, skin, onTaskScheduled)
        }
    }

    override fun onClosed() {
        primitives.forEach { it.decreaseReferenceCount() }
    }
}
