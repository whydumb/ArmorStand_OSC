package top.fifthlight.blazerod.model.resource

import org.joml.Matrix4fc

class RenderSkin(
    val name: String?,
    val inverseBindMatrices: List<Matrix4fc>?,
    val jointSize: Int,
)
