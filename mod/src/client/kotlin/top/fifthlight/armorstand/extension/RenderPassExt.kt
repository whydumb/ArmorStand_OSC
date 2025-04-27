package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderPass.UniformUploader
import top.fifthlight.armorstand.helper.RenderObjectHelper
import top.fifthlight.armorstand.render.GpuTextureBuffer
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.VertexBuffer
import java.util.function.Consumer

fun RenderPass.setVertexBuffer(vertexBuffer: VertexBuffer) =
    (this as RenderPassExt).`armorStand$setVertexBuffer`(vertexBuffer)

fun RenderPass.bindSampler(name: String, bufferTexture: GpuTextureBuffer) =
    (this as RenderPassExt).`armorStand$bindSampler`(name, bufferTexture)

fun RenderPass.setUniform(name: String, buffer: GpuBuffer?) =
    (this as RenderPassExt).`armorStand$setUniform`(name, buffer)

fun RenderPass.drawIndexedInstanced(instances: Int, offset: Int, count: Int) =
    (this as RenderPassExt).`armorStand$drawIndexedInstanced`(instances, offset, count)

fun RenderPass.drawInstanced(instances: Int, offset: Int, count: Int) =
    (this as RenderPassExt).`armorStand$drawInstanced`(instances, offset, count)
