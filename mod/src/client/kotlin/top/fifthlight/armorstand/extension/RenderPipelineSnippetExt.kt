package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.pipeline.RenderPipeline

@Suppress("CAST_NEVER_SUCCEEDS")
val RenderPipeline.Snippet.vertexType
    get() = (this as RenderPipelineSnippetExt).`armorstand$getVertexType`()

@Suppress("CAST_NEVER_SUCCEEDS")
val RenderPipeline.Snippet.uniformBuffers
    get() = (this as RenderPipelineSnippetExt).`armorstand$getUniformBuffers`()
