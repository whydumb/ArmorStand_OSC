package top.fifthlight.armorstand.model

import top.fifthlight.renderer.model.Primitive

// Named so to avoid conflict with VertexFormat.
// VertexFormat class from Mojang don't handle direct loaded buffer very well, so we roll our own version here.
data class VertexType(
    val elements: List<Primitive.Attributes.Key>,
) {
    companion object {
        val POSITION = VertexType(listOf(Primitive.Attributes.Key.POSITION))

        val POSITION_TEXTURE_COLOR = VertexType(listOf(
            Primitive.Attributes.Key.POSITION,
            Primitive.Attributes.Key.TEXCOORD,
            Primitive.Attributes.Key.COLORS,
        ))

        val POSITION_TEXTURE_COLOR_JOINT_WEIGHT = VertexType(listOf(
            Primitive.Attributes.Key.POSITION,
            Primitive.Attributes.Key.TEXCOORD,
            Primitive.Attributes.Key.COLORS,
            Primitive.Attributes.Key.JOINTS,
            Primitive.Attributes.Key.WEIGHTS,
        ))
    }
}
