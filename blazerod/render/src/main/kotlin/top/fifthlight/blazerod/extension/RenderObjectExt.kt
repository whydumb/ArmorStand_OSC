package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.systems.RenderPass
import top.fifthlight.blazerod.helper.RenderObjectHelper
import top.fifthlight.blazerod.render.IndexBuffer
import top.fifthlight.blazerod.render.VertexBuffer
import java.util.function.BiConsumer

fun <T> RenderObject(
    vertexBufferSlot: Int,
    vertexBuffer: VertexBuffer,
    indexBuffer: IndexBuffer?,
    firstIndex: Int,
    indexCount: Int,
    uniformUploaderConsumer: BiConsumer<T, RenderPass.UniformUploader>? = null,
): RenderPass.RenderObject<T> = RenderObjectHelper.create(
    vertexBufferSlot,
    vertexBuffer,
    indexBuffer,
    firstIndex,
    indexCount,
    uniformUploaderConsumer,
)

@Suppress("CAST_NEVER_SUCCEEDS")
val <T> RenderPass.RenderObject<T>.vertexBuffer
    get() = (this as RenderObjectExt).`blazerod$getVertexBuffer`()
