package top.fifthlight.blazerod.render

import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.util.AbstractRefCount

class IndexBuffer(
    val type: VertexFormat.IndexType,
    val length: Int,
    val buffer: RefCountedGpuBuffer,
) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "index_buffer")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    init {
        buffer.increaseReferenceCount()
        buffer.inner.size == length * type.size
    }

    override fun onClosed() {
        buffer.decreaseReferenceCount()
    }
}

fun RenderPass.setIndexBuffer(indexBuffer: IndexBuffer) = setIndexBuffer(indexBuffer.buffer.inner, indexBuffer.type)
