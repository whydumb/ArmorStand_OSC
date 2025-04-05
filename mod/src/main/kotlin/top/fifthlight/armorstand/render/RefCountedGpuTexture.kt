package top.fifthlight.armorstand.render

import com.mojang.blaze3d.textures.GpuTexture
import top.fifthlight.armorstand.util.AbstractRefCount

class RefCountedGpuTexture(val inner: GpuTexture): AbstractRefCount() {
    override fun onClosed() {
        inner.close()
    }
}