package top.fifthlight.armorstand.animation

import org.joml.Quaternionf
import org.joml.Vector3f

interface KeyFrameData<T> {
    val frames: Int
    val stride: Int
    fun get(index: Int, data: List<T>)
}

class Vector3KeyFrameData(
    private val values: FloatArray,
    override val stride: Int,
) : KeyFrameData<Vector3f> {
    init {
        require(values.size % (stride * 3) == 0) {
            "Invalid data size ${values.size} for stride $stride (requires multiple of ${stride * 3})"
        }
    }

    override val frames = values.size / (stride * 3)

    override fun get(index: Int, data: List<Vector3f>) {
        val baseOffset = index * stride * 3
        for (i in 0 until stride) {
            val offset = baseOffset + i * 3
            data[i].set(values[offset], values[offset + 1], values[offset + 2])
        }
    }
}

class QuaternionKeyFrameData(
    private val values: FloatArray,
    override val stride: Int,
) : KeyFrameData<Quaternionf> {
    init {
        require(values.size % (stride * 4) == 0) {
            "Invalid data size ${values.size} for stride $stride (requires multiple of ${stride * 4})"
        }
    }

    override val frames = values.size / (stride * 4)

    override fun get(index: Int, data: List<Quaternionf>) {
        val baseOffset = index * stride * 4
        for (i in 0 until stride) {
            val offset = baseOffset + i * 4
            data[i].set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3])
        }
    }
}
