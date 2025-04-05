package top.fifthlight.armorstand.util

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import net.minecraft.client.gl.RenderPassImpl
import top.fifthlight.armorstand.helper.RenderPassWithVertexBuffer
import top.fifthlight.armorstand.helper.RenderPipelineWithVertexType
import top.fifthlight.armorstand.model.VertexType
import top.fifthlight.armorstand.render.VertexBuffer

fun RenderPipeline.Builder.withVertexType(type: VertexType) = also {
    (this as RenderPipelineWithVertexType).`armorStand$setVertexType`(type)
}

fun RenderPipeline.Builder.getVertexType() =
    (this as RenderPipelineWithVertexType).`armorStand$getVertexType`()

fun RenderPipeline.Snippet.withVertexType(type: VertexType) = also {
    (this as RenderPipelineWithVertexType).`armorStand$setVertexType`(type)
}

fun RenderPipeline.Snippet.getVertexType() =
    (this as RenderPipelineWithVertexType).`armorStand$getVertexType`()

fun RenderPipeline.withVertexType(type: VertexType) = also {
    (this as RenderPipelineWithVertexType).`armorStand$setVertexType`(type)
}

fun RenderPipeline.getVertexType() =
    (this as RenderPipelineWithVertexType).`armorStand$getVertexType`()
