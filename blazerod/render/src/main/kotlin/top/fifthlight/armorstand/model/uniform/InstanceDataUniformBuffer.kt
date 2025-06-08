package top.fifthlight.armorstand.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.armorstand.BlazeRod
import top.fifthlight.armorstand.std140.Std140Layout
import top.fifthlight.armorstand.util.FramedObjectPool
import top.fifthlight.armorstand.util.Pool

class InstanceDataUniformBuffer : UniformBuffer<InstanceDataUniformBuffer, InstanceDataUniformBuffer.InstanceDataLayout>(InstanceDataLayout) {
    override val pool: Pool<InstanceDataUniformBuffer>
        get() = POOL

    override val layout: InstanceDataLayout
        get() = InstanceDataLayout

    object InstanceDataLayout : Std140Layout<InstanceDataLayout>() {
        var primitiveSize by int()
        var primitiveIndex by int()
        var viewModelMatrices by mat4Array(BlazeRod.INSTANCE_SIZE)
        var lightMapUvs by ivec2Array(BlazeRod.INSTANCE_SIZE)
        val localMatricesIndices by intArray(BlazeRod.INSTANCE_SIZE)
    }

    companion object {
        private val POOL = FramedObjectPool<InstanceDataUniformBuffer>(
            identifier = Identifier.of("armorstand", "instance_data_uniform_buffer"),
            create = ::InstanceDataUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}