package top.fifthlight.blazerod.model.data

import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.util.SlottedGpuBuffer

class ModelMatricesBuffer(
    scene: RenderScene,
    val slot: SlottedGpuBuffer.Slot,
) : AutoCloseable {
    companion object {
        private val IDENTITY = Matrix4f()
        const val MAT4X4_SIZE = 4 * 4 * 4
    }

    init {
        require(slot.size == scene.primitiveNodes.size * MAT4X4_SIZE) { "Slot size mismatch: want ${scene.primitiveNodes.size * MAT4X4_SIZE}, but got ${slot.size}" }
    }

    private val primitiveNodesSize = scene.primitiveNodes.size

    fun clear() = slot.edit {
        repeat(primitiveNodesSize) {
            IDENTITY.get(it * MAT4X4_SIZE, this)
        }
    }

    override fun close() = slot.close()

    fun setMatrix(index: Int, matrix4f: Matrix4fc) = slot.edit {
        matrix4f.get(index * MAT4X4_SIZE, this)
    }
}