package top.fifthlight.armorstand.std140

import it.unimi.dsi.fastutil.floats.AbstractFloatList
import it.unimi.dsi.fastutil.floats.FloatList
import it.unimi.dsi.fastutil.ints.AbstractIntList
import it.unimi.dsi.fastutil.ints.IntList
import org.joml.*
import java.nio.ByteBuffer
import java.util.Objects.checkIndex
import kotlin.reflect.KProperty

abstract class Std140Layout<L : Std140Layout<L>> {
    private var offset = 0
    private var buffer: ByteBuffer? = null

    val totalSize: Int
        get() = offset

    sealed class Field<T>(layout: Std140Layout<*>, align: Int, size: Int) {
        val offset: Int

        operator fun setValue(thisRef: Std140Layout<*>, property: KProperty<*>, value: T) {
            setValue(checkNotNull(thisRef.buffer) { "No buffer bound to layout" }, value)
        }

        operator fun getValue(thisRef: Std140Layout<*>, property: KProperty<*>): T =
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

        class IntField(layout: Std140Layout<*>) : Field<Int>(
            layout = layout,
            align = 4,
            size = 4,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Int) {
                buffer.putInt(offset, value)
            }

            override fun getValue(buffer: ByteBuffer) = buffer.getInt(offset)
        }

        class FloatField(layout: Std140Layout<*>) : Field<Float>(
            layout = layout,
            align = 4,
            size = 4,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Float) {
                buffer.putFloat(offset, value)
            }

