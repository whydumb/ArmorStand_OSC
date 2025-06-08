package top.fifthlight.blazerod.model

import org.joml.Matrix4f

data class Skin(
    val name: String? = null,
    val joints: List<NodeId>,
    val inverseBindMatrices: List<Matrix4f>? = null,
    val skeleton: NodeId? = null,
    val jointHumanoidTags: List<HumanoidTag?> = listOf(),
)
