package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import org.joml.Vector3fc
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.VertexBuffer
import top.fifthlight.armorstand.render.setIndexBuffer
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.setVertexBuffer
import java.util.*

class RenderPrimitive(
    private val vertexBuffer: VertexBuffer,
    private val indexBuffer: IndexBuffer?,
    private val material: RenderMaterial,
    val positionMin: Vector3fc,
    val positionMax: Vector3fc,
) : AbstractRefCount() {
    init {
        vertexBuffer.increaseReferenceCount()
        indexBuffer?.increaseReferenceCount()
        material.increaseReferenceCount()
    }

    private val mainColorTexture: GpuTexture
        get() = MinecraftClient.getInstance().framebuffer.colorAttachment!!
    private val mainDepthTexture: GpuTexture?
        get() = MinecraftClient.getInstance().framebuffer.depthAttachment

    fun render(matrixStack: MatrixStack, light: Int) {
        val viewStack = RenderSystem.getModelViewStack()
        viewStack.pushMatrix()
        viewStack.mul(matrixStack.peek().positionMatrix)
        RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(mainColorTexture, OptionalInt.empty(), mainDepthTexture, OptionalDouble.empty())
            .use { renderPass ->
                material.setup(renderPass, light)
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
