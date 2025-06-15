package top.fifthlight.blazerod.model

import org.joml.Matrix4f

sealed class Camera {
    abstract val name: String?
    abstract fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float)

    data class Perspective(
        override val name: String?,
        val aspectRatio: Float? = null,
        val yfov: Float,
        val zfar: Float? = null,
        val znear: Float,
    ) : Camera() {
        override fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float) {
            matrix.perspective(yfov, aspectRatio, znear, zfar ?: farPlaneDistance)
        }
    }

    data class Orthographic(
        override val name: String?,
        val xmag: Float,
        val ymag: Float,
        val zfar: Float,
        val znear: Float,
    ) : Camera() {
        override fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float) {
            val xmag = ymag * aspectRatio
            matrix.ortho(-xmag, xmag, -ymag, ymag, znear, zfar)
        }
    }
}