package top.fifthlight.blazerod.model.data

import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.RenderSkin
import top.fifthlight.blazerod.util.SlottedGpuBuffer

class RenderSkinBuffer(
    val skin: RenderSkin,
    val slot: SlottedGpuBuffer.Slot,
) : AutoCloseable {
    companion object {
        private val IDENTITY = Matrix4f()
        const val MAT4X4_SIZE = 4 * 4 * 4
    }

    init {
        require(slot.size == skin.jointSize * MAT4X4_SIZE) {
            "Bad slot size: ${slot.size}, want ${skin.jointSize * MAT4X4_SIZE}"
        }
    }

    override fun close() = slot.close()

    fun clear() = slot.edit {
        repeat(skin.jointSize) {
            IDENTITY.get(it * MAT4X4_SIZE, this)
        }
    }

    fun setMatrix(index: Int, matrix4f: Matrix4fc) = slot.edit {
        matrix4f.get(index * MAT4X4_SIZE, this)
    }
}