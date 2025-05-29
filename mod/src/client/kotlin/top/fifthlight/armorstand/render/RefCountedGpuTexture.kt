package top.fifthlight.armorstand.render

import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.util.AbstractRefCount

class RefCountedGpuTexture(
    val inner: GpuTexture,
    val view: GpuTextureView,
): AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "gpu_texture")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    override fun onClosed() {
        inner.close()
    }
}