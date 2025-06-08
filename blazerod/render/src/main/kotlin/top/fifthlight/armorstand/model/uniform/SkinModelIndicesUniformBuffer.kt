package top.fifthlight.armorstand.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.armorstand.BlazeRod
import top.fifthlight.armorstand.std140.Std140Layout
import top.fifthlight.armorstand.util.FramedObjectPool
import top.fifthlight.armorstand.util.Pool

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
            identifier = Identifier.of("armorstand", "skin_model_indices_uniform_buffer"),
            create = ::SkinModelIndicesUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}