package top.fifthlight.armorstand.util

import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.withContext

suspend inline fun <T> withRenderDevice(crossinline block: suspend (GpuDevice) -> T): T = withContext(ClientThreadDispatcher) {
    block(RenderSystem.getDevice())
}