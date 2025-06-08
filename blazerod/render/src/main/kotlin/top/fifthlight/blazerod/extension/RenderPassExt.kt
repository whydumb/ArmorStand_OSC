package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.systems.RenderPass
import top.fifthlight.blazerod.render.VertexBuffer

fun RenderPass.setVertexBuffer(vertexBuffer: VertexBuffer) =
    (this as RenderPassExt).`blazerod$setVertexBuffer`(vertexBuffer)

fun RenderPass.draw(baseVertex: Int, firstIndex: Int, count: Int, instanceCount: Int) =
    (this as RenderPassExt).`blazerod$draw`(baseVertex, firstIndex, count, instanceCount)