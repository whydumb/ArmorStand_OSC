package top.fifthlight.blazerod.model.load

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import kotlinx.coroutines.*
import top.fifthlight.blazerod.extension.GpuBufferExt
import top.fifthlight.blazerod.extension.createBuffer
import top.fifthlight.blazerod.model.resource.RenderPrimitive
import top.fifthlight.blazerod.model.resource.RenderTexture
import top.fifthlight.blazerod.render.GpuIndexBuffer
import top.fifthlight.blazerod.render.RefCountedGpuBuffer
import top.fifthlight.blazerod.util.blaze3d
import top.fifthlight.blazerod.util.useMipmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

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

    private fun CoroutineScope.awaitFence(context: CoroutineContext = EmptyCoroutineContext) = async(context) {
        suspendCancellableCoroutine { continuation ->
            RenderSystem.queueFencedTask {
                continuation.resume(Unit)
            }
        }
    }

    private suspend inline fun <T> CoroutineScope.awaitFence(
        context: CoroutineContext = EmptyCoroutineContext,
        crossinline block: suspend () -> T,
    ): T {
        val fence = awaitFence(context)
        val result = block()
        fence.await()
        return result
    }

    fun load(
        scope: CoroutineScope,
        loadDispatcher: CoroutineDispatcher,
        gpuDispatcher: CoroutineDispatcher,
        info: PreProcessModelLoadInfo,
    ): GpuLoadModelLoadInfo {
        val textures = info.textures.mapAll(scope, gpuDispatcher) { info ->
            val info = info ?: return@mapAll null
            info.use { info ->
                val (name, nativeImage, sampler) = info
                val device = RenderSystem.getDevice()
                val commandEncoder = device.createCommandEncoder()
                val gpuTexture: GpuTexture
                nativeImage.use {
                    gpuTexture = device.createTexture(
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
                    scope.awaitFence(gpuDispatcher) {
                        commandEncoder.writeToTexture(gpuTexture, nativeImage)
                    }
                }
                val textureView = device.createTextureView(gpuTexture)
                RenderTexture(gpuTexture, textureView)
            }
        }
        val indexBuffers = info.indexBuffers.mapAll(scope, gpuDispatcher) { indexData ->
            val device = RenderSystem.getDevice()
            val commandEncoder = device.createCommandEncoder()
            val buffer = RefCountedGpuBuffer(
                device.createBuffer(
                    null,
                    GpuBuffer.USAGE_MAP_WRITE or GpuBuffer.USAGE_INDEX,
                    indexData.buffer.remaining()
                )
            )
            commandEncoder.mapBuffer(buffer.inner, false, true).use { mapped ->
                val data = mapped.data()
                withContext(loadDispatcher) {
                    data.put(indexData.buffer)
                }
            }
            GpuIndexBuffer(
                type = indexData.type,
                length = indexData.length,
                buffer = buffer,
            )
        }
        val vertexBuffers = info.vertexBuffers.mapAll(scope, gpuDispatcher) {
            val device = RenderSystem.getDevice()
            val commandEncoder = device.createCommandEncoder()
            val buffer = RefCountedGpuBuffer(
                device.createBuffer(null, GpuBuffer.USAGE_MAP_WRITE or GpuBuffer.USAGE_VERTEX, it.remaining())
            )
            scope.awaitFence(gpuDispatcher) {
                commandEncoder.mapBuffer(buffer.inner, false, true).use { mapped ->
                    val data = mapped.data()
                    withContext(loadDispatcher) {
                        data.put(it)
                    }
                }
            }
            GpuLoadVertexData(
                gpuBuffer = buffer,
                cpuBuffer = it,
            )
        }
        val morphTargetInfos = info.morphTargetInfos.mapAll(scope, gpuDispatcher) {
            suspend fun loadTarget(target: MorphTargetsLoadData.TargetInfo): RenderPrimitive.Target {
                val targetBuffer = if (target.targetsCount == 0) {
                    // No targets, but we can't create an empty buffer, so let's create a dummy one
                    ByteBuffer.allocateDirect(target.itemStride).order(ByteOrder.nativeOrder())
                } else {
                    target.buffer
                }
                val device = RenderSystem.getDevice()
                val commandEncoder = device.createCommandEncoder()
                val gpuBuffer = device.createBuffer(
                    labelGetter = { "Morph target buffer" },
                    usage = GpuBuffer.USAGE_MAP_WRITE or GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER,
                    extraUsage = GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER,
                    size = targetBuffer.remaining(),
                )
                scope.awaitFence(gpuDispatcher) {
                    commandEncoder.mapBuffer(gpuBuffer, false, true).use { mapped ->
                        val data = mapped.data()
                        withContext(loadDispatcher) {
                            data.put(targetBuffer)
                        }
                    }
                }
                return RenderPrimitive.Target(
                    gpuBuffer = gpuBuffer,
                    cpuBuffer = targetBuffer,
                    targetsCount = target.targetsCount,
                )
            }
            MorphTargetsLoadData(
                targetGroups = it.targetGroups,
                position = loadTarget(it.position),
                color = loadTarget(it.color),
                texCoord = loadTarget(it.texCoord),
            )
        }
        return GpuLoadModelLoadInfo(
            textures = textures,
            indexBuffers = indexBuffers,
            vertexBuffers = vertexBuffers,
            morphTargetInfos = morphTargetInfos,
            primitiveInfos = info.primitiveInfos,
            rootNodeIndex = info.rootNodeIndex,
            nodes = info.nodes,
            skins = info.skins,
            expressions = info.expressions,
            expressionGroups = info.expressionGroups,
        )
    }
}