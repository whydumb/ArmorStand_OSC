package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.systems.RenderPass
import top.fifthlight.armorstand.render.VertexBuffer

fun RenderPass.setVertexBuffer(vertexBuffer: VertexBuffer) =
    (this as RenderPassExt).`armorStand$setVertexBuffer`(vertexBuffer)

fun RenderPass.draw(baseVertex: Int, firstIndex: Int, count: Int, instanceCount: Int) =
    (this as RenderPassExt).`armorStand$draw`(baseVertex, firstIndex, count, instanceCount)