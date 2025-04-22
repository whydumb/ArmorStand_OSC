package top.fifthlight.renderer.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Accessor(
    val bufferView: BufferView?,
    val byteOffset: Int = 0,
    val componentType: ComponentType,
    val normalized: Boolean = false,
    val count: Int,
    val type: AccessorType,
    val max: List<Float>? = null,
    val min: List<Float>? = null,
    val name: String? = null,
) {
    val totalByteLength
        get() = bufferView?.byteStride?.takeIf { it > 0 }?.let { stride ->
            (count - 1) * stride + componentType.byteLength * type.components
        } ?: run {
            count * componentType.byteLength * type.components
        }

    init {
        require(count >= 1) { "Bad count of accessor: $count" }
        require(byteOffset >= 0) { "Invalid byte offset for accessor: $byteOffset" }
        bufferView?.let { bufferView ->
            if (bufferView.byteStride > 0) {
                require(bufferView.byteStride % componentType.byteLength == 0) { "Invalid byte stride: ${bufferView.byteStride} (${bufferView.byteStride} % ${componentType.byteLength} != 0)" }
            }
            require(totalByteLength <= bufferView.byteLength - byteOffset) { "Bad accessor size: $totalByteLength, bufferView: ${bufferView.byteLength}, accessor offset: $byteOffset" }
        }
    }

    inline fun read(order: ByteOrder = ByteOrder.nativeOrder(), crossinline func: (ByteBuffer) -> Unit) {
        val bufferView = bufferView
        val elementLength = componentType.byteLength * type.components
        if (bufferView == null) {
            val buffer = ByteBuffer.allocate(elementLength)
            repeat(count) { func(buffer) }
            return
        }
        val buffer = bufferView.buffer.buffer.slice(byteOffset + bufferView.byteOffset, totalByteLength)
        buffer.order(order)
        val stride = bufferView.byteStride.takeIf { it > 0 } ?: elementLength
        repeat(count) {
            val position = buffer.position()
            func(buffer)
            buffer.position(position + stride)
        }
    }

    enum class ComponentType(val byteLength: Int) {
        BYTE(1),
        UNSIGNED_BYTE(1),
        SHORT(2),
        UNSIGNED_SHORT(2),
        UNSIGNED_INT(4),
        FLOAT(4),
    }

    data class ComponentTypeItem(
        val type: ComponentType,
        val normalized: Boolean = false,
    ) {
        override fun toString(): String = "$type (normalized: $normalized)"
    }

    enum class AccessorType(val components: Int) {
        SCALAR(1),
        VEC2(2),
        VEC3(3),
        VEC4(4),
        MAT2(4),
        MAT3(9),
        MAT4(16),
    }
}