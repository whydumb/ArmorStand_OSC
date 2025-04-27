package top.fifthlight.armorstand.model

import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
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
import java.util.*

class RenderPrimitive(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer?,
    val material: RenderMaterial<*>,
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
    }

    fun render(matrix: Matrix4fc, light: Int, skin: RenderSkinData?) {
        val mainColorTexture: GpuTexture = MinecraftClient.getInstance().framebuffer.colorAttachment!!
        val mainDepthTexture: GpuTexture? = MinecraftClient.getInstance().framebuffer.depthAttachment
        val viewStack = RenderSystem.getModelViewStack()
        viewStack.pushMatrix()
        viewStack.mul(matrix)
        RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(mainColorTexture, OptionalInt.empty(), mainDepthTexture, OptionalDouble.empty())
            .use { renderPass ->
                material.setup(renderPass, light)
                if (RenderPassImpl.IS_DEVELOPMENT) {
                    require(material.skinned == (skin != null)) {
                        "Primitive's skin data ${skin != null} and material skinned ${material.skinned} not matching"
                    }
                }
                skin?.getBuffer()?.let { skinBuffer ->
                    renderPass.bindSampler("Joints", skinBuffer)
                }
                renderPass.setVertexBuffer(vertexBuffer)
                indexBuffer?.let { indices ->
                    renderPass.setIndexBuffer(indices)
                    renderPass.drawIndexed(0, indices.length)
                } ?: run {
                    renderPass.draw(0, vertexBuffer.verticesCount)
                }
            }
        viewStack.popMatrix()
    }

    fun renderInstanced(tasks: List<RenderTask.Primitive>) {
        require(material.supportInstancing) { "Primitives which cannot be instanced were scheduled" }

        when (tasks.size) {
            0 -> return
            1 -> {
                val task = tasks.first()
                render(task.modelViewMatrix, task.light, task.skinData)
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
                        setUniform("ModelJoints", tasks.first().skinData!!.skin.jointSize)
                    }
                }
                setUniform("Instances", instanceBuffer.getBuffer())

                setVertexBuffer(vertexBuffer)
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
            renderPass?.close()
        }
    }

    override fun onClosed() {
        vertexBuffer.decreaseReferenceCount()
        indexBuffer?.decreaseReferenceCount()
        material.decreaseReferenceCount()
    }

    fun schedule(matrix: Matrix4fc, light: Int, skin: RenderSkinData?, onTaskScheduled: (RenderTask<*, *>) -> Unit) {
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
            })
        } else {
            render(matrix, light, skin)
        }
    }
}
