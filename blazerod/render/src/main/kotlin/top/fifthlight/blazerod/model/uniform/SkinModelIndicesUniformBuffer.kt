package top.fifthlight.blazerod.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.std140.Std140Layout
import top.fifthlight.blazerod.util.FramedObjectPool
import top.fifthlight.blazerod.util.Pool

class SkinModelIndicesUniformBuffer : UniformBuffer<SkinModelIndicesUniformBuffer, SkinModelIndicesUniformBuffer.SkinModelIndicesLayout>(SkinModelIndicesLayout) {
    override val pool: Pool<SkinModelIndicesUniformBuffer>
        get() = POOL

    override val layout: SkinModelIndicesLayout
        get() = SkinModelIndicesLayout

    object SkinModelIndicesLayout : Std140Layout<SkinModelIndicesLayout>() {
        var skinJoints by int()
        var skinModelOffsets by intArray(BlazeRod.INSTANCE_SIZE)
    }

    companion object {
        private val POOL = FramedObjectPool<SkinModelIndicesUniformBuffer>(
            identifier = Identifier.of("blazerod", "skin_model_indices_uniform_buffer"),
            create = ::SkinModelIndicesUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}