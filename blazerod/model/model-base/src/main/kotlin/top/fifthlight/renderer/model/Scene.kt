package top.fifthlight.renderer.model

data class Scene(
    val nodes: List<Node>,
    val initialTransform: NodeTransform? = null,
)