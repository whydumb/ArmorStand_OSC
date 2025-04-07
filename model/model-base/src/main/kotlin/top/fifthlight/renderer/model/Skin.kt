package top.fifthlight.renderer.model

import org.joml.Matrix4f

data class Skin(
    val name: String? = null,
    val joints: List<NodeId>,
    val inverseBindMatrices: List<Matrix4f>? = null,
    val skeleton: NodeId? = null,
    // According to GLTF specification 3.7.3.2, skinned node shouldn't apply global transform.
    val ignoreGlobalTransform: Boolean,
)
