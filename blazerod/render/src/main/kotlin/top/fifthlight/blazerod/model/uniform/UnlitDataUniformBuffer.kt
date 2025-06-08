package top.fifthlight.blazerod.model.uniform

import net.minecraft.util.Identifier
import top.fifthlight.blazerod.std140.Std140Layout
import top.fifthlight.blazerod.util.FramedObjectPool
import top.fifthlight.blazerod.util.Pool

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
            identifier = Identifier.of("blazerod", "unlit_data_uniform_buffer"),
            create = ::UnlitDataUniformBuffer,
            onAcquired = { released = false }
        )

        fun acquire() = POOL.acquire()
    }
}