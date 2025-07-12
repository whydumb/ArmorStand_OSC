package top.fifthlight.blazerod.render

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.model.resource.VertexType
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.model.Accessor
import top.fifthlight.blazerod.model.Primitive

abstract class VertexBuffer : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "vertex_buffer")
    }

    override val typeId: Identifier
        get() = TYPE_ID

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
        companion object {
            private val TYPE_ID = Identifier.of("blazerod", "vertex_element")
        }

        override val typeId: Identifier
            get() = TYPE_ID

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