package top.fifthlight.renderer.model

data class Scene(
    val name: String?,
    val nodes: List<Node>,
    val skins: List<Skin>,
)