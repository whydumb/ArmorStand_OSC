package top.fifthlight.armorstand.util

import com.mojang.blaze3d.systems.RenderPass
import net.minecraft.client.gl.RenderPassImpl
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer
import top.fifthlight.armorstand.render.VertexBuffer

fun RenderPass.setVertexBuffer(vertexBuffer: VertexBuffer) =
    ((this as RenderPassImpl) as RenderPassWithVertexBuffer).`armorStand$setVertexBuffer`(vertexBuffer)

fun RenderPass.getVertexBuffer() =
    ((this as RenderPassImpl) as RenderPassWithVertexBuffer).`armorStand$getVertexBuffer`()
