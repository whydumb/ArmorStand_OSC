package top.fifthlight.renderer.model

data class Scene(
    val metadata: Metadata? = null,
    val nodes: List<Node>,
    val skins: List<Skin>,
)