package top.fifthlight.blazerod.model.animation

import it.unimi.dsi.fastutil.floats.FloatList
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.Accessor
import top.fifthlight.blazerod.model.NodeTransform

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface AnimationKeyFrameData<T> {
    val frames: Int
    val elements: Int
    fun get(index: Int, data: List<T>)

    companion object
}

class FloatListAnimationKeyFrameData<T>(
    private val values: FloatList,
    override val elements: Int,
    private val componentCount: Int,
    private val elementGetter: (list: FloatList, offset: Int, result: T) -> Unit,
) : AnimationKeyFrameData<T> {
    init {
        require(values.size % (elements * componentCount) == 0) {
            "Invalid data size ${values.size} for elements $elements (requires multiple of ${elements * componentCount})"
        }
    }

    override val frames = values.size / (elements * componentCount)

    override fun get(index: Int, data: List<T>) {
        val baseOffset = index * elements * componentCount
        for (i in 0 until elements) {
            val offset = baseOffset + i * componentCount
            elementGetter(values, offset, data[i])
        }
    }
}

fun AnimationKeyFrameData.Companion.ofVector3f(
    values: FloatList,
    elements: Int,
) = FloatListAnimationKeyFrameData<Vector3f>(
    values = values,
    elements = elements,
    componentCount = 3,
    elementGetter = { list, offset, result -> result.set(list.getFloat(offset), list.getFloat(offset + 1), list.getFloat(offset + 2)) },
)

fun AnimationKeyFrameData.Companion.ofQuaternionf(
    values: FloatList,
    elements: Int,
) = FloatListAnimationKeyFrameData<Quaternionf>(
    values = values,
    elements = elements,
    componentCount = 4,
    elementGetter = { list, offset, result ->
        result.set(
            list.getFloat(offset),
            list.getFloat(offset + 1),
            list.getFloat(offset + 2),
            list.getFloat(offset + 3)
        )
    },
)

fun AnimationKeyFrameData.Companion.ofDecomposedNodeTransform(
    values: FloatList,
    elements: Int,
) = FloatListAnimationKeyFrameData<NodeTransform.Decomposed>(
    values = values,
    elements = elements,
    componentCount = 10,
    elementGetter = { list, offset, result ->
        result.translation.set(list.getFloat(offset), list.getFloat(offset + 1), list.getFloat(2))
        result.rotation.set(list.getFloat(offset + 3), list.getFloat(offset + 4), list.getFloat(offset + 5), list.getFloat(offset + 6))
        result.scale.set(list.getFloat(offset + 7), list.getFloat(offset + 8), list.getFloat(offset + 9))
    }
)

class AccessorAnimationKeyFrameData<T>(
    private val accessor: Accessor,
    override val elements: Int,
    private val elementGetter: (buffer: ByteBuffer, result: T) -> Unit,
) : AnimationKeyFrameData<T> {
    init {
        require(accessor.count % elements == 0) { "Invalid data size ${accessor.count} for elements $elements" }
        accessor.bufferView?.let { require(it.byteStride == 0) { "Byte stride is not zero" } }
    }

    override val frames = accessor.count / elements

    private val isZeroFilled = accessor.bufferView == null
    private val itemLength = accessor.componentType.byteLength * accessor.type.components
    private val elementLength = itemLength * elements
    private val slice = accessor.bufferView?.let { bufferView ->
        bufferView.buffer.buffer
            .slice(accessor.byteOffset + bufferView.byteOffset, accessor.totalByteLength)
            .order(ByteOrder.LITTLE_ENDIAN)
    } ?: run {
        ByteBuffer.allocate(itemLength).order(ByteOrder.LITTLE_ENDIAN)
    }

    override fun get(index: Int, data: List<T>) {
        var position = index * elementLength
        for (i in 0 until elements) {
            if (isZeroFilled) {
                slice.clear()
                elementGetter(slice, data[i])
            } else {
                slice.clear()
                slice.position(position)
                slice.limit(position + itemLength)
                elementGetter(slice, data[i])
                position += itemLength
            }
        }
    }
}
