package top.fifthlight.blazerod.model.uniform

import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.std140.Std140Layout

object InstanceDataUniformBuffer : UniformBuffer<InstanceDataUniformBuffer, InstanceDataUniformBuffer.InstanceDataLayout>(
    name = "InstanceDataUniformBuffer",
) {
    override val layout: InstanceDataLayout
        get() = InstanceDataLayout

    object InstanceDataLayout : Std140Layout<InstanceDataLayout>() {
        var primitiveSize by int()
        var primitiveIndex by int()
        var modelViewMatrices by mat4Array(BlazeRod.INSTANCE_SIZE)
        var lightMapUvs by ivec2Array(BlazeRod.INSTANCE_SIZE)
        val localMatricesIndices by intArray(BlazeRod.INSTANCE_SIZE)
    }
}