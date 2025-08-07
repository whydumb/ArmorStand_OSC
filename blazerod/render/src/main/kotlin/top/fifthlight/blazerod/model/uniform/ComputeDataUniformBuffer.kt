package top.fifthlight.blazerod.model.uniform

import top.fifthlight.blazerod.layout.GpuDataLayout
import top.fifthlight.blazerod.layout.LayoutStrategy

object ComputeDataUniformBuffer : UniformBuffer<ComputeDataUniformBuffer, ComputeDataUniformBuffer.ComputeDataLayout>(
    name = "ComputeDataUniformBuffer",
) {
    override val layout: ComputeDataLayout
        get() = ComputeDataLayout

    object ComputeDataLayout : GpuDataLayout<ComputeDataLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std140LayoutStrategy
        var totalVertices by int()
        var uv1 by int()
        var uv2 by int()
    }
}