package top.fifthlight.armorstand.render

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import top.fifthlight.armorstand.model.VertexType
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.Accessor
import top.fifthlight.renderer.model.Primitive

abstract class VertexBuffer : AbstractRefCount() {
    abstract val mode: VertexFormat.DrawMode
    abstract val elements: List<VertexElement>
    abstract val verticesCount: Int

    val type by lazy { VertexType(elements.map { it.usage }) }

    class VertexElement(
        val buffer: RefCountedGpuBuffer,
        val offset: Int,
        val stride: Int,
        val usage: Primitive.Attributes.Key,
        val type: VertexFormatElement.Type,
        val componentType: Accessor.AccessorType,
        val normalized: Boolean,
    ) : AbstractRefCount() {
        init {
            buffer.increaseReferenceCount()
        }

        override fun onClosed() {
            buffer.decreaseReferenceCount()
        }
    }

    override fun onClosed() {
        elements.forEach { it.decreaseReferenceCount() }
    }
}