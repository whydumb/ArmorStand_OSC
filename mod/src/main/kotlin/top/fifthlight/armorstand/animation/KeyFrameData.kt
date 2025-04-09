package top.fifthlight.armorstand.animation

import org.joml.Quaternionf
import org.joml.Vector3f

interface KeyFrameData<T> {
    fun get(index: Int, stride: Int, data: List<T>)
}

class Vector3KeyFrameData(private val values: FloatArray) : KeyFrameData<Vector3f> {
    override fun get(index: Int, stride: Int, data: List<Vector3f>) {
        val baseOffset = index * stride * 3
        for (i in 0 until stride) {
            val offset = baseOffset + i * 3
            data[i].set(values[offset], values[offset + 1], values[offset + 2])
        }
    }
}

class QuaternionKeyFrameData(private val values: FloatArray) : KeyFrameData<Quaternionf> {
    override fun get(index: Int, stride: Int, data: List<Quaternionf>) {
        val baseOffset = index * stride * 4
        for (i in 0 until stride) {
            val offset = baseOffset + i * 4
            data[i].set(values[offset], values[offset + 1], values[offset + 2], values[offset + 3])
        }
    }
}