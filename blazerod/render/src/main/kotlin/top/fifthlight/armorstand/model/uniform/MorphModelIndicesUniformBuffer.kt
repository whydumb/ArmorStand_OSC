package top.fifthlight.armorstand.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.armorstand.BlazeRod
import top.fifthlight.armorstand.std140.Std140Layout
import top.fifthlight.armorstand.util.FramedObjectPool
import top.fifthlight.armorstand.util.Pool

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
            identifier = Identifier.of("armorstand", "morph_data_uniform_buffer"),
            create = ::MorphModelIndicesUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}