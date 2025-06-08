package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.pipeline.RenderPipeline
import top.fifthlight.blazerod.model.VertexType

fun RenderPipeline.Builder.withVertexType(type: VertexType) = also {
    (this as RenderPipelineBuilderExt).`blazerod$withVertexType`(type)
}
