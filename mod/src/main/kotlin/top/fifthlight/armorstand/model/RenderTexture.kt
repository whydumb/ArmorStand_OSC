package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.texture.NativeImage
import top.fifthlight.armorstand.render.RefCountedGpuTexture
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.renderer.model.Texture
import java.nio.ByteBuffer

class RenderTexture(
    val texture: RefCountedGpuTexture,
    val sampler: Texture.Sampler,
    val coordinate: Int,
) : AbstractRefCount() {
    init {
        texture.increaseReferenceCount()
    }

    override fun onClosed() {
        texture.decreaseReferenceCount()
    }

    companion object {
        val WHITE_RGBA_TEXTURE by lazy {
            val buffer = ByteBuffer.allocateDirect(4).asIntBuffer().apply { repeat(4) { put(0xFF) } }
            val texture = RenderSystem.getDevice().run {
                createTexture("White RGBA texture", TextureFormat.RGBA8, 1, 1, 1).also {
                    createCommandEncoder().writeToTexture(it, buffer, NativeImage.Format.RGBA, 0, 0, 0, 1, 1)
                }
            }.let { RefCountedGpuTexture(it) }
            RenderTexture(
                texture = texture,
                sampler = Texture.Sampler(),
                coordinate = 0,
            ).apply {
                // Increase ref count to keep it not being closed
                increaseReferenceCount()
            }
        }
    }
}