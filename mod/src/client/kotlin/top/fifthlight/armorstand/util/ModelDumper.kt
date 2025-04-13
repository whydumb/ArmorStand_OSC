package top.fifthlight.armorstand.util

import org.slf4j.Logger
import top.fifthlight.armorstand.model.RenderNode

object ModelDumper {
    fun RenderNode.getNodeDescription(): String = when (this) {
        is RenderNode.Group -> "Group (${children.size} children)"
        is RenderNode.Transform -> "Transform[targetIndex=$transformIndex]"
        is RenderNode.Mesh -> "Mesh[primitiveSize=${mesh.primitives.size}, skinIndex=$skinIndex]"
        is RenderNode.Joint -> "Joint[skinIndex=$skinIndex, jointIndex=$jointIndex]"
    }

    fun RenderNode.dumpTreeInternal(
        logger: Logger,
        prefix: String,
        isLastChild: Boolean,
        isFirst: Boolean = false,
    ) {
        val currentSymbol = if (isFirst) "" else if (isLastChild) "└── " else "├── "
        logger.info("$prefix$currentSymbol${getNodeDescription()}")

        val children = this.toList()
        val newPrefix = prefix + if (isFirst) "" else if (isLastChild) "    " else "│   "

        children.forEachIndexed { index, child ->
            val isLast = index == children.lastIndex
            child.dumpTreeInternal(logger, newPrefix, isLast)
        }
    }

    fun dumpNode(node: RenderNode, logger: Logger) {
        node.dumpTreeInternal(logger, "", true, true)
    }
}