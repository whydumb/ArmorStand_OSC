package top.fifthlight.armorstand.render

import com.mojang.blaze3d.buffers.GpuBuffer
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.util.AbstractRefCount

class RefCountedGpuBuffer(val inner: GpuBuffer): AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "gpu_buffer")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    override fun onClosed() {
        inner.close()
    }
}