package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.texture.NativeImage
import net.minecraft.util.Identifier
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

    override val typeId: Identifier
        get() = TYPE_ID

    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "texture")
        val WHITE_RGBA_TEXTURE by lazy {
            val buffer = ByteBuffer.allocateDirect(16).asIntBuffer().apply {
                repeat(4) { put(0xFFFFFFFFu.toInt()) }
                flip()
            }
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
                // Increase ref count to keep it from being closed
                increaseReferenceCount()
            }
        }
    }
}