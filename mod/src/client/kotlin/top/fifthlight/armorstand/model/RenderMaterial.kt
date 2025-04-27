@file:Suppress("NOTHING_TO_INLINE")

package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.util.BitmapItem
import top.fifthlight.armorstand.extension.setUniform
import top.fifthlight.armorstand.extension.withUniformBuffer
import top.fifthlight.armorstand.extension.withVertexType
import top.fifthlight.renderer.model.Material.AlphaMode
import top.fifthlight.renderer.model.Material.AlphaMode.OPAQUE
import top.fifthlight.renderer.model.RgbColor
import top.fifthlight.renderer.model.RgbaColor

abstract class RenderMaterial<Desc : RenderMaterial.Descriptor> : AbstractRefCount() {
    companion object {
        private val descriptors = listOf(
            // Pbr.Descriptor,
            Unlit.Descriptor,
            Fallback.Descriptor,
        )

        fun initialize() {
            for (descriptor in descriptors) {
                descriptor.initialize()
            }
        }
    }

    abstract val name: String?
    abstract val baseColor: RgbaColor
    abstract val baseColorTexture: RenderTexture?
    abstract val alphaMode: AlphaMode
    abstract val alphaCutoff: Float
    abstract val doubleSided: Boolean
    abstract val skinned: Boolean
    val supportInstancing: Boolean
        get() = descriptor.supportInstancing

    @JvmInline
    value class PipelineInfo(val bitmap: BitmapItem = BitmapItem()) {
        constructor(
            doubleSided: Boolean = true,
            skinned: Boolean = false,
            instanced: Boolean = false
        ) : this(Unit.run {
            var item = BitmapItem()
            if (doubleSided) {
                item += ELEMENT_DOUBLE_SIDED
            }
            if (skinned) {
                item += ELEMENT_SKINNED
            }
            if (instanced) {
                item += ELEMENT_INSTANCED
            }
            item
        })

        val doubleSided
            get() = ELEMENT_DOUBLE_SIDED in bitmap
        val skinned
            get() = ELEMENT_SKINNED in bitmap
        val instanced
            get() = ELEMENT_INSTANCED in bitmap

        fun nameSuffix() = buildString {
            if (doubleSided) {
                append("_cull")
            } else {
                append("_no_cull")
            }
            if (skinned) {
                append("_skinned")
            }
            if (instanced) {
                append("_instanced")
            }
        }

        fun pipelineSnippet(): RenderPipeline.Snippet = RenderPipeline.builder().apply {
            withCull(!doubleSided)
            if (skinned) {
                withShaderDefine("SKINNED")
                withSampler("Joints")
            }
            if (instanced) {
                withShaderDefine("INSTANCED")
                withShaderDefine("INSTANCE_SIZE", ArmorStandClient.INSTANCE_SIZE)
                withUniformBuffer("Instances")
                if (skinned) {
                    withUniform("ModelJoints", UniformType.INT)
                }
            } else {
                withUniform("ProjMat", UniformType.MATRIX4X4)
                withUniform("ModelViewMat", UniformType.MATRIX4X4)
            }
        }.buildSnippet()

        companion object {
            val ELEMENT_DOUBLE_SIDED = BitmapItem.Element.of(0)
            val ELEMENT_SKINNED = BitmapItem.Element.of(1)
            val ELEMENT_INSTANCED = BitmapItem.Element.of(2)
        }

        inline operator fun plus(element: BitmapItem.Element) =
            PipelineInfo(bitmap + element)

        inline operator fun minus(element: BitmapItem.Element) =
            PipelineInfo(bitmap - element)

        inline operator fun contains(element: BitmapItem.Element) =
            element in bitmap

        override fun toString(): String {
            return "PipelineInfo(doubleSided=$doubleSided, skinned=$skinned, instanced=$instanced)"
        }
    }

    abstract val descriptor: Desc

    abstract class Descriptor {
        abstract val name: String
        val typeId: Identifier = Identifier.of("armorstand", "material_$name")
        open val supportInstancing: Boolean
            get() = false

        abstract fun setupPipeline(
            info: PipelineInfo,
            snippet: RenderPipeline.Snippet,
            location: Identifier
        ): RenderPipeline

