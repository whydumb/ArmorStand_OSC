package top.fifthlight.blazerod.model.uniform

import top.fifthlight.blazerod.std140.Std140Layout

object UnlitDataUniformBuffer: UniformBuffer<UnlitDataUniformBuffer, UnlitDataUniformBuffer.UnlitDataLayout>(
    name = "UnlitDataUniformBuffer",
) {
    override val layout: UnlitDataLayout
        get() = UnlitDataLayout

    object UnlitDataLayout: Std140Layout<UnlitDataLayout>() {
        var baseColor by rgba()
    }
}