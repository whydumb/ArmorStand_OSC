package top.fifthlight.blazerod.model.uniform

import top.fifthlight.blazerod.std140.Std140Layout

object MorphDataUniformBuffer: UniformBuffer<MorphDataUniformBuffer, MorphDataUniformBuffer.MorphDataLayout>(
    name = "MorphDataUniformBuffer",
) {
    override val layout: MorphDataLayout
        get() = MorphDataLayout

    object MorphDataLayout: Std140Layout<MorphDataLayout>() {
        var totalVertices by int()
        var posTargets by int()
        var colorTargets by int()
        var texCoordTargets by int()
        var totalTargets by int()
    }
}