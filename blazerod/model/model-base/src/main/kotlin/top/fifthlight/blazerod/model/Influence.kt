package top.fifthlight.blazerod.model

data class Influence(
    val source: NodeId,
    val influence: Float,
    val influenceRotation: Boolean,
    val influenceTranslation: Boolean,
)