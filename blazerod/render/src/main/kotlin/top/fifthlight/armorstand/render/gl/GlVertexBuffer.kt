package top.fifthlight.armorstand.render.gl

import com.mojang.blaze3d.opengl.GlConst
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.GlGpuBuffer
import top.fifthlight.armorstand.helper.gl.GlStateManagerHelper
import top.fifthlight.armorstand.render.VertexBuffer

class GlVertexBuffer(
    override val mode: VertexFormat.DrawMode,
    override val elements: List<VertexElement>,
    override val verticesCount: Int,
): VertexBuffer() {
    val vaoId: Int

    init {
        RenderSystem.assertOnRenderThread()
        vaoId = createVao()
        elements.forEach { it.increaseReferenceCount() }
    }

    private fun createVao(): Int {
        val vaoId = GlStateManager._glGenVertexArrays()
        GlStateManager._glBindVertexArray(vaoId)
        elements.forEachIndexed { index, element ->
            val bufferId = (element.buffer.inner as GlGpuBuffer).id
            GlStateManager._glBindBuffer(GlConst.GL_ARRAY_BUFFER, bufferId)
            if (element.usage.useAsInteger) {
                GlStateManager._vertexAttribIPointer(
                    index,
                    element.componentType.components,
                    GlConst.toGl(element.type),
                    element.stride,
                    element.offset.toLong()
                )
            } else {
                GlStateManager._vertexAttribPointer(
                    index,
                    element.componentType.components,
                    GlConst.toGl(element.type),
                    element.normalized,
                    element.stride,
                    element.offset.toLong()
                )
            }
            GlStateManager._enableVertexAttribArray(index)
        }
        GlStateManager._glBindVertexArray(0)
        return vaoId
    }

    override fun onClosed() {
        elements.forEach { it.decreaseReferenceCount() }
        GlStateManagerHelper._glDeleteVertexArrays(vaoId)
    }
}