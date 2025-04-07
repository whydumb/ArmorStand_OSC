package top.fifthlight.armorstand.util

import com.mojang.blaze3d.pipeline.RenderPipeline
import top.fifthlight.armorstand.helper.RenderPipelineWithVertexType
import top.fifthlight.armorstand.model.VertexType

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
