package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.pipeline.RenderPipeline
import top.fifthlight.blazerod.extension.RenderPipelineSnippetExt

@Suppress("CAST_NEVER_SUCCEEDS")
val RenderPipeline.Snippet.vertexType
    get() = (this as RenderPipelineSnippetExt).`blazerod$getVertexType`()
