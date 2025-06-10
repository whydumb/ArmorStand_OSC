package top.fifthlight.blazerod.model

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

sealed class NodeTransform {
    abstract val matrix: Matrix4fc
    abstract fun clone(): NodeTransform

    data class Matrix(
        override val matrix: Matrix4f = Matrix4f()
    ) : NodeTransform() {
        override fun clone() = Matrix(
            matrix = Matrix4f(matrix),
        )
    }

    data class Decomposed(
        val translation: Vector3f = Vector3f(0f),
        val rotation: Quaternionf = Quaternionf(),
        val scale: Vector3f = Vector3f(1f),
    ) : NodeTransform() {
        private val cacheMatrix = Matrix4f()
        override val matrix: Matrix4f
            get() = cacheMatrix.translationRotateScale(translation, rotation, scale)

        fun set(other: Decomposed) {
            translation.set(other.translation)
            rotation.set(other.rotation)
            scale.set(other.scale)
        }

        override fun clone() = Decomposed(
            translation = Vector3f(translation),
            rotation = Quaternionf(rotation),
            scale = Vector3f(scale),
        )
    }
}

data class NodeId(
    val modelId: UUID,
    val index: Int,
)

data class Node(
    val name: String? = null,
    val id: NodeId,
    val children: List<Node> = listOf(),
    val transform: NodeTransform? = null,
    val mesh: Mesh? = null,
    val skin: Skin? = null,
)