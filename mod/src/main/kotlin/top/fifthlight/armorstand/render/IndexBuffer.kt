package top.fifthlight.armorstand.render

import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.vertex.VertexFormat
import top.fifthlight.armorstand.util.AbstractRefCount

class IndexBuffer(
    val type: VertexFormat.IndexType,
    val length: Int,
    val buffer: RefCountedGpuBuffer,
) : AbstractRefCount() {
    init {
        buffer.increaseReferenceCount()
        buffer.inner.size == length * type.size
    }

    override fun onClosed() {
        buffer.decreaseReferenceCount()
    }
}

fun RenderPass.setIndexBuffer(indexBuffer: IndexBuffer) = setIndexBuffer(indexBuffer.buffer.inner, indexBuffer.type)