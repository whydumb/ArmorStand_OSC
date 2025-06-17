package top.fifthlight.blazerod.model.animation

data class Animation(
    val name: String? = null,
    val channels: List<AnimationChannel<*, *>>,
)
