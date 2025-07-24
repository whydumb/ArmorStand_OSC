package top.fifthlight.blazerod.model

import java.util.*

data class NodeId(
    val modelId: UUID,
    val index: Int,
)

data class Node(
    val name: String? = null,
    val id: NodeId,
    val transform: NodeTransform? = null,
    val children: List<Node> = listOf(),
    val components: List<NodeComponent> = listOf(),
) {
    val meshComponent: NodeComponent.MeshComponent?
    val skinComponent: NodeComponent.SkinComponent?

    init {
        var requireMesh = false
        var typeComponents = mutableMapOf<NodeComponent.Type, MutableList<NodeComponent>>()
        for (component in components) {
            if (component.type.singleInstanceOnly && typeComponents.containsKey(component.type)) {
                throw IllegalArgumentException("Node ${id.index} has multiple components of type ${component.type}")
            }
            requireMesh = requireMesh || component.type.requireMesh
            typeComponents.getOrPut(component.type) { mutableListOf() }.add(component)
        }
        if (requireMesh && !typeComponents.containsKey(NodeComponent.Type.MESH)) {
            throw IllegalArgumentException("This node must have a mesh component, as it has component requires a mesh")
        }
        meshComponent = typeComponents[NodeComponent.Type.MESH]?.first() as? NodeComponent.MeshComponent
        skinComponent = typeComponents[NodeComponent.Type.SKIN]?.first() as? NodeComponent.SkinComponent
    }
}

fun Node.forEach(action: (Node) -> Unit) {
    action(this)
    for (child in children) {
        child.forEach(action)
    }
}
