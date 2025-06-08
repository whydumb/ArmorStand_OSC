package top.fifthlight.armorstand.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.armorstand.std140.Std140Layout
import top.fifthlight.armorstand.util.FramedObjectPool
import top.fifthlight.armorstand.util.Pool

class MorphDataUniformBuffer: UniformBuffer<MorphDataUniformBuffer, MorphDataUniformBuffer.MorphDataLayout>(MorphDataLayout) {
    override val pool: Pool<MorphDataUniformBuffer>
        get() = POOL

    override val layout: MorphDataLayout
        get() = MorphDataLayout

    object MorphDataLayout: Std140Layout<MorphDataLayout>() {
        var totalVertices by int()
        var posTargets by int()
        var colorTargets by int()
        var texCoordTargets by int()
        var totalTargets by int()
    }

    companion object {
        private val POOL = FramedObjectPool<MorphDataUniformBuffer>(
            identifier = Identifier.of("armorstand", "morph_data_uniform_buffer"),
            create = ::MorphDataUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}