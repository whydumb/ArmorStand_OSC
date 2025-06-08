package top.fifthlight.blazerod.model

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.util.Identifier
import org.joml.Matrix4fc
import org.joml.Vector2i
import top.fifthlight.blazerod.extension.setVertexBuffer
import top.fifthlight.blazerod.extension.draw
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.model.data.RenderTargetBuffer
import top.fifthlight.blazerod.model.uniform.InstanceDataUniformBuffer
import top.fifthlight.blazerod.model.uniform.MorphDataUniformBuffer
import top.fifthlight.blazerod.model.uniform.MorphModelIndicesUniformBuffer
import top.fifthlight.blazerod.model.uniform.SkinModelIndicesUniformBuffer
import top.fifthlight.blazerod.model.uniform.UniformBuffer
import top.fifthlight.blazerod.render.IndexBuffer
import top.fifthlight.blazerod.render.VertexBuffer
import top.fifthlight.blazerod.render.setIndexBuffer
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.SlottedGpuBuffer
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
        instance: ModelInstance,
        primitiveIndex: Int,
        viewModelMatrix: Matrix4fc,
        light: Int,
        skinBuffer: RenderSkinBuffer?,
        targetBuffer: RenderTargetBuffer?,
    ) {
        val mainColorTextureView: GpuTextureView = MinecraftClient.getInstance().framebuffer.colorAttachmentView!!
        val mainDepthTextureView: GpuTextureView? = MinecraftClient.getInstance().framebuffer.depthAttachmentView
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()
        var renderPass: RenderPass? = null
        var materialUniform: UniformBuffer<*, *>? = null
        var instanceDataUniformBuffer: InstanceDataUniformBuffer? = null
        var localMatricsBuffer: GpuBuffer? = null
        var skinModelIndices: SkinModelIndicesUniformBuffer? = null
        var skinJointBuffer: GpuBuffer? = null
        var morphDataUniformBuffer: MorphDataUniformBuffer? = null
        var morphModelIndices: MorphModelIndicesUniformBuffer? = null
        var morphWeightsBuffer: GpuBuffer? = null
        var morphTargetIndicesBuffer: GpuBuffer? = null

        try {
            instanceDataUniformBuffer = InstanceDataUniformBuffer.Companion.acquire()
            instanceDataUniformBuffer.write {
                primitiveSize = instance.scene.primitiveNodes.size
                this.primitiveIndex = primitiveIndex
                this.modelViewMatrices[0] = viewModelMatrix
                lightVector.set(
                    light and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                    (light shr 16) and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F)
                )
                this.lightMapUvs[0] = lightVector
                when (val slot = instance.modelData.modelMatricesBuffer.slot) {
                    is SlottedGpuBuffer.Slotted -> {
                        localMatricesIndices.set(0, slot.index)
                        localMatricsBuffer = slot.buffer.getBuffer()
                    }

                    is SlottedGpuBuffer.Unslotted -> {
                        localMatricesIndices.set(0, 0)
                        localMatricsBuffer = slot.getBuffer()
                    }
                }
            }
            skinBuffer?.let { skinBuffer ->
                skinModelIndices = SkinModelIndicesUniformBuffer.Companion.acquire()
                val slot = skinBuffer.slot
                when (slot) {
                    is SlottedGpuBuffer.Slotted -> {
                        skinModelIndices.write {
                            skinJoints = skinBuffer.skin.jointSize
                            skinModelOffsets.set(0, slot.index)
                        }
                        skinJointBuffer = slot.buffer.getBuffer()
                    }

                    is SlottedGpuBuffer.Unslotted -> {
                        skinModelIndices.write {
                            skinJoints = skinBuffer.skin.jointSize
                            skinModelOffsets.set(0, 0)
                        }
                        skinJointBuffer = slot.getBuffer()
                    }
                }
            }
            targetBuffer?.let { targetBuffer ->
                targets?.let { targets ->
                    morphDataUniformBuffer = MorphDataUniformBuffer.Companion.acquire()
                    morphDataUniformBuffer.write {
                        totalVertices = vertexBuffer.verticesCount
                        posTargets = targets.position.targetsCount
                        colorTargets = targets.color.targetsCount
                        texCoordTargets = targets.texCoord.targetsCount
                        totalTargets =
                            targets.position.targetsCount + targets.color.targetsCount + targets.texCoord.targetsCount
                    }
                }
                targetBuffer.uploadIndices()
                morphModelIndices = MorphModelIndicesUniformBuffer.Companion.acquire()
                morphModelIndices.write {
                    when (val slot = targetBuffer.weightsSlot) {
                        is SlottedGpuBuffer.Slotted -> {
                            morphWeightIndices.set(0, slot.index)
                            morphWeightsBuffer = slot.buffer.getBuffer()
                        }

                        is SlottedGpuBuffer.Unslotted -> {
                            morphWeightIndices.set(0, 0)
                            morphWeightsBuffer = slot.getBuffer()
                        }
                    }
                    when (val slot = targetBuffer.indicesSlot) {
                        is SlottedGpuBuffer.Slotted -> {
                            morphIndexIndices.set(0, slot.index)
                            morphTargetIndicesBuffer = slot.buffer.getBuffer()
                        }

                        is SlottedGpuBuffer.Unslotted -> {
                            morphIndexIndices.set(0, 0)
                            morphTargetIndicesBuffer = slot.getBuffer()
                        }
                    }
                }
            }

            val setupResult = material.setup {
                commandEncoder.createRenderPass(
                    { "BlazeRod render pass (non-instanced)" },
                    mainColorTextureView,
                    OptionalInt.empty(),
                    mainDepthTextureView,
                    OptionalDouble.empty()
                )
            }
            renderPass = setupResult.first
            materialUniform = setupResult.second

            with(renderPass) {
                if (RenderPassImpl.IS_DEVELOPMENT) {
                    require(material.skinned == (skinBuffer != null)) {
                        "Primitive's skin data ${skinBuffer != null} and material skinned ${material.skinned} not matching"
                    }
                }
                setUniform("InstanceData", instanceDataUniformBuffer.slice)
                setUniform("LocalMatrices", localMatricsBuffer)
                skinJointBuffer?.let { skinJointBuffer ->
                    setUniform("Joints", skinJointBuffer)
                }
                skinModelIndices?.let { skinModelIndices ->
                    setUniform("SkinModelIndices", skinModelIndices.slice)
                }
                morphModelIndices?.let { morphModelIndices ->
                    setUniform("MorphModelIndices", morphModelIndices.slice)
                }
                morphDataUniformBuffer?.let { morphDataUniformBuffer ->
                    setUniform("MorphData", morphDataUniformBuffer.slice)
                }
                morphWeightsBuffer?.let { morphWeightsBuffer ->
                    setUniform("MorphWeights", morphWeightsBuffer)
                }
                morphTargetIndicesBuffer?.let { morphTargetIndicesBuffer ->
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
            materialUniform?.close()
            instanceDataUniformBuffer?.close()
            skinModelIndices?.close()
            morphDataUniformBuffer?.close()
            morphModelIndices?.close()
        }
    }

    fun renderInstanced(
        tasks: List<RenderTask.Instance>,
        node: RenderNode.Primitive,
    ) {
        require(material.supportInstancing) { "Primitives which cannot be instanced were scheduled" }

        val mainColorTextureView: GpuTextureView = MinecraftClient.getInstance().framebuffer.colorAttachmentView!!
        val mainDepthTextureView: GpuTextureView? = MinecraftClient.getInstance().framebuffer.depthAttachmentView
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()
        var renderPass: RenderPass? = null
        var materialUniform: UniformBuffer<*, *>? = null
        var instanceDataUniformBuffer: InstanceDataUniformBuffer? = null
        var localMatricsBuffer: GpuBuffer? = null
        var skinModelIndices: SkinModelIndicesUniformBuffer? = null
        var skinJointBuffer: GpuBuffer? = null
        var morphDataUniformBuffer: MorphDataUniformBuffer? = null
        var morphModelIndices: MorphModelIndicesUniformBuffer? = null
        var morphWeightsBuffer: GpuBuffer? = null
        var morphTargetIndicesBuffer: GpuBuffer? = null

        val firstInstance = tasks.first().instance

        fun SlottedGpuBuffer.Slot.asSlotted() = this as SlottedGpuBuffer.Slotted

        try {
            instanceDataUniformBuffer = InstanceDataUniformBuffer.Companion.acquire()
            instanceDataUniformBuffer.write {
                primitiveSize = firstInstance.scene.primitiveNodes.size
                this.primitiveIndex = node.primitiveIndex
                for ((index, task) in tasks.withIndex()) {
                    val light = task.light
                    lightVector.set(
                        light and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F),
                        (light shr 16) and (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or 0xFF0F)
                    )
                    this.lightMapUvs[index] = lightVector
                    this.modelViewMatrices[index] = task.modelViewMatrix
                    val slot = task.instance.modelData.modelMatricesBuffer.slot.asSlotted()
                    localMatricesIndices.set(index, slot.index)
                }
                localMatricsBuffer = firstInstance.modelData.modelMatricesBuffer.slot.asSlotted().buffer.getBuffer()
            }
            node.skinIndex?.let { skinIndex ->
                skinModelIndices = SkinModelIndicesUniformBuffer.Companion.acquire()
                skinModelIndices.write {
                    for ((index, task) in tasks.withIndex()) {
                        val skinBuffer = task.instance.modelData.skinBuffers[skinIndex]
                        val slot = skinBuffer.slot
                        skinJoints = skinBuffer.skin.jointSize
                        skinModelOffsets.set(index, slot.asSlotted().index)
                    }
                }
                skinJointBuffer = firstInstance.modelData.skinBuffers[skinIndex].slot.asSlotted().buffer.getBuffer()
            }
            node.morphedPrimitiveIndex?.let { weightsIndex ->
                targets?.let { targets ->
                    morphDataUniformBuffer = MorphDataUniformBuffer.Companion.acquire()
                    morphDataUniformBuffer.write {
                        totalVertices = vertexBuffer.verticesCount
                        posTargets = targets.position.targetsCount
                        colorTargets = targets.color.targetsCount
                        texCoordTargets = targets.texCoord.targetsCount
                        totalTargets =
                            targets.position.targetsCount + targets.color.targetsCount + targets.texCoord.targetsCount
                    }
                }
                morphModelIndices = MorphModelIndicesUniformBuffer.Companion.acquire()
                morphModelIndices.write {
                    for ((index, task) in tasks.withIndex()) {
                        val targetBuffer = task.instance.modelData.targetBuffers[weightsIndex]
                        targetBuffer.uploadIndices()
                        val weightsSlot = targetBuffer.weightsSlot.asSlotted()
                        val indicesSlot = targetBuffer.indicesSlot.asSlotted()
                        morphWeightIndices.set(index, weightsSlot.index)
                        morphWeightsBuffer = weightsSlot.buffer.getBuffer()
                        morphIndexIndices.set(index, indicesSlot.index)
                        morphTargetIndicesBuffer = indicesSlot.buffer.getBuffer()
                    }
                }
            }

            val setupResult = material.setup(true) {
                commandEncoder.createRenderPass(
                    { "BlazeRod render pass (instanced)" },
                    mainColorTextureView,
                    OptionalInt.empty(),
                    mainDepthTextureView,
                    OptionalDouble.empty()
                )
            }
            renderPass = setupResult.first
            materialUniform = setupResult.second

            with(renderPass) {
                if (RenderPassImpl.IS_DEVELOPMENT) {
                    require(material.skinned == (node.skinIndex != null)) {
                        "Primitive's skin data and material skinned property not matching"
                    }
                }
                setUniform("InstanceData", instanceDataUniformBuffer.slice)
                setUniform("LocalMatrices", localMatricsBuffer)
                skinJointBuffer?.let { skinJointBuffer ->
                    setUniform("Joints", skinJointBuffer)
                }
                skinModelIndices?.let { skinModelIndices ->
                    setUniform("SkinModelIndices", skinModelIndices.slice)
                }
                morphModelIndices?.let { morphModelIndices ->
                    setUniform("MorphModelIndices", morphModelIndices.slice)
                }
                morphDataUniformBuffer?.let { morphDataUniformBuffer ->
                    setUniform("MorphData", morphDataUniformBuffer.slice)
                }
                morphWeightsBuffer?.let { morphWeightsBuffer ->
                    setUniform("MorphWeights", morphWeightsBuffer)
                }
                morphTargetIndicesBuffer?.let { morphTargetIndicesBuffer ->
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
            materialUniform?.close()
            instanceDataUniformBuffer?.close()
            skinModelIndices?.close()
            morphDataUniformBuffer?.close()
            morphModelIndices?.close()
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
