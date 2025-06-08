package top.fifthlight.armorstand.model

import top.fifthlight.blazerod.model.Expression.Tag

data class RenderExpression(
    val name: String? = null,
    val tag: Tag,
    val isBinary: Boolean = false,
    val bindings: List<Binding>,
) {
    sealed class Binding {
        data class MorphTarget(
            val morphedPrimitiveIndex: Int,
            val groupIndex: Int,
            val weight: Float,
        ): Binding()
    }
}