        abstract val supportedPipelineElements: List<BitmapItem.Element>
        lateinit var pipelines: Array<RenderPipeline?>

        fun initialize() {
            val elements = supportedPipelineElements
            val bitSize = (1 shl elements.size)
            val array = arrayOfNulls<RenderPipeline?>(bitSize)
            for (i in 0 until bitSize) {
                var infoBitmap = 0
                for ((index, element) in elements.withIndex()) {
                    val indexMask = 1 shl index
                    if (i and indexMask == 0) {
                        continue
                    }
                    infoBitmap = infoBitmap or element.mask
                }

                val info = PipelineInfo(BitmapItem(infoBitmap))
                val location = Identifier.of("armorstand", name + info.nameSuffix())
                val pipeline = setupPipeline(info, info.pipelineSnippet(), location)
                array[infoBitmap] = pipeline
                RenderPipelines.register(pipeline)
            }
            pipelines = array
        }
    }

    abstract val vertexType: VertexType
    fun getPipeline(instanced: Boolean): RenderPipeline {
        val info = PipelineInfo(
            doubleSided = doubleSided,
            skinned = skinned,
            instanced = instanced,
        )
        return descriptor.pipelines[info.bitmap.inner] ?: error("No pipeline for pipeline info $info")
    }

    override val typeId: Identifier
        get() = descriptor.typeId

    open fun setup(renderPass: RenderPass, light: Int) {
        renderPass.setPipeline(getPipeline(false))
    }

    open fun setupInstanced(renderPass: RenderPass) {
        if (!descriptor.supportInstancing) {
            throw UnsupportedOperationException("Instanced rendering is not supported for pipeline $name")
        }
        renderPass.setPipeline(getPipeline(true))
    }

    abstract override fun onClosed()

    class Pbr private constructor(
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
        override val doubleSided: Boolean,
        override val skinned: Boolean,
    ) : RenderMaterial<Pbr.Descriptor>() {
        init {
            baseColorTexture.increaseReferenceCount()
            metallicRoughnessTexture.increaseReferenceCount()
            normalTexture.increaseReferenceCount()
            occlusionTexture.increaseReferenceCount()
            emissiveTexture.increaseReferenceCount()
        }

        override val descriptor
            get() = Descriptor

        override val vertexType: VertexType
            get() = TODO("Not yet implemented")

        override fun setup(renderPass: RenderPass, light: Int) = TODO()

        override fun onClosed() {
            baseColorTexture.decreaseReferenceCount()
            metallicRoughnessTexture.decreaseReferenceCount()
            normalTexture.decreaseReferenceCount()
            occlusionTexture.decreaseReferenceCount()
            emissiveTexture.decreaseReferenceCount()
        }

        companion object Descriptor : RenderMaterial.Descriptor() {
            override val name: String = "pbr"

            override val supportInstancing: Boolean
                get() = true

            override val supportedPipelineElements = listOf(
                PipelineInfo.ELEMENT_DOUBLE_SIDED,
                PipelineInfo.ELEMENT_SKINNED,
                PipelineInfo.ELEMENT_INSTANCED,
            )

            override fun setupPipeline(
                info: PipelineInfo,
                snippet: RenderPipeline.Snippet,
                location: Identifier
            ): RenderPipeline {
                TODO("Not yet implemented")
            }
        }
    }

    class Unlit(
        override val name: String?,
        override val baseColor: RgbaColor = RgbaColor(1f, 1f, 1f, 1f),
        override val baseColorTexture: RenderTexture = RenderTexture.WHITE_RGBA_TEXTURE,
        override val alphaMode: AlphaMode = OPAQUE,
        override val alphaCutoff: Float = .5f,
        override val doubleSided: Boolean,
        override val skinned: Boolean,
    ) : RenderMaterial<Unlit.Descriptor>() {
        init {
            baseColorTexture.increaseReferenceCount()
        }

        override val descriptor
            get() = Descriptor

        override val vertexType: VertexType
            get() = if (skinned) {
                VertexType.POSITION_TEXTURE_COLOR_JOINT_WEIGHT
            } else {
                VertexType.POSITION_TEXTURE_COLOR
            }

        override fun setup(renderPass: RenderPass, light: Int) {
            super.setup(renderPass, light)
            with(renderPass) {
                setUniform("BaseColor", baseColor.r, baseColor.g, baseColor.b, baseColor.a)
                setUniform(
                    "LightMapUv",
                    light and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                    (light shr 16) and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                    0,
                )
                bindSampler("SamplerBaseColor", baseColorTexture.texture.inner)
                bindSampler(
                    "SamplerLightMap",
                    MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.glTexture
                )
            }
        }

        override fun setupInstanced(renderPass: RenderPass) {
            super.setupInstanced(renderPass)
            with(renderPass) {
                setUniform("BaseColor", baseColor.r, baseColor.g, baseColor.b, baseColor.a)
                setUniform("Instances", null as GpuBuffer?)
                bindSampler("SamplerBaseColor", baseColorTexture.texture.inner)
                bindSampler(
                    "SamplerLightMap",
                    MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.glTexture
                )
            }
        }

        override fun onClosed() {
            baseColorTexture.decreaseReferenceCount()
        }

        companion object Descriptor : RenderMaterial.Descriptor() {
            override val name: String = "unlit"

            override val supportInstancing: Boolean
                get() = true

            override val supportedPipelineElements = listOf(
                PipelineInfo.ELEMENT_DOUBLE_SIDED,
                PipelineInfo.ELEMENT_SKINNED,
                PipelineInfo.ELEMENT_INSTANCED,
            )

            override fun setupPipeline(
                info: PipelineInfo,
                snippet: RenderPipeline.Snippet,
                location: Identifier
            ): RenderPipeline =
                RenderPipeline.builder(snippet).apply {
                    withLocation(location)
                    withVertexShader(Identifier.of("armorstand", "core/unlit"))
                    withFragmentShader(Identifier.of("armorstand", "core/unlit"))
                    withoutBlend()
                    withSampler("SamplerBaseColor")
                    withSampler("SamplerLightMap")
                    if (info.skinned) {
                        withVertexType(VertexType.POSITION_TEXTURE_COLOR_JOINT_WEIGHT)
                    } else {
                        withVertexType(VertexType.POSITION_TEXTURE_COLOR)
                    }

                    // Vertex
                    withUniform("FogShape", UniformType.INT)

                    // Fragment
                    withUniform("FogStart", UniformType.FLOAT)
                    withUniform("FogEnd", UniformType.FLOAT)
                    withUniform("FogColor", UniformType.VEC4)
                    withUniform("ColorModulator", UniformType.VEC4)
                    withUniform("BaseColor", UniformType.VEC4)

                    if (!info.instanced) {
                        withUniform("LightMapUv", UniformType.IVEC3)
                    }
                }.build()
        }
    }

    object Fallback : RenderMaterial<Fallback.Descriptor>() {
        override val descriptor
            get() = Descriptor
        override val vertexType: VertexType
            get() = VertexType.POSITION
        override val name: String
            get() = "Fallback"
        override val baseColor: RgbaColor
            get() = RgbaColor(1f, 0f, 1f, 1f)
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

        override fun onClosed() = Unit

        object Descriptor : RenderMaterial.Descriptor() {
            override val name: String = "fallback"

            override val supportedPipelineElements = listOf(PipelineInfo.ELEMENT_DOUBLE_SIDED)

            override fun setupPipeline(
                info: PipelineInfo,
                snippet: RenderPipeline.Snippet,
                location: Identifier
            ): RenderPipeline =
                RenderPipeline.builder(snippet).apply {
                    withLocation(location)
                    withVertexShader("core/position")
                    withFragmentShader("core/position")
                    withoutBlend()

                    withUniform("ProjMat", UniformType.MATRIX4X4)
                    withUniform("ModelViewMat", UniformType.MATRIX4X4)

                    withUniform("ColorModulator", UniformType.VEC4)

                    withUniform("FogStart", UniformType.FLOAT)
                    withUniform("FogEnd", UniformType.FLOAT)
                    withUniform("FogColor", UniformType.VEC4)
                    withUniform("FogShape", UniformType.INT)

                    withVertexType(VertexType.POSITION)
                }.build()
        }
    }
}
