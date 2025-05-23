package top.fifthlight.armorstand.test.std140

import org.joml.*
import top.fifthlight.armorstand.std140.Std140Layout
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Std140Test {
    private object SimpleLayout : Std140Layout<SimpleLayout>() {
        var intField by int()       // offset: 0, size: 4, align: 4
        var floatField by float()   // offset: 4, size: 4, align: 4
        // total size: 8
    }

    private fun ByteBuffer.assertInts(offset: Int, vararg expected: Int) = expected.forEachIndexed { index, float ->
        assertEquals(expected[index], getInt(offset + index * 4))
    }

    private fun ByteBuffer.assertFloats(offset: Int, vararg expected: Float) = expected.forEachIndexed { index, float ->
        assertEquals(expected[index], getFloat(offset + index * 4))
    }

    @Test
    fun testSimpleLayout() {
        assertEquals(8, SimpleLayout.totalSize)
        val buffer = ByteBuffer.allocate(SimpleLayout.totalSize)
        SimpleLayout.withBuffer(buffer) {
            intField = 4
            floatField = 2.5f
        }
        assertEquals(4, buffer.getInt(0))
        assertEquals(2.5f, buffer.getFloat(4))
    }

    private object VectorLayout : Std140Layout<VectorLayout>() {
        var vec2Field by vec2()     // offset: 0, size: 8, align: 8
        var vec3Field by vec3()     // offset: 16, size: 12, align: 16
        var vec4Field by vec4()     // offset: 32, size: 16, align: 16
        // total size: 48
    }

    @Test
    fun testVectorLayout() {
        assertEquals(48, VectorLayout.totalSize)
        val buffer = ByteBuffer.allocate(VectorLayout.totalSize)
        VectorLayout.withBuffer(buffer) {
            vec2Field = Vector2f(1.0f, 2.0f)
            vec3Field = Vector3f(3.0f, 4.0f, 5.0f)
            vec4Field = Vector4f(6.0f, 7.0f, 8.0f, 9.0f)
        }

        buffer.assertFloats(0, 1.0f, 2.0f)
        buffer.assertFloats(16, 3.0f, 4.0f, 5.0f)
        buffer.assertFloats(32, 6.0f, 7.0f, 8.0f, 9.0f)
    }

    private object MatrixLayout : Std140Layout<MatrixLayout>() {
        var mat2Field by mat2()     // offset: 0, size: 16, align: 16
        var mat3Field by mat3()     // offset: 16, size: 48, align: 16
        var mat4Field by mat4()     // offset: 64, size: 64, align: 16
        // total size: 128
    }

    @Test
    fun testMatrixLayout() {
        assertEquals(128, MatrixLayout.totalSize)
        val buffer = ByteBuffer.allocate(MatrixLayout.totalSize)
        MatrixLayout.withBuffer(buffer) {
            mat2Field = Matrix2f(
                1.0f, 3.0f,
                2.0f, 4.0f
            )
            mat3Field = Matrix3f(
                5.0f, 8.0f, 11.0f,
                6.0f, 9.0f, 12.0f,
                7.0f, 10.0f, 13.0f
            )
            mat4Field = Matrix4f(
                14.0f, 18.0f, 22.0f, 26.0f,
                15.0f, 19.0f, 23.0f, 27.0f,
                16.0f, 20.0f, 24.0f, 28.0f,
                17.0f, 21.0f, 25.0f, 29.0f
            )
        }

        buffer.assertFloats(0, 1.0f, 3.0f)
        buffer.assertFloats(8, 2.0f, 4.0f)

        buffer.assertFloats(16, 5.0f, 8.0f, 11.0f)
        buffer.assertFloats(32, 6.0f, 9.0f, 12.0f)
        buffer.assertFloats(48, 7.0f, 10.0f, 13.0f)

        buffer.assertFloats(64, 14.0f, 18.0f, 22.0f, 26.0f)
        buffer.assertFloats(80, 15.0f, 19.0f, 23.0f, 27.0f)
        buffer.assertFloats(96, 16.0f, 20.0f, 24.0f, 28.0f)
        buffer.assertFloats(112, 17.0f, 21.0f, 25.0f, 29.0f)
    }

    private object MixedLayout : Std140Layout<MixedLayout>() {
        var intField by int()       // offset: 0, size: 4, align: 4
        var vec3Field by vec3()     // offset: 16, size: 12, align: 16
        var floatField by float()   // offset: 28, size: 4, align: 4
        var mat2Field by mat2()     // offset: 32, size: 16, align: 16
        // total size: 48
    }

    @Test
    fun testMixedLayout() {
        assertEquals(48, MixedLayout.totalSize)
        val buffer = ByteBuffer.allocate(MixedLayout.totalSize)
        MixedLayout.withBuffer(buffer) {
            intField = 1
            vec3Field = Vector3f(2.0f, 3.0f, 4.0f)
            floatField = 5.0f
            mat2Field = Matrix2f(
                6.0f, 8.0f,
                7.0f, 9.0f
            )
        }

        assertEquals(1, buffer.getInt(0))
        buffer.assertFloats(16, 2.0f, 3.0f, 4.0f)
        assertEquals(5.0f, buffer.getFloat(28))
        buffer.assertFloats(32, 6.0f, 8.0f)
        buffer.assertFloats(40, 7.0f, 9.0f)
    }

    private object ArrayLayout : Std140Layout<ArrayLayout>() {
        var intArray by intArray(4)     // offset: 0, size: 16, align: 4
        var floatArray by floatArray(4)     // offset: 16, size: 16, align: 4
        // total size: 32
    }

    @Test
    fun testArrayLayout() {
        assertEquals(32, ArrayLayout.totalSize)
        val buffer = ByteBuffer.allocate(ArrayLayout.totalSize)
        ArrayLayout.withBuffer(buffer) {
            intArray.apply {
                this[0] = 1
                this[1] = 2
                this[2] = 3
                this[3] = 4
                assertFailsWith(IndexOutOfBoundsException::class) {
                    this[4] = 5
                }
            }
            floatArray.apply {
                this[0] = 1.0f
                this[1] = 2.0f
                this[2] = 3.0f
                this[3] = 4.0f
                assertFailsWith(IndexOutOfBoundsException::class) {
                    this[4] = 5.0f
                }
            }
        }
        buffer.assertInts(0, 1, 2, 3, 4)
        buffer.assertFloats(16, 1.0f, 2.0f, 3.0f, 4.0f)
    }
}
