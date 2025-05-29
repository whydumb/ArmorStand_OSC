package top.fifthlight.armorstand.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.armorstand.std140.Std140Layout
import top.fifthlight.armorstand.util.FramedObjectPool
import top.fifthlight.armorstand.util.Pool

class UnlitDataUniformBuffer: UniformBuffer<UnlitDataUniformBuffer, UnlitDataUniformBuffer.UnlitDataLayout>(UnlitDataLayout) {
    override val pool: Pool<UnlitDataUniformBuffer>
        get() = POOL

    override val layout: UnlitDataLayout
        get() = UnlitDataLayout

    object UnlitDataLayout: Std140Layout<UnlitDataLayout>() {
        var baseColor by rgba()
    }

    companion object {
        private val POOL = FramedObjectPool<UnlitDataUniformBuffer>(
            identifier = Identifier.of("armorstand", "unlit_data_uniform_buffer"),
            create = ::UnlitDataUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}