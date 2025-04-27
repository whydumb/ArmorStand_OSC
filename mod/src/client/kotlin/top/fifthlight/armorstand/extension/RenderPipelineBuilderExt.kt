package top.fifthlight.armorstand.extension

import com.mojang.blaze3d.pipeline.RenderPipeline
import top.fifthlight.armorstand.model.VertexType

fun RenderPipeline.Builder.withVertexType(type: VertexType) = also {
    (this as RenderPipelineBuilderExt).`armorStand$withVertexType`(type)
}

fun RenderPipeline.Builder.withUniformBuffer(name: String) =
    (this as RenderPipelineBuilderExt).`armorStand$withUniformBuffer`(name)