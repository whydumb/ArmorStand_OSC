package top.fifthlight.armorstand.render.gl

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.opengl.GlConst.GL_STATIC_DRAW
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.DebugLabelManager
import net.minecraft.client.gl.GlGpuBuffer
import org.lwjgl.opengl.ARBClearBufferObject
import org.lwjgl.opengl.GL32C
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryUtil
import top.fifthlight.armorstand.helper.GpuDeviceExt.FillType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.function.Supplier

object FilledVertexElementProvider {
    fun generate(
        labelGetter: Supplier<String>?,
        type: BufferType,
        usage: BufferUsage,
        size: Int,
        fillType: FillType,
    ): GpuBuffer {
        val provider = this.provider ?: error("Please initialize DefaultVertexElementProvider")
        return provider.generate(labelGetter, type, usage, size, fillType)
    }

    interface Provider {
        fun generate(
            labelGetter: Supplier<String>?,
            type: BufferType,
            usage: BufferUsage,
            size: Int,
            fillType: FillType
        ): GpuBuffer
    }

    private var provider: Provider? = null
    private const val USE_ARB_EXTENSION: Boolean = true

    fun init(capabilities: GLCapabilities, labeler: DebugLabelManager, usedCapabilities: MutableSet<String>) {
        if (provider != null) {
            return
        }
        provider = if (capabilities.GL_ARB_clear_buffer_object && USE_ARB_EXTENSION) {
            usedCapabilities.add("GL_ARB_clear_buffer_object")
            ARBBufferProvider(labeler)
        } else {
            DefaultBufferProvider(labeler)
        }
    }

    private class ARBBufferProvider(private val labeler: DebugLabelManager) : Provider {
        override fun generate(
            labelGetter: Supplier<String>?,
            type: BufferType,
            usage: BufferUsage,
            size: Int,
            fillType: FillType,
        ): GpuBuffer = RenderSystem.getDevice().let { device ->
            device.createBuffer(labelGetter, type, usage, size).also { buffer ->
                GlStateManager._glBindBuffer(GL32C.GL_ARRAY_BUFFER, (buffer as GlGpuBuffer).id)
                GlStateManager._glBufferData(GL32C.GL_ARRAY_BUFFER, size.toLong(), GL_STATIC_DRAW)
                when (fillType) {
                    FillType.ZERO_FILLED -> ARBClearBufferObject.glClearBufferData(
                        GL32C.GL_ARRAY_BUFFER,
                        GL32C.GL_R8,
                        GL32C.GL_RED_INTEGER,
                        GL32C.GL_UNSIGNED_BYTE,
                        null as ByteBuffer?,
                    )

                    FillType.BYTE_ONE_FILLED -> ARBClearBufferObject.glClearBufferData(
                        GL32C.GL_ARRAY_BUFFER,
                        GL32C.GL_R8,
                        GL32C.GL_RED_INTEGER,
                        GL32C.GL_UNSIGNED_BYTE,
                        ByteBuffer.allocateDirect(1).apply {
                            put(0xFF.toByte())
                            flip()
                        }
                    )

                    FillType.FLOAT_ONE_FILLED -> ARBClearBufferObject.glClearBufferData(
                        GL32C.GL_ARRAY_BUFFER,
                        GL32C.GL_R32F,
                        GL32C.GL_RED,
                        GL32C.GL_FLOAT,
                        FloatArray(1) { 1f }
                    )
                }
                labeler.labelGlGpuBuffer(buffer)
            }
        }
    }

    private class DefaultBufferProvider(private val labeler: DebugLabelManager) : Provider {
        override fun generate(
            labelGetter: Supplier<String>?,
            type: BufferType,
            usage: BufferUsage,
            size: Int,
            fillType: FillType,
        ): GpuBuffer = RenderSystem.getDevice().let { device ->
            val buffer = ByteBuffer.allocateDirect(size).also { buffer ->
                when (fillType) {
                    FillType.ZERO_FILLED -> {
                        MemoryUtil.memSet(buffer, 0)
                    }

                    FillType.BYTE_ONE_FILLED -> {
                        MemoryUtil.memSet(buffer, 0xFF)
                    }

                    FillType.FLOAT_ONE_FILLED -> {
                        require(size % 4 == 0) { "Bad required byte length: $size" }
                        buffer.order(ByteOrder.nativeOrder())
                        val floatBuffer = buffer.asFloatBuffer()
                        repeat(size / 4) { floatBuffer.put(1f) }
                        floatBuffer.flip()
                    }
                }
            }
            device.createBuffer(labelGetter, type, usage, buffer).also { labeler.labelGlGpuBuffer(it as GlGpuBuffer) }
        }
    }
}