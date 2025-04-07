package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4fc
import org.joml.Vector3fc
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.VertexBuffer
import top.fifthlight.armorstand.render.setIndexBuffer
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.bindSampler
import top.fifthlight.armorstand.util.setVertexBuffer
import java.util.*

class RenderPrimitive(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer?,
    val material: RenderMaterial,
) : AbstractRefCount() {
    init {
        vertexBuffer.increaseReferenceCount()
        indexBuffer?.increaseReferenceCount()
        material.increaseReferenceCount()
    }

    fun render(matrix: Matrix4fc, light: Int, skin: RenderSkinData?) {
        val mainColorTexture: GpuTexture = MinecraftClient.getInstance().framebuffer.colorAttachment!!
        val mainDepthTexture: GpuTexture? = MinecraftClient.getInstance().framebuffer.depthAttachment
        val viewStack = RenderSystem.getModelViewStack()
        viewStack.pushMatrix()
        viewStack.mul(matrix)
        RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(mainColorTexture, OptionalInt.empty(), mainDepthTexture, OptionalDouble.empty())
            .use { renderPass ->
                material.setup(renderPass, light)
                skin?.getBuffer()?.let { skinBuffer ->
                    if (RenderPassImpl.IS_DEVELOPMENT) {
                        require(material.skinned) { "Primitive has skin data, but material is not skinned" }
                    }
                    renderPass.bindSampler("Joints", skinBuffer)
                }
                renderPass.setVertexBuffer(vertexBuffer)
                indexBuffer?.let { indices ->
                    renderPass.setIndexBuffer(indices)
                    renderPass.drawIndexed(0, indices.length)
                } ?: run {
                    renderPass.draw(0, vertexBuffer.verticesCount)
                }
            }
        viewStack.popMatrix()
    }

    override fun onClosed() {
        vertexBuffer.decreaseReferenceCount()
        indexBuffer?.decreaseReferenceCount()
        material.decreaseReferenceCount()
    }
}
