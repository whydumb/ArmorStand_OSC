package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.pipeline.RenderPipeline
import top.fifthlight.blazerod.extension.RenderPipelineExt

val RenderPipeline.vertexType
    get() = (this as RenderPipelineExt).`blazerod$getVertexType`()
