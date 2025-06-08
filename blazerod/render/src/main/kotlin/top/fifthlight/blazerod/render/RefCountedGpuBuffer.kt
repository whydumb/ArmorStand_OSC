package top.fifthlight.blazerod.render

import com.mojang.blaze3d.buffers.GpuBuffer
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.util.AbstractRefCount

class RefCountedGpuBuffer(val inner: GpuBuffer): AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "gpu_buffer")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    override fun onClosed() {
        inner.close()
    }
}