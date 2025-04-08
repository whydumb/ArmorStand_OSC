package top.fifthlight.armorstand.util

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.RenderPipelines.MATRICES_COLOR_SNIPPET
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexFormats

object RenderLayers {
    val DEBUG_BONE_LINES: RenderLayer = RenderLayer.of(
        "debug_bone_lines",
        1536,
        false,
        false,
        RenderPipelines.register(
            RenderPipeline.builder(MATRICES_COLOR_SNIPPET)
                .withLocation("pipeline/debug_line_strip")
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                .withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build()
        ),
        RenderLayer.MultiPhaseParameters.builder().build(false)
    )
}