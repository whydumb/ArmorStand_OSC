package top.fifthlight.armorstand.model

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.getVertexType
import top.fifthlight.armorstand.util.withVertexType
import top.fifthlight.renderer.model.Material.AlphaMode
import top.fifthlight.renderer.model.Material.AlphaMode.OPAQUE
import top.fifthlight.renderer.model.RgbColor
import top.fifthlight.renderer.model.RgbaColor

sealed class RenderMaterial : AbstractRefCount() {
    abstract val name: String?
    abstract val baseColor: RgbaColor
    abstract val baseColorTexture: RenderTexture?
    abstract val alphaMode: AlphaMode
    abstract val alphaCutoff: Float
    abstract val doubleSided: Boolean
    abstract val skinned: Boolean

    abstract val pipeline: RenderPipeline
    val vertexType
        get() = pipeline.getVertexType()!!

    open fun setup(renderPass: RenderPass, light: Int) {
        renderPass.setPipeline(pipeline)
    }

    class Pbr(
        override val name: String?,
        override val baseColor: RgbaColor = RgbaColor(1f, 1f, 1f, 1f),
        override val baseColorTexture: RenderTexture = RenderTexture.WHITE_RGBA_TEXTURE,
        val metallicFactor: Float = 1f,
        val roughnessFactor: Float = 1f,
        val metallicRoughnessTexture: RenderTexture = RenderTexture.WHITE_RGBA_TEXTURE,
        val normalTexture: RenderTexture = RenderTexture.WHITE_RGBA_TEXTURE,
        val occlusionTexture: RenderTexture = RenderTexture.WHITE_RGBA_TEXTURE,
        val emissiveTexture: RenderTexture = RenderTexture.WHITE_RGBA_TEXTURE,
        val emissiveFactor: RgbColor = RgbColor(1f, 1f, 1f),
        override val alphaMode: AlphaMode = OPAQUE,
        override val alphaCutoff: Float = .5f,
        override val doubleSided: Boolean = false,
    ) : RenderMaterial() {
        override val skinned
            get() = false

        init {
            baseColorTexture.increaseReferenceCount()
            metallicRoughnessTexture.increaseReferenceCount()
            normalTexture.increaseReferenceCount()
            occlusionTexture.increaseReferenceCount()
            emissiveTexture.increaseReferenceCount()
        }

        override val pipeline = TODO()

        override fun setup(renderPass: RenderPass, light: Int) = TODO()

        override fun onClosed() {
            baseColorTexture.decreaseReferenceCount()
            metallicRoughnessTexture.decreaseReferenceCount()
            normalTexture.decreaseReferenceCount()
            occlusionTexture.decreaseReferenceCount()
            emissiveTexture.decreaseReferenceCount()
        }
    }

    class Unlit(
        override val name: String?,
        override val baseColor: RgbaColor = RgbaColor(1f, 1f, 1f, 1f),
        override val baseColorTexture: RenderTexture = RenderTexture.WHITE_RGBA_TEXTURE,
        override val alphaMode: AlphaMode = OPAQUE,
        override val alphaCutoff: Float = .5f,
        override val doubleSided: Boolean = false,
        override val skinned: Boolean = false,
    ) : RenderMaterial() {
        init {
            baseColorTexture.increaseReferenceCount()
        }

        override val pipeline = if (skinned) {
            if (doubleSided) {
                PIPELINE_NO_CULL_SKINNED
            } else {
                PIPELINE_CULL_SKINNED
            }
        } else {
            if (doubleSided) {
                PIPELINE_NO_CULL
            } else {
                PIPELINE_CULL
            }
        }

        override fun setup(renderPass: RenderPass, light: Int) {
            super.setup(renderPass, light)
            renderPass.setUniform("BaseColor", baseColor.r, baseColor.g, baseColor.b, baseColor.a)
            renderPass.bindSampler("Sampler0", baseColorTexture.texture.inner)
            renderPass.bindSampler("Sampler2", MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.glTexture)
            renderPass.setUniform(
                "LightMapUv",
                light and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                (light shr 16) and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                0,
            )
        }

        override fun onClosed() {
            baseColorTexture.decreaseReferenceCount()
        }

        companion object {
            private val PIPELINE_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                .withVertexShader(Identifier.of("armorstand", "core/unlit"))
                .withFragmentShader(Identifier.of("armorstand", "core/unlit"))
                .withSampler("Sampler0")
                .withSampler("Sampler2")
                .withUniform("BaseColor", UniformType.VEC4)
                .withUniform("LightMapUv", UniformType.IVEC3)
                .withVertexType(VertexType.POSITION_TEXTURE_COLOR)
                .withoutBlend()
                .buildSnippet()

            private val SKIN_SNIPPET = RenderPipeline.builder(RenderMaterial.SKIN_SNIPPET)
                .withVertexType(VertexType.POSITION_TEXTURE_COLOR_JOINT_WEIGHT)
                .buildSnippet()

            private val PIPELINE_NO_CULL: RenderPipeline = RenderPipeline.builder(PIPELINE_SNIPPET)
                .withLocation(Identifier.of("armorstand", "unlit_no_cull"))
                .withCull(false)
                .build()

            private val PIPELINE_CULL: RenderPipeline = RenderPipeline.builder(PIPELINE_SNIPPET)
                .withLocation(Identifier.of("armorstand", "unlit_cull"))
                .withCull(true)
                .build()

            private val PIPELINE_NO_CULL_SKINNED: RenderPipeline = RenderPipeline.builder(PIPELINE_SNIPPET, SKIN_SNIPPET)
                .withLocation(Identifier.of("armorstand", "unlit_no_cull_skinned"))
                .withCull(false)
                .build()

            private val PIPELINE_CULL_SKINNED: RenderPipeline = RenderPipeline.builder(PIPELINE_SNIPPET, SKIN_SNIPPET)
                .withLocation(Identifier.of("armorstand", "unlit_cull_skinned"))
                .withCull(true)
                .build()

            val PIPELINES = listOf(
                PIPELINE_NO_CULL,
                PIPELINE_CULL,
                PIPELINE_NO_CULL_SKINNED,
                PIPELINE_CULL_SKINNED,
            )
        }
    }

    object Default : RenderMaterial() {
        override val name: String
            get() = "Default"
        override val baseColor: RgbaColor
            get() = RgbaColor(1f, 1f, 1f, 1f)
        override val baseColorTexture: RenderTexture?
            get() = null
        override val alphaMode: AlphaMode
            get() = OPAQUE
        override val alphaCutoff: Float
            get() = .5f
        override val doubleSided: Boolean
            get() = false
        override val skinned: Boolean
            get() = false

        override val pipeline: RenderPipeline = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
            .withLocation(Identifier.of("armorstand", "default"))
            .withVertexShader("core/position")
            .withFragmentShader("core/position")
            .withVertexType(VertexType.POSITION)
            .withoutBlend()
            .withCull(false)
            .build()

        override fun onClosed() = Unit
    }

    abstract override fun onClosed()

    companion object {
        private val SKIN_SNIPPET = RenderPipeline.builder()
            .withSampler("Joints")
            .withShaderDefine("SKINNED")
            .buildSnippet()

        val PIPELINES = Unlit.PIPELINES + Default.pipeline
    }
}
