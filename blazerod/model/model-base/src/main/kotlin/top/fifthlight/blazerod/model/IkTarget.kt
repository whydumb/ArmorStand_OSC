package top.fifthlight.blazerod.model

import org.joml.Vector3fc

data class IkTarget(
    val loopCount: Int,
    val limitRadian: Float,
    val ikLinks: List<IkLink>,
) {
    data class IkLink(
        val index: NodeId,
        val limit: Limits?,
    ) {
        data class Limits(
            val min: Vector3fc,
            val max: Vector3fc,
        )
    }
}
