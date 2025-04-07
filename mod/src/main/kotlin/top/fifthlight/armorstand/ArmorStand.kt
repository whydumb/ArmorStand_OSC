package top.fifthlight.armorstand

import kotlinx.coroutines.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.SharedConstants
import net.minecraft.client.gl.RenderPassImpl
import net.minecraft.client.gl.RenderPipelines
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.model.RenderMaterial
import top.fifthlight.armorstand.state.PlayerModelManager
import top.fifthlight.armorstand.util.ClientThreadDispatcher
import kotlin.coroutines.CoroutineContext

object ArmorStand : ClientModInitializer {
    val scope by lazy { CoroutineScope(SupervisorJob() + ClientThreadDispatcher) }

    override fun onInitializeClient() {
        if (System.getProperty("armorstand.debug") == "true") {
            RenderPassImpl.IS_DEVELOPMENT = true
        }

        RenderMaterial.PIPELINES.forEach(RenderPipelines::register)

        ConfigHolder.read()
        ClientLifecycleEvents.CLIENT_STARTED.register { client ->
            PlayerModelManager.initialize()
        }
    }
}
