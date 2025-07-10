package top.fifthlight.blazerod.model

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.util.Identifier
import org.joml.Matrix4fc
import org.joml.Vector2i
import top.fifthlight.blazerod.extension.draw
import top.fifthlight.blazerod.extension.setVertexBuffer
import top.fifthlight.blazerod.extension.shaderDataPool
import top.fifthlight.blazerod.model.data.ModelMatricesBuffer
import top.fifthlight.blazerod.model.data.MorphTargetBuffer
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.model.uniform.InstanceDataUniformBuffer
import top.fifthlight.blazerod.model.uniform.MorphDataUniformBuffer
import top.fifthlight.blazerod.model.uniform.SkinModelIndicesUniformBuffer
import top.fifthlight.blazerod.render.IndexBuffer
import top.fifthlight.blazerod.render.VertexBuffer
import top.fifthlight.blazerod.render.setIndexBuffer
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.upload
import java.util.*

class RenderPrimitive(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer?,
    val material: RenderMaterial<*>,
    val targets: Targets?,
    val targetGroups: List<MorphTargetGroup>,
) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "primitive")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    init {
        vertexBuffer.increaseReferenceCount()
        indexBuffer?.increaseReferenceCount()
        material.increaseReferenceCount()
        if (targetGroups.isEmpty()) {
            require(targets == null) { "Empty target groups with non-empty targets" }
        } else {
            require(targets != null) { "Non-empty target groups with empty targets" }
        }
    }

    class Target(
        val data: GpuBuffer,
        val targetsCount: Int,
    ) : AutoCloseable by data {
        init {
            require(data.usage() and GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER != 0) {
                "RenderPrimitive's target should have buffer with usage USAGE_UNIFORM_TEXEL_BUFFER"
            }
        }
    }

    class Targets(
        val position: Target,
        val color: Target,
        val texCoord: Target,
    )

    private fun RenderPass.bindMorphTargets(targets: Targets) {
        setUniform("MorphPositionData", targets.position.data)
        setUniform("MorphColorData", targets.color.data)
        setUniform("MorphTexCoordData", targets.texCoord.data)
    }

    private val lightVector = Vector2i()

    fun render(
        scene: RenderScene,
        primitiveIndex: Int,
        viewModelMatrix: Matrix4fc,
        light: Int,
        modelMatricesBuffer: ModelMatricesBuffer,
        skinBuffer: RenderSkinBuffer?,
        targetBuffer: MorphTargetBuffer?,
    ) {
        val mainColorTextureView: GpuTextureView = MinecraftClient.getInstance().framebuffer.colorAttachmentView!!
        val mainDepthTextureView: GpuTextureView? = MinecraftClient.getInstance().framebuffer.depthAttachmentView
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()
        var renderPass: RenderPass? = null
        val instanceDataUniformBufferSlice: GpuBufferSlice
        val modelMatricesBufferSlice: GpuBufferSlice
        var skinModelIndicesBufferSlice: GpuBufferSlice? = null
        var skinJointBufferSlice: GpuBufferSlice? = null
        var morphDataUniformBufferSlice: GpuBufferSlice? = null
        var morphWeightsBufferSlice: GpuBufferSlice? = null
        var morphTargetIndicesBufferSlice: GpuBufferSlice? = null

        try {
            instanceDataUniformBufferSlice = InstanceDataUniformBuffer.write {
                primitiveSize = scene.primitiveNodes.size
                this.primitiveIndex = primitiveIndex
                this.modelViewMatrices[0] = viewModelMatrix
                lightVector.set(
                    light and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                    (light shr 16) and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F)
                )
                this.lightMapUvs[0] = lightVector
            }
            modelMatricesBufferSlice = device.shaderDataPool.upload(modelMatricesBuffer.buffer)
            skinBuffer?.let { skinBuffer ->
                skinModelIndicesBufferSlice = SkinModelIndicesUniformBuffer.write {
                    skinJoints = skinBuffer.jointSize
                }
                skinJointBufferSlice = device.shaderDataPool.upload(skinBuffer.buffer)
            }
            targetBuffer?.let { targetBuffer ->
                targets?.let { targets ->
                    morphDataUniformBufferSlice = MorphDataUniformBuffer.write {
                        totalVertices = vertexBuffer.verticesCount
                        posTargets = targets.position.targetsCount
                        colorTargets = targets.color.targetsCount
                        texCoordTargets = targets.texCoord.targetsCount
                        totalTargets =
                            targets.position.targetsCount + targets.color.targetsCount + targets.texCoord.targetsCount
                    }
                }
                morphWeightsBufferSlice = device.shaderDataPool.upload(targetBuffer.weightsBuffer)
                morphTargetIndicesBufferSlice = device.shaderDataPool.upload(targetBuffer.indicesBuffer)
            }

            renderPass = material.setup {
                commandEncoder.createRenderPass(
                    { "BlazeRod render pass (non-instanced)" },
                    mainColorTextureView,
                    OptionalInt.empty(),
                    mainDepthTextureView,
                    OptionalDouble.empty()
                )
            }

            with(renderPass) {
                if (RenderPassImpl.IS_DEVELOPMENT) {
                    require(material.skinned == (skinBuffer != null)) {
                        "Primitive's skin data ${skinBuffer != null} and material skinned ${material.skinned} not matching"
                    }
                }
                setUniform("InstanceData", instanceDataUniformBufferSlice)
                setUniform("LocalMatrices", modelMatricesBufferSlice)
                skinJointBufferSlice?.let { skinJointBuffer ->
                    setUniform("Joints", skinJointBuffer)
                }
                skinModelIndicesBufferSlice?.let { skinModelIndices ->
                    setUniform("SkinModelIndices", skinModelIndices)
                }
                morphDataUniformBufferSlice?.let { morphDataUniformBuffer ->
                    setUniform("MorphData", morphDataUniformBuffer)
                }
                morphWeightsBufferSlice?.let { morphWeightsBuffer ->
                    setUniform("MorphWeights", morphWeightsBuffer)
                }
                morphTargetIndicesBufferSlice?.let { morphTargetIndicesBuffer ->
                    setUniform("MorphTargetIndices", morphTargetIndicesBuffer)
                }
                setVertexBuffer(vertexBuffer)
                targets?.let { targets ->
                    bindMorphTargets(targets)
                }
                indexBuffer?.let { indices ->
                    setIndexBuffer(indices)
                    drawIndexed(0, 0, indices.length, 1)
                } ?: run {
                    draw(0, vertexBuffer.verticesCount)
                }
            }
        } finally {
            renderPass?.close()
        }
    }

    fun renderInstanced(
        tasks: List<RenderTask>,
        node: RenderNode.Primitive,
    ) {
        require(material.supportInstancing) { "Primitives which cannot be instanced were scheduled" }

        val mainColorTextureView: GpuTextureView = MinecraftClient.getInstance().framebuffer.colorAttachmentView!!
        val mainDepthTextureView: GpuTextureView? = MinecraftClient.getInstance().framebuffer.depthAttachmentView
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()
        var renderPass: RenderPass? = null
        val instanceDataUniformBufferSlice: GpuBufferSlice
        val modelMatricesBufferSlice: GpuBufferSlice
        var skinModelIndicesBufferSlice: GpuBufferSlice? = null
        var skinJointBufferSlice: GpuBufferSlice? = null
        var morphDataUniformBufferSlice: GpuBufferSlice? = null
        var morphWeightsBufferSlice: GpuBufferSlice? = null
        var morphTargetIndicesBufferSlice: GpuBufferSlice? = null

        val firstTask = tasks.first()
        val firstInstance = firstTask.instance
        val scene = firstInstance.scene
        try {
            instanceDataUniformBufferSlice = InstanceDataUniformBuffer.write {
                primitiveSize = scene.primitiveNodes.size
                this.primitiveIndex = node.primitiveIndex
                for ((index, task) in tasks.withIndex()) {
                    val light = task.light
                    lightVector.set(
                        light and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                        (light shr 16) and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F)
                    )
                    this.lightMapUvs[index] = lightVector
                    this.modelViewMatrices[index] = task.modelViewMatrix
                }
            }
            modelMatricesBufferSlice = device.shaderDataPool.upload(tasks.map { it.modelMatricesBuffer.content.buffer })
            node.skinIndex?.let { skinIndex ->
                val firstSkinBuffer = firstTask.skinBuffer[skinIndex].content
                skinModelIndicesBufferSlice = SkinModelIndicesUniformBuffer.write {
                    skinJoints = firstSkinBuffer.jointSize
                }
                skinJointBufferSlice = device.shaderDataPool.upload(tasks.map { it.skinBuffer[skinIndex].content.buffer })
            }
            node.morphedPrimitiveIndex?.let { morphedPrimitiveIndex ->
                val targets = targets ?: error("Morphed primitive index was set but targets were not")
                morphDataUniformBufferSlice = MorphDataUniformBuffer.write {
                    totalVertices = vertexBuffer.verticesCount
                    posTargets = targets.position.targetsCount
                    colorTargets = targets.color.targetsCount
                    texCoordTargets = targets.texCoord.targetsCount
                    totalTargets =
                        targets.position.targetsCount + targets.color.targetsCount + targets.texCoord.targetsCount
                }
                morphWeightsBufferSlice = device.shaderDataPool.upload(tasks.map { it.morphTargetBuffer[morphedPrimitiveIndex].content.weightsBuffer })
                morphTargetIndicesBufferSlice = device.shaderDataPool.upload(tasks.map { it.morphTargetBuffer[morphedPrimitiveIndex].content.indicesBuffer })
            }

            renderPass = material.setup(true) {
                commandEncoder.createRenderPass(
                    { "BlazeRod render pass (instanced)" },
                    mainColorTextureView,
                    OptionalInt.empty(),
                    mainDepthTextureView,
                    OptionalDouble.empty()
                )
            }

            with(renderPass) {
                if (RenderPassImpl.IS_DEVELOPMENT) {
                    require(material.skinned == (node.skinIndex != null)) {
                        "Primitive's skin data and material skinned property not matching"
                    }
                }
                setUniform("InstanceData", instanceDataUniformBufferSlice)
                setUniform("LocalMatrices", modelMatricesBufferSlice)
                skinJointBufferSlice?.let { skinJointBuffer ->
                    setUniform("Joints", skinJointBuffer)
                }
                skinModelIndicesBufferSlice?.let { skinModelIndices ->
                    setUniform("SkinModelIndices", skinModelIndices)
                }
                morphDataUniformBufferSlice?.let { morphDataUniformBuffer ->
                    setUniform("MorphData", morphDataUniformBuffer)
                }
                morphWeightsBufferSlice?.let { morphWeightsBuffer ->
                    setUniform("MorphWeights", morphWeightsBuffer)
                }
                morphTargetIndicesBufferSlice?.let { morphTargetIndicesBuffer ->
                    setUniform("MorphTargetIndices", morphTargetIndicesBuffer)
                }
                setVertexBuffer(vertexBuffer)
                targets?.let { targets ->
                    bindMorphTargets(targets)
                }
                indexBuffer?.let { indices ->
                    setIndexBuffer(indices)
                    drawIndexed(0, 0, indices.length, tasks.size)
                } ?: run {
                    draw(0, 0, vertexBuffer.verticesCount, tasks.size)
                }
            }
        } finally {
            renderPass?.close()
        }
    }

    override fun onClosed() {
        vertexBuffer.decreaseReferenceCount()
        indexBuffer?.decreaseReferenceCount()
        material.decreaseReferenceCount()
        targets?.apply {
            position.close()
            color.close()
            texCoord.close()
        }
    }
}
