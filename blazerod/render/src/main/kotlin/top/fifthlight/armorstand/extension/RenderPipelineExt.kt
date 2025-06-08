package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.pipeline.RenderPipeline

val RenderPipeline.vertexType
    get() = (this as RenderPipelineExt).`armorStand$getVertexType`()
