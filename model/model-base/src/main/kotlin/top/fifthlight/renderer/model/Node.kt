package top.fifthlight.renderer.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

sealed class NodeTransform {
    abstract val matrix: Matrix4f

    data class Matrix(
        override val matrix: Matrix4f
    ) : NodeTransform()

    data class Decomposed(
        val translation: Vector3f,
        val rotation: Quaternionf,
        val scale: Vector3f,
    ) : NodeTransform() {
        override val matrix: Matrix4f = Matrix4f().translationRotateScale(translation, rotation, scale)
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