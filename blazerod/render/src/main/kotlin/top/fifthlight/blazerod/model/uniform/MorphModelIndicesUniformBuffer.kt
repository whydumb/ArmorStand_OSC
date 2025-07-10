package top.fifthlight.blazerod.model.uniform

import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.layout.GpuDataLayout
import top.fifthlight.blazerod.layout.LayoutStrategy

object MorphModelIndicesUniformBuffer: UniformBuffer<MorphModelIndicesUniformBuffer, MorphModelIndicesUniformBuffer.MorphModelIndicesLayout>(
    name = "MorphModelIndicesUniformBuffer",
) {
    override val layout: MorphModelIndicesLayout
        get() = MorphModelIndicesLayout

    object MorphModelIndicesLayout: GpuDataLayout<MorphModelIndicesLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std140LayoutStrategy
        var morphWeightIndices by intArray(BlazeRod.INSTANCE_SIZE)
        var morphIndexIndices by intArray(BlazeRod.INSTANCE_SIZE)
    }
}