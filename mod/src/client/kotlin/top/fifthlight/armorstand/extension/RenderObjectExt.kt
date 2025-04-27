package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.systems.RenderPass
import top.fifthlight.armorstand.helper.RenderObjectHelper
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.VertexBuffer
import java.util.function.Consumer

fun RenderObject(
    vertexBufferSlot: Int,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer?,
    firstIndex: Int,
    indexCount: Int,
    uniformUploaderConsumer: Consumer<RenderPass.UniformUploader>? = null,
): RenderPass.RenderObject = RenderObjectHelper.create(
    vertexBufferSlot,
    vertexBuffer,
    indexBuffer,
    firstIndex,
    indexCount,
    uniformUploaderConsumer,
)

@Suppress("CAST_NEVER_SUCCEEDS")
val RenderPass.RenderObject.vertexBuffer
    get() = (this as RenderObjectExt).`armorStand$getVertexBuffer`()
