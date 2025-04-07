package top.fifthlight.armorstand.util

import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderPass.UniformUploader
import net.minecraft.client.gl.RenderPassImpl
import top.fifthlight.armorstand.helper.RenderPassWithTextureBuffer
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer
import top.fifthlight.armorstand.render.GpuTextureBuffer
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.VertexBuffer
import java.util.function.Consumer

fun RenderPass.setVertexBuffer(vertexBuffer: VertexBuffer) =
    ((this as RenderPassImpl) as RenderPassWithVertexBuffer).`armorStand$setVertexBuffer`(vertexBuffer)

fun RenderPass.getVertexBuffer() =
    ((this as RenderPassImpl) as RenderPassWithVertexBuffer).`armorStand$getVertexBuffer`()

fun RenderPass.bindSampler(name: String, bufferTexture: GpuTextureBuffer) =
    ((this as RenderPassImpl) as RenderPassWithTextureBuffer).`armorStand$bindSampler`(name, bufferTexture)

fun RenderObject(
    vertexBufferSlot: Int,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer?,
    firstIndex: Int,
    indexCount: Int,
    uniformUploaderConsumer: Consumer<UniformUploader>? = null,
) = RenderPass.RenderObject(
    vertexBufferSlot,
    null,
    indexBuffer?.buffer?.inner,
    indexBuffer?.type,
    firstIndex,
    indexCount,
    uniformUploaderConsumer,
).also {
    val renderObject = it as Any
    @Suppress("KotlinConstantConditions")
    (renderObject as RenderPassWithVertexBuffer).`armorStand$setVertexBuffer`(vertexBuffer)
}