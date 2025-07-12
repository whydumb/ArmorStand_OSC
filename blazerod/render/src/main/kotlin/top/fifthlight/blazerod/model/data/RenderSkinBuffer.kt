package top.fifthlight.blazerod.model.data

import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.resource.RenderSkin
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.CowBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RenderSkinBuffer private constructor(
    val jointSize: Int,
) : CowBuffer.Content<RenderSkinBuffer>,
    AbstractRefCount() {
    companion object {
        private val IDENTITY = Matrix4f()
        const val MAT4X4_SIZE = 4 * 4 * 4
        private val TYPE_ID = Identifier.of("blazerod", "render_skin_buffer")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    constructor(skin: RenderSkin) : this(skin.jointSize)

    val buffer: ByteBuffer = ByteBuffer.allocateDirect(jointSize * MAT4X4_SIZE).order(ByteOrder.nativeOrder())

    fun clear() {
        repeat(jointSize) {
            buffer.position(it * MAT4X4_SIZE)
            IDENTITY.get(it * MAT4X4_SIZE, buffer)
        }
        buffer.rewind()
    }

    fun setMatrix(index: Int, matrix4f: Matrix4fc) {
        buffer.position(index * MAT4X4_SIZE)
        matrix4f.get(index * MAT4X4_SIZE, buffer)
    }

    override fun copy(): RenderSkinBuffer = RenderSkinBuffer(jointSize).also {
        buffer.rewind()
        it.buffer.put(buffer)
    }

    override fun onClosed() = Unit
}