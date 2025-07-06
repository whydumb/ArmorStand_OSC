package top.fifthlight.blazerod.model

data class Influence(
    val sources: List<NodeId>,
    val influence: Float,
    val relative: Boolean,
    val influenceRotation: Boolean,
    val influenceTranslation: Boolean,
) {
    init {
        require(sources.isNotEmpty()) { "Influence sources cannot be empty" }
    }
}