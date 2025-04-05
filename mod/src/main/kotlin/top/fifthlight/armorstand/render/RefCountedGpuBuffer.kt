package top.fifthlight.armorstand.render

import com.mojang.blaze3d.buffers.GpuBuffer
import top.fifthlight.armorstand.util.AbstractRefCount

class RefCountedGpuBuffer(val inner: GpuBuffer): AbstractRefCount() {
    override fun onClosed() {
        inner.close()
    }
}