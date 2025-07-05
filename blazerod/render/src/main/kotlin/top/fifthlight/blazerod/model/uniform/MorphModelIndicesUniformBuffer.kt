package top.fifthlight.blazerod.model.uniform

import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.std140.Std140Layout

object MorphModelIndicesUniformBuffer: UniformBuffer<MorphModelIndicesUniformBuffer, MorphModelIndicesUniformBuffer.MorphModelIndicesLayout>(
    name = "MorphModelIndicesUniformBuffer",
) {
    override val layout: MorphModelIndicesLayout
        get() = MorphModelIndicesLayout

    object MorphModelIndicesLayout: Std140Layout<MorphModelIndicesLayout>() {
        var morphWeightIndices by intArray(BlazeRod.INSTANCE_SIZE)
        var morphIndexIndices by intArray(BlazeRod.INSTANCE_SIZE)
    }
}