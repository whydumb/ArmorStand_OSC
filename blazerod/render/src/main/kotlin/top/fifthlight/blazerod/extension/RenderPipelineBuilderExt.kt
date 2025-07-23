package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.pipeline.RenderPipeline
import top.fifthlight.blazerod.model.resource.VertexType

fun RenderPipeline.Builder.withVertexType(type: VertexType) = also {
    (this as RenderPipelineBuilderExt).`blazerod$withVertexType`(type)
}

fun RenderPipeline.Builder.withStorageBuffer(name: String) = also {
    (this as RenderPipelineBuilderExt).`blazerod$withStorageBuffer`(name)
}
