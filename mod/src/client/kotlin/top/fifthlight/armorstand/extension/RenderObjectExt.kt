package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.systems.RenderPass
import top.fifthlight.armorstand.helper.RenderObjectHelper
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.VertexBuffer
import java.util.function.BiConsumer
import java.util.function.Consumer

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
    get() = (this as RenderObjectExt).`armorStand$getVertexBuffer`()
