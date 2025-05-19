package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.util.Identifier
import org.joml.Matrix4fc
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.VertexBuffer
import top.fifthlight.armorstand.render.setIndexBuffer
import top.fifthlight.armorstand.util.AbstractRefCount
import top.fifthlight.armorstand.extension.bindSampler
import top.fifthlight.armorstand.extension.drawIndexedInstanced
import top.fifthlight.armorstand.extension.drawInstanced
import top.fifthlight.armorstand.extension.setUniform
import top.fifthlight.armorstand.extension.setVertexBuffer
import top.fifthlight.armorstand.render.GpuTextureBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class RenderPrimitive(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer?,
    val material: RenderMaterial<*>,
    val targets: Targets?,
    val targetGroups: List<MorphTargetGroup>,
) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("armorstand", "primitive")
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
        val data: GpuTextureBuffer,
        val targetsCount: Int,
    ): AutoCloseable by data

    class Targets(
        val position: Target,
        val color: Target,
        val texCoord: Target,
    )

    class TargetWeight(val size: Int) {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder())
        private val floatBuffer = buffer.asFloatBuffer()
        val enabledIndices = IntOpenHashSet(size)

        operator fun set(index: Int, value: Float) {
            if (value == 0f) {
                enabledIndices.remove(index)
            } else {
                enabledIndices.add(index)
            }
            floatBuffer.put(index, value)
        }

        operator fun get(index: Int) = floatBuffer.get(index)
    }

    class TargetWeights(
        val position: TargetWeight,
        val color: TargetWeight,
        val texCoord: TargetWeight,
    )

    private fun RenderPass.bindMorphTargets(targets: Targets) {
        setUniform("TotalVertices", vertexBuffer.verticesCount)
        bindSampler("MorphPositionData", targets.position.data)
        bindSampler("MorphColorData", targets.color.data)
        bindSampler("MorphTexCoordData", targets.texCoord.data)
        setUniform("MorphTargetSizes", targets.position.targetsCount, targets.color.targetsCount, targets.position.targetsCount + targets.color.targetsCount + targets.texCoord.targetsCount)
    }

    fun render(matrix: Matrix4fc, light: Int, skin: RenderSkinData?, targetWeights: TargetWeights?) {
        val mainColorTexture: GpuTexture = MinecraftClient.getInstance().framebuffer.colorAttachment!!
        val mainDepthTexture: GpuTexture? = MinecraftClient.getInstance().framebuffer.depthAttachment
        val viewStack = RenderSystem.getModelViewStack()
        viewStack.pushMatrix()
        viewStack.mul(matrix)
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()
        var renderPass: RenderPass? = null

        var weightsBuffer: TargetWeightsBuffer? = null
        var indicesBuffer: TargetIndicesBuffer? = null

        try {
            weightsBuffer = targetWeights?.let { targetWeights ->
                TargetWeightsBuffer.acquire().also { weightsBuffer ->
                    weightsBuffer.upload(device, commandEncoder, listOf(targetWeights))
                }
            }
            indicesBuffer = targetWeights?.let { targetWeights ->
                TargetIndicesBuffer.acquire().also { indicesBuffer ->
                    indicesBuffer.upload(device, commandEncoder, listOf(targetWeights))
                }
            }

            renderPass = commandEncoder.createRenderPass(mainColorTexture, OptionalInt.empty(), mainDepthTexture, OptionalDouble.empty())
            with(renderPass) {
                material.setup(this, light)
                if (RenderPassImpl.IS_DEVELOPMENT) {
                    require(material.skinned == (skin != null)) {
                        "Primitive's skin data ${skin != null} and material skinned ${material.skinned} not matching"
                    }
                }
                skin?.getBuffer()?.let { skinBuffer ->
                    bindSampler("Joints", skinBuffer)
                }
                setVertexBuffer(vertexBuffer)
                targets?.let { targets ->
                    bindMorphTargets(targets)
                    bindSampler("MorphWeights", weightsBuffer!!.getBuffer())
                    setUniform("MorphIndices", indicesBuffer!!.getBuffer())
                }
                indexBuffer?.let { indices ->
                    setIndexBuffer(indices)
                    drawIndexed(0, indices.length)
                } ?: run {
                    draw(0, vertexBuffer.verticesCount)
                }
            }
        } finally {
            weightsBuffer?.close()
            indicesBuffer?.close()
            renderPass?.close()
        }
        viewStack.popMatrix()
    }

    fun renderInstanced(tasks: List<RenderTask.Primitive>) {
        require(material.supportInstancing) { "Primitives which cannot be instanced were scheduled" }

        when (tasks.size) {
            0 -> return
            1 -> {
                val task = tasks.first()
                render(task.modelViewMatrix, task.light, task.skinData, task.targetWeights)
                return
            }
        }

        val mainColorTexture: GpuTexture = MinecraftClient.getInstance().framebuffer.colorAttachment!!
        val mainDepthTexture: GpuTexture? = MinecraftClient.getInstance().framebuffer.depthAttachment
        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()

        var renderPass: RenderPass? = null
        var instanceBuffer: InstanceDataBuffer? = null
        var skinData: MergedSkinData? = null
        var weightsBuffer: TargetWeightsBuffer? = null
        var indicesBuffer: TargetIndicesBuffer? = null
        try {
            skinData = if (material.skinned) {
                MergedSkinData.acquire().also { skinData ->
                    skinData.upload(device, commandEncoder, tasks.map { it.skinData!! })
                }
            } else {
                null
            }
            instanceBuffer = InstanceDataBuffer.acquire()
            instanceBuffer.upload(device, commandEncoder, tasks)
            if (material.morphed) {
                val weights = tasks.map { it.targetWeights!! }
                weightsBuffer = TargetWeightsBuffer.acquire().also { weightsBuffer ->
                    weightsBuffer.upload(device, commandEncoder, weights)
                }
                indicesBuffer = TargetIndicesBuffer.acquire().also { indicesBuffer ->
                    indicesBuffer.upload(device, commandEncoder, weights)
                }
            }

            renderPass = commandEncoder.createRenderPass(
                mainColorTexture,
                OptionalInt.empty(),
                mainDepthTexture,
                OptionalDouble.empty()
            )

            with(renderPass) {
                material.setupInstanced(this)
                skinData?.getBuffer()?.let { skinBuffer ->
                    bindSampler("Joints", skinBuffer)
                    if (material.skinned) {
                        setUniform("TotalJoints", tasks.first().skinData!!.skin.jointSize)
                    }
                }
                setUniform("Instances", instanceBuffer.getBuffer())

                setVertexBuffer(vertexBuffer)
                targets?.let { targets ->
                    bindMorphTargets(targets)
                    bindSampler("MorphWeights", weightsBuffer!!.getBuffer())
                    setUniform("MorphIndices", indicesBuffer!!.getBuffer())
                }
                indexBuffer?.let { indices ->
                    setIndexBuffer(indices)
                    drawIndexedInstanced(tasks.size, 0, indices.length)
                } ?: run {
                    drawInstanced(tasks.size, 0, vertexBuffer.verticesCount)
                }
            }
        } finally {
            skinData?.close()
            instanceBuffer?.close()
            weightsBuffer?.close()
            indicesBuffer?.close()
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

    fun schedule(matrix: Matrix4fc, light: Int, skin: RenderSkinData?, targetWeights: TargetWeights?, onTaskScheduled: (RenderTask<*, *>) -> Unit) {
        if (material.supportInstancing) {
            onTaskScheduled(RenderTask.Primitive.acquire().apply {
                primitive = this@RenderPrimitive
                this.skinData = skin
                this.modelViewMatrix.set(matrix)
                this.modelViewProjMatrix.apply {
                    set(RenderSystem.getModelViewStack())
                    mul(matrix)
                    mulLocal(RenderSystem.getProjectionMatrix())
                }
                this.light = light
                this.targetWeights = targetWeights
            })
        } else {
            render(matrix, light, skin, targetWeights)
        }
    }
}
