package top.fifthlight.blazerod.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.std140.Std140Layout
import top.fifthlight.blazerod.util.FramedObjectPool
import top.fifthlight.blazerod.util.Pool

class MorphModelIndicesUniformBuffer: UniformBuffer<MorphModelIndicesUniformBuffer, MorphModelIndicesUniformBuffer.MorphModelIndicesLayout>(MorphModelIndicesLayout) {
    override val pool: Pool<MorphModelIndicesUniformBuffer>
        get() = POOL

    override val layout: MorphModelIndicesLayout
        get() = MorphModelIndicesLayout

    object MorphModelIndicesLayout: Std140Layout<MorphModelIndicesLayout>() {
        var morphWeightIndices by intArray(BlazeRod.INSTANCE_SIZE)
        var morphIndexIndices by intArray(BlazeRod.INSTANCE_SIZE)
    }

    companion object {
        private val POOL = FramedObjectPool<MorphModelIndicesUniformBuffer>(
            identifier = Identifier.of("blazerod", "morph_data_uniform_buffer"),
            create = ::MorphModelIndicesUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}