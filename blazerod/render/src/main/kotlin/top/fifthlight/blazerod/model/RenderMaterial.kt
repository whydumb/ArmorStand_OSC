@file:Suppress("NOTHING_TO_INLINE")

package top.fifthlight.blazerod.model

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.BlazeRod
import top.fifthlight.blazerod.extension.TextureFormatExt
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.BitmapItem
import top.fifthlight.blazerod.extension.withVertexType
import top.fifthlight.blazerod.model.uniform.UniformBuffer
import top.fifthlight.blazerod.model.uniform.UnlitDataUniformBuffer
import top.fifthlight.blazerod.model.Material.AlphaMode
import top.fifthlight.blazerod.model.Material.AlphaMode.OPAQUE

abstract class RenderMaterial<Desc : RenderMaterial.Descriptor> : AbstractRefCount() {
    companion object {
        private val descriptors = listOf(
            // Pbr.Descriptor,
            Unlit.Descriptor,
        )

        val defaultMaterial by lazy {
            // Increase reference count to avoid being closed
            Unlit(name = "Default").apply { increaseReferenceCount() }
        }

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
    abstract val morphed: Boolean

    val supportInstancing: Boolean
        get() = descriptor.supportInstancing
    val supportMorphing: Boolean
        get() = descriptor.supportMorphing

    @JvmInline
    value class PipelineInfo(val bitmap: BitmapItem = BitmapItem()) {
        constructor(
            doubleSided: Boolean = true,
            skinned: Boolean = false,
            instanced: Boolean = false,
            morphed: Boolean = false,
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
            if (morphed) {
                item += ELEMENT_MORPHED
            }
            item
        })

        val doubleSided
            get() = ELEMENT_DOUBLE_SIDED in bitmap
        val skinned
            get() = ELEMENT_SKINNED in bitmap
        val instanced
            get() = ELEMENT_INSTANCED in bitmap
        val morphed
            get() = ELEMENT_MORPHED in bitmap

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
            if (morphed) {
                append("_morphed")
            }
        }

        fun pipelineSnippet(): RenderPipeline.Snippet = RenderPipeline.builder().apply {
            withCull(!doubleSided)
            withUniform("Projection", UniformType.UNIFORM_BUFFER)
            if (morphed) {
                withShaderDefine("MORPHED")
                withShaderDefine("MAX_ENABLED_MORPH_TARGETS", BlazeRod.MAX_ENABLED_MORPH_TARGETS)
                withUniform("MorphData", UniformType.UNIFORM_BUFFER)
                withUniform("MorphModelIndices", UniformType.UNIFORM_BUFFER)
                withUniform("MorphPositionData", UniformType.TEXEL_BUFFER, TextureFormatExt.RGB32F)
                withUniform("MorphColorData", UniformType.TEXEL_BUFFER, TextureFormatExt.RGBA32F)
                withUniform("MorphTexCoordData", UniformType.TEXEL_BUFFER, TextureFormatExt.RG32F)
                withUniform("MorphTargetIndices", UniformType.TEXEL_BUFFER, TextureFormatExt.R32I)
                withUniform("MorphWeights", UniformType.TEXEL_BUFFER, TextureFormatExt.R32F)
            }
            if (skinned) {
                withShaderDefine("SKINNED")
                withUniform("SkinModelIndices", UniformType.UNIFORM_BUFFER)
                withUniform("Joints", UniformType.TEXEL_BUFFER, TextureFormatExt.RGBA32F)
            }
            withUniform("InstanceData", UniformType.UNIFORM_BUFFER)
            withUniform("LocalMatrices", UniformType.TEXEL_BUFFER, TextureFormatExt.RGBA32F)
            withShaderDefine("INSTANCE_SIZE", BlazeRod.INSTANCE_SIZE)
            if (instanced) {
                withShaderDefine("INSTANCED")
            }
        }.buildSnippet()

        companion object {
            val ELEMENT_DOUBLE_SIDED = BitmapItem.Element.of(0)
            val ELEMENT_SKINNED = BitmapItem.Element.of(1)
            val ELEMENT_INSTANCED = BitmapItem.Element.of(2)
            val ELEMENT_MORPHED = BitmapItem.Element.of(3)
        }

        inline operator fun plus(element: BitmapItem.Element) =
            PipelineInfo(bitmap + element)

        inline operator fun minus(element: BitmapItem.Element) =
            PipelineInfo(bitmap - element)

        inline operator fun contains(element: BitmapItem.Element) =
            element in bitmap

        override fun toString(): String {
            return "PipelineInfo(doubleSided=$doubleSided, skinned=$skinned, instanced=$instanced, morphed=$morphed)"
        }
    }

    abstract val descriptor: Desc

    abstract class Descriptor {
        abstract val name: String
        val typeId: Identifier = Identifier.of("blazerod", "material_$name")
        open val supportInstancing: Boolean
            get() = false
        open val supportMorphing: Boolean
            get() = false

        abstract fun setupPipeline(
            info: PipelineInfo,
            snippet: RenderPipeline.Snippet,
            location: Identifier,
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
                val location = Identifier.of("blazerod", name + info.nameSuffix())
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
            morphed = morphed,
        )
        return descriptor.pipelines[info.bitmap.inner] ?: error("No pipeline for pipeline info $info")
    }

    override val typeId: Identifier
        get() = descriptor.typeId

    // FIXME: don't use pair, which allocate objects every frame
    open fun setup(instanced: Boolean = false, renderPassCreator: () -> RenderPass): Pair<RenderPass, UniformBuffer<*, *>?> {
        val renderPass = renderPassCreator()
        renderPass.setPipeline(getPipeline(instanced))
        RenderSystem.bindDefaultUniforms(renderPass)
        return Pair(renderPass, null)
    }

    abstract override fun onClosed()

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

        override val morphed: Boolean
            get() = false

        override fun setup(instanced: Boolean, renderPassCreator: () -> RenderPass): Pair<RenderPass, UniformBuffer<*, *>?> = TODO()

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
                location: Identifier,
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
        override val doubleSided: Boolean = false,
        override val skinned: Boolean = false,
        override val morphed: Boolean = false,
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

        override fun setup(instanced: Boolean, renderPassCreator: () -> RenderPass): Pair<RenderPass, UniformBuffer<*, *>?> {
            val unlitData = UnlitDataUniformBuffer.Companion.acquire()
            unlitData.write {
                baseColor = this@Unlit.baseColor
            }
            val (renderPass, _) = super.setup(instanced, renderPassCreator)
            with(renderPass) {
                setUniform("UnlitData", unlitData.slice)
                bindSampler("SamplerBaseColor", baseColorTexture.texture.view)
                val lightMapTexture = MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.glTextureView
                bindSampler("SamplerLightMap", lightMapTexture)
            }
            return Pair(renderPass, unlitData)
        }

        override fun onClosed() {
            baseColorTexture.decreaseReferenceCount()
        }

        companion object Descriptor : RenderMaterial.Descriptor() {
            override val name: String = "unlit"

            override val supportInstancing: Boolean
                get() = true

            override val supportMorphing: Boolean
                get() = true

            override val supportedPipelineElements = listOf(
                PipelineInfo.ELEMENT_DOUBLE_SIDED,
                PipelineInfo.ELEMENT_SKINNED,
                PipelineInfo.ELEMENT_INSTANCED,
                PipelineInfo.ELEMENT_MORPHED,
            )

            override fun setupPipeline(
                info: PipelineInfo,
                snippet: RenderPipeline.Snippet,
                location: Identifier,
            ): RenderPipeline =
                RenderPipeline.builder(
                    snippet,
                    RenderPipelines.FOG_SNIPPET,
                ).apply {
                    withLocation(location)
                    withVertexShader(Identifier.of("blazerod", "core/unlit"))
                    withFragmentShader(Identifier.of("blazerod", "core/unlit"))
                    withBlend(BlendFunction.TRANSLUCENT)
                    withSampler("SamplerBaseColor")
                    withSampler("SamplerLightMap")
                    if (info.skinned) {
                        withVertexType(VertexType.POSITION_TEXTURE_COLOR_JOINT_WEIGHT)
                    } else {
                        withVertexType(VertexType.POSITION_TEXTURE_COLOR)
                    }
                    withUniform("UnlitData", UniformType.UNIFORM_BUFFER)
                }.build()
        }
    }
}