            override fun getValue(buffer: ByteBuffer) = buffer.getFloat(offset)
        }

        class Vec2Field(layout: Std140Layout<*>) : Field<Vector2fc>(
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

        class Vec3Field(layout: Std140Layout<*>) : Field<Vector3fc>(
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

        class Vec4Field(layout: Std140Layout<*>) : Field<Vector4fc>(
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

        class IVec2Field(layout: Std140Layout<*>) : Field<Vector2ic>(
            layout = layout,
            align = 8,
            size = 8,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Vector2ic) {
                value.get(offset, buffer)
            }

            private val vector = Vector2i()

            override fun getValue(buffer: ByteBuffer) = vector.apply {
                set(offset, buffer)
            }
        }

        class IVec3Field(layout: Std140Layout<*>) : Field<Vector3ic>(
            layout = layout,
            align = 16,
            size = 12,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Vector3ic) {
                value.get(offset, buffer)
            }

            private val vector = Vector3i()

            override fun getValue(buffer: ByteBuffer) = vector.apply {
                set(offset, buffer)
            }
        }

        class IVec4Field(layout: Std140Layout<*>) : Field<Vector4ic>(
            layout = layout,
            align = 16,
            size = 16,
        ) {
            override fun setValue(buffer: ByteBuffer, value: Vector4ic) {
                value.get(offset, buffer)
            }

            private val vector = Vector4i()

            override fun getValue(buffer: ByteBuffer) = vector.apply {
                set(offset, buffer)
            }
        }

        class Mat2Field(layout: Std140Layout<*>) : Field<Matrix2fc>(
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

        class Mat3Field(layout: Std140Layout<*>) : Field<Matrix3fc>(
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

        class Mat4Field(layout: Std140Layout<*>) : Field<Matrix4fc>(
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

        class IntArray(layout: Std140Layout<*>, private val length: Int) : Field<IntList>(
            layout = layout,
            align = 4,
            size = 4 * length,
        ) {
            override fun setValue(buffer: ByteBuffer, value: IntList) {
                require(value.size == length) { "List size mismatch: expected $length, but got ${value.size}" }
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
                    checkIndex(index, length)
                    return requireNotNull(buffer).getInt(offset + index * 4)
                }

                override fun set(index: Int, k: Int): Int = getInt(index).also {
                    requireNotNull(buffer).putInt(offset + index * 4, k)
                }
            }

            override fun getValue(buffer: ByteBuffer) = list.also { it.buffer = buffer }
        }

        class FloatArray(layout: Std140Layout<*>, private val length: Int) : Field<FloatList>(
            layout = layout,
            align = 4,
            size = 4 * length,
        ) {
            override fun setValue(buffer: ByteBuffer, value: FloatList) {
                require(value.size == length) { "List size mismatch: expected $length, but got ${value.size}" }
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
                    checkIndex(index, length)
                    return requireNotNull(buffer).getFloat(offset + index * 4)
                }

                override fun set(index: Int, k: Float): Float = getFloat(index).also {
                    requireNotNull(buffer).putFloat(offset + index * 4, k)
                }
            }

            override fun getValue(buffer: ByteBuffer) = list.also { it.buffer = buffer }
        }

        class Vec2Array(
            layout: Std140Layout<*>,
            private val length: Int
        ) : Field<MutableList<Vector2fc>>(
            layout = layout,
            align = 8,
            size = length * 8,
        ) {
            private val vector = Vector2f()
            private val list = object : AbstractMutableList<Vector2fc>() {
                var buffer: ByteBuffer? = null

                override fun add(index: Int, element: Vector2fc) = throw UnsupportedOperationException()
                override fun removeAt(index: Int) = throw UnsupportedOperationException()

                override val size: Int
                    get() = length

                override fun get(index: Int): Vector2fc {
                    checkIndex(index, length)
                    return vector.apply { set(offset + index * 8, buffer) }
                }

                override fun set(index: Int, element: Vector2fc) = get(index).also {
                    element.get(offset + index * 8, requireNotNull(buffer))
                }
            }

            override fun setValue(buffer: ByteBuffer, value: MutableList<Vector2fc>) {
                require(value.size == length) { "List size mismatch" }
                value.forEachIndexed { i, vec -> vec.get(offset + i * 8, buffer) }
            }

            override fun getValue(buffer: ByteBuffer) = list.also { it.buffer = buffer }
        }

        class Vec3Array(
            layout: Std140Layout<*>,
            private val length: Int
        ) : Field<MutableList<Vector3fc>>(
            layout = layout,
            align = 16,
            size = length * 16,
        ) {
            private val vector = Vector3f()
            private val list = object : AbstractMutableList<Vector3fc>() {
                var buffer: ByteBuffer? = null

                override fun add(index: Int, element: Vector3fc) = throw UnsupportedOperationException()
                override fun removeAt(index: Int) = throw UnsupportedOperationException()

                override val size: Int
                    get() = length

                override fun get(index: Int): Vector3fc {
                    checkIndex(index, length)
                    return vector.apply { set(offset + index * 16, buffer) }
                }

                override fun set(index: Int, element: Vector3fc) = get(index).also {
                    element.get(offset + index * 16, requireNotNull(buffer))
                }
            }

            override fun setValue(buffer: ByteBuffer, value: MutableList<Vector3fc>) {
                require(value.size == length) { "List size mismatch" }
                value.forEachIndexed { i, vec -> vec.get(offset + i * 16, buffer) }
            }

            override fun getValue(buffer: ByteBuffer) = list.also { it.buffer = buffer }
        }

        class Vec4Array(
            layout: Std140Layout<*>,
            private val length: Int
        ) : Field<MutableList<Vector4fc>>(
            layout = layout,
            align = 16,
            size = length * 16,
        ) {
            private val vector = Vector4f()
            private val list = object : AbstractMutableList<Vector4fc>() {
                var buffer: ByteBuffer? = null

                override fun add(index: Int, element: Vector4fc) = throw UnsupportedOperationException()
                override fun removeAt(index: Int) = throw UnsupportedOperationException()

                override val size: Int
                    get() = length

                override fun get(index: Int): Vector4fc {
                    checkIndex(index, length)
                    return vector.apply { set(offset + index * 16, buffer) }
                }

                override fun set(index: Int, element: Vector4fc) = get(index).also {
                    element.get(offset + index * 16, requireNotNull(buffer))
                }
            }

            override fun setValue(buffer: ByteBuffer, value: MutableList<Vector4fc>) {
                require(value.size == length) { "List size mismatch" }
                value.forEachIndexed { i, vec -> vec.get(offset + i * 16, buffer) }
            }

            override fun getValue(buffer: ByteBuffer) = list.also { it.buffer = buffer }
        }
    }

    fun int() = Field.IntField(this)
    fun float() = Field.FloatField(this)

    fun vec2() = Field.Vec2Field(this)
    fun vec3() = Field.Vec3Field(this)
    fun vec4() = Field.Vec4Field(this)

    fun ivec2() = Field.IVec2Field(this)
    fun ivec3() = Field.IVec3Field(this)
    fun ivec4() = Field.IVec4Field(this)

    fun mat2() = Field.Mat2Field(this)
    fun mat3() = Field.Mat3Field(this)
    fun mat4() = Field.Mat4Field(this)

    fun intArray(size: Int) = Field.IntArray(this, size)
    fun floatArray(size: Int) = Field.FloatArray(this, size)

    fun vec2Array(size: Int) = Field.Vec2Array(this, size)
    fun vec3Array(size: Int) = Field.Vec3Array(this, size)
    fun vec4Array(size: Int) = Field.Vec4Array(this, size)

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
