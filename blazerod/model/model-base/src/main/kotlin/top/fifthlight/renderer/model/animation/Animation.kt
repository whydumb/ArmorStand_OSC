package top.fifthlight.renderer.model.animation

data class Animation(
    val name: String? = null,
    val channels: List<AnimationChannel<*>>,
)
