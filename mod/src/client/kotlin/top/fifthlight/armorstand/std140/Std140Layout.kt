package top.fifthlight.armorstand.std140

import it.unimi.dsi.fastutil.floats.AbstractFloatList
import it.unimi.dsi.fastutil.floats.FloatList
import it.unimi.dsi.fastutil.ints.AbstractIntList
import it.unimi.dsi.fastutil.ints.IntList
import org.joml.*
import java.nio.ByteBuffer
import kotlin.reflect.KProperty

abstract class Std140Layout<L : Std140Layout<L>> {
    private var offset = 0
    private var buffer: ByteBuffer? = null

    val totalSize: Int
        get() = offset

    sealed class Field<T, L : Std140Layout<L>>(layout: Std140Layout<L>, align: Int, size: Int) {
        val offset: Int

        operator fun setValue(thisRef: Std140Layout<L>, property: KProperty<*>, value: T) {
            setValue(checkNotNull(thisRef.buffer) { "No buffer bound to layout" }, value)
        }

        operator fun getValue(thisRef: Std140Layout<L>, property: KProperty<*>): T =
            getValue(checkNotNull(thisRef.buffer) { "No buffer bound to layout" })

        abstract fun setValue(buffer: ByteBuffer, value: T)
        abstract fun getValue(buffer: ByteBuffer): T

        init {
            val padding = when (val remainder = layout.offset % align) {
                0 -> 0
                else -> align - remainder
            }
            offset = layout.offset + padding
            layout.offset += padding + size
        }

        class IntField<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Int, L>(
            layout = layout,
            align = 4,
            size = 4,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Int) {
                buffer.putInt(offset, value)
            }

            override fun getValue(buffer: ByteBuffer) = buffer.getInt(offset)
        }

        class FloatField<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Float, L>(
            layout = layout,
            align = 4,
            size = 4,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Float) {
                buffer.putFloat(offset, value)
            }

            override fun getValue(buffer: ByteBuffer) = buffer.getFloat(offset)
        }

        class Vec2Field<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Vector2fc, L>(
            layout = layout,
            align = 8,
            size = 8,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Vector2fc) {
                value.get(offset, buffer)
            }

            private val vector = Vector2f()

            override fun getValue(buffer: ByteBuffer) = vector.apply {
                set(offset, buffer)
            }
        }

        class Vec3Field<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Vector3fc, L>(
            layout = layout,
            align = 16,
            size = 12,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Vector3fc) {
                value.get(offset, buffer)
            }

            private val vector = Vector3f()

            override fun getValue(buffer: ByteBuffer) = vector.apply {
                set(offset, buffer)
            }
        }

        class Vec4Field<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Vector4fc, L>(
            layout = layout,
            align = 16,
            size = 16,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Vector4fc) {
                value.get(offset, buffer)
            }

            private val vector = Vector4f()

            override fun getValue(buffer: ByteBuffer) = vector.apply {
                set(offset, buffer)
            }
        }

        class Mat2Field<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Matrix2fc, L>(
            layout = layout,
            align = 8,
            size = 16,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Matrix2fc) {
                value.get(offset, buffer)
            }

            private val matrix = Matrix2f()

            override fun getValue(buffer: ByteBuffer) = matrix.apply {
                set(offset, buffer)
            }
        }

        class Mat3Field<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Matrix3fc, L>(
            layout = layout,
            align = 16,
            size = 48,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Matrix3fc) {
                buffer.putFloat(offset + 0, value.m00())
                buffer.putFloat(offset + 4, value.m01())
                buffer.putFloat(offset + 8, value.m02())
                buffer.putFloat(offset + 16, value.m10())
                buffer.putFloat(offset + 20, value.m11())
                buffer.putFloat(offset + 24, value.m12())
                buffer.putFloat(offset + 32, value.m20())
                buffer.putFloat(offset + 36, value.m21())
                buffer.putFloat(offset + 40, value.m22())
            }

            private val matrix = Matrix3f()

            override fun getValue(buffer: ByteBuffer) = matrix.apply {
                m00(buffer.getFloat(offset + 0))
                m01(buffer.getFloat(offset + 4))
                m02(buffer.getFloat(offset + 8))
                m10(buffer.getFloat(offset + 16))
                m11(buffer.getFloat(offset + 20))
                m12(buffer.getFloat(offset + 24))
                m20(buffer.getFloat(offset + 32))
                m21(buffer.getFloat(offset + 36))
                m22(buffer.getFloat(offset + 40))
            }
        }

        class Mat4Field<L : Std140Layout<L>>(layout: Std140Layout<L>) : Field<Matrix4fc, L>(
            layout = layout,
            align = 16,
            size = 64,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Matrix4fc) {
                value.get(offset, buffer)
            }

            private val matrix = Matrix4f()

            override fun getValue(buffer: ByteBuffer) = matrix.apply {
                set(offset, buffer)
            }
        }

        class IntArray<L : Std140Layout<L>>(layout: Std140Layout<L>, private val length: Int) : Field<IntList, L>(
            layout = layout,
            align = 4,
            size = 4 * length,
        ) {
            override fun setValue(buffer: ByteBuffer, value: IntList) {
                require(value.size == length) { "List size not match: expected $length, but got ${value.size}" }
                repeat(length) { index ->
                    val item = value.getInt(index)
                    buffer.putInt(offset + index * 4, item)
                }
            }

            private val list = object : AbstractIntList() {
                var buffer: ByteBuffer? = null

                override fun add(key: Int?) = throw UnsupportedOperationException()
                override fun remove(key: Int?) = throw UnsupportedOperationException()

                override val size: Int
                    get() = length

                override fun getInt(index: Int): Int {
                    if (index !in 0 until length) {
                        throw IndexOutOfBoundsException(index)
                    }
                    return requireNotNull(buffer).getInt(offset + index * 4)
                }

                override fun set(index: Int, k: Int): Int = getInt(index).also {
                    requireNotNull(buffer).putInt(offset + index * 4, k)
                }
            }

            override fun getValue(buffer: ByteBuffer) = list.also { it.buffer = buffer }
        }

        class FloatArray<L : Std140Layout<L>>(layout: Std140Layout<L>, private val length: Int) : Field<FloatList, L>(
            layout = layout,
            align = 4,
            size = 4 * length,
        ) {
            override fun setValue(buffer: ByteBuffer, value: FloatList) {
                require(value.size == length) { "List size not match: expected $length, but got ${value.size}" }
                repeat(length) { index ->
                    val item = value.getFloat(index)
                    buffer.putFloat(offset + index * 4, item)
                }
            }

            private val list = object : AbstractFloatList() {
                var buffer: ByteBuffer? = null

                override fun add(key: Float?) = throw UnsupportedOperationException()
                override fun remove(key: Float?) = throw UnsupportedOperationException()

                override val size: Int
                    get() = length

                override fun getFloat(index: Int): Float {
                    if (index !in 0 until length) {
                        throw IndexOutOfBoundsException(index)
                    }
                    return requireNotNull(buffer).getFloat(offset + index * 4)
                }

                override fun set(index: Int, k: Float): Float = getFloat(index).also {
                    requireNotNull(buffer).putFloat(offset + index * 4, k)
                }
            }

            override fun getValue(buffer: ByteBuffer) = list.also { it.buffer = buffer }
        }
    }

    fun int() = Field.IntField(this)
    fun float() = Field.FloatField(this)

    fun vec2() = Field.Vec2Field(this)
    fun vec3() = Field.Vec3Field(this)
    fun vec4() = Field.Vec4Field(this)

    fun mat2() = Field.Mat2Field(this)
    fun mat3() = Field.Mat3Field(this)
    fun mat4() = Field.Mat4Field(this)

    fun intArray(size: Int) = Field.IntArray(this, size)
    fun floatArray(size: Int) = Field.FloatArray(this, size)

    fun withBuffer(buffer: ByteBuffer, block: L.() -> Unit) {
        try {
            this.buffer = buffer
            @Suppress("UNCHECKED_CAST")
            block.invoke(this as L)
        } finally {
            this.buffer = null
        }
    }
}
