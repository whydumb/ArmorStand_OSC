package top.fifthlight.blazerod.model.uniform

import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.std140.Std140Layout

object SkinModelIndicesUniformBuffer : UniformBuffer<SkinModelIndicesUniformBuffer, SkinModelIndicesUniformBuffer.SkinModelIndicesLayout>(
    name = "SkinModelIndicesUniformBuffer",
) {
    override val layout: SkinModelIndicesLayout
        get() = SkinModelIndicesLayout

    object SkinModelIndicesLayout : Std140Layout<SkinModelIndicesLayout>() {
        var skinJoints by int()
        var skinModelOffsets by intArray(BlazeRod.INSTANCE_SIZE)
    }
}