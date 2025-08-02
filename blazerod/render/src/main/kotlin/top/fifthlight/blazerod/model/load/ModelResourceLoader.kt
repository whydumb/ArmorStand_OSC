package top.fifthlight.blazerod.model.load

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.lwjgl.opengl.GL11C
import top.fifthlight.blazerod.model.resource.RenderTexture
import top.fifthlight.blazerod.render.GpuIndexBuffer
import top.fifthlight.blazerod.render.RefCountedGpuBuffer
import top.fifthlight.blazerod.util.blaze3d
import top.fifthlight.blazerod.util.useMipmap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object ModelResourceLoader {
    private fun <T, R> Deferred<T>.map(
        scope: CoroutineScope,
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend (T) -> R,
    ): Deferred<R> = scope.async(context) {
        block(await())
    }

    private fun <T, R> Iterable<Deferred<T>>.mapAll(
        scope: CoroutineScope,
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend (T) -> R,
    ): List<Deferred<R>> = map {
        it.map(scope, context, block)
    }

    private inline fun wrapWithGlErrorCheck(crossinline block: () -> Unit) {
        GlStateManager.clearGlErrors()
        block()
        val error = GlStateManager._getError()
        if (error != GL11C.GL_NO_ERROR) {
            error("OpenGL error: 0x${error.toUInt().toString(16).padStart(8, '0')}")
        }
    }

    fun load(scope: CoroutineScope, dispatcher: CoroutineDispatcher, info: PreProcessModelLoadInfo) =
        GpuLoadModelLoadInfo(
            textures = info.textures.mapAll(scope, dispatcher) { info ->
                val info = info ?: return@mapAll null
                info.use { info ->
                    val (name, nativeImage, sampler) = info
                    RenderSystem.getDevice().let { device ->
                        val gpuTexture = device.createTexture(
                            name,
                            GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST,
                            TextureFormat.RGBA8,
                            nativeImage.width,
                            nativeImage.height,
                            1,
                            1
                        )
                        gpuTexture.setAddressMode(sampler.wrapS.blaze3d, sampler.wrapT.blaze3d)
                        gpuTexture.setTextureFilter(
                            sampler.minFilter.blaze3d,
                            sampler.magFilter.blaze3d,
                            sampler.minFilter.useMipmap,
                        )
                        wrapWithGlErrorCheck {
                            device.createCommandEncoder().writeToTexture(gpuTexture, nativeImage)
                        }
                        val textureView = device.createTextureView(gpuTexture)
                        RenderTexture(gpuTexture, textureView)
                    }
                }
            },
            indexBuffers = info.indexBuffers.mapAll(scope, dispatcher) { indexData ->
                val buffer = RefCountedGpuBuffer(
                    RenderSystem.getDevice().createBuffer(null, GpuBuffer.USAGE_INDEX, indexData.buffer)
                )
                GpuIndexBuffer(
                    type = indexData.type,
                    length = indexData.length,
                    buffer = buffer,
                )
            },
            vertexBuffers = info.vertexBuffers.mapAll(scope, dispatcher) {
                RefCountedGpuBuffer(RenderSystem.getDevice().createBuffer(null, GpuBuffer.USAGE_VERTEX, it))
            },
            primitiveInfos = info.primitiveInfos,
            rootNodeIndex = info.rootNodeIndex,
            nodes = info.nodes,
        )
}