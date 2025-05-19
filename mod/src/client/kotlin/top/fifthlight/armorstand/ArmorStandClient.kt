package top.fifthlight.armorstand

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPassImpl
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.debug.ModelManagerDebugFrame
import top.fifthlight.armorstand.debug.ObjectCountTrackerFrame
import top.fifthlight.armorstand.debug.ObjectPoolTracker
import top.fifthlight.armorstand.debug.ResourceCountTracker
import top.fifthlight.armorstand.debug.ResourceCountTrackerFrame
import top.fifthlight.armorstand.model.RenderMaterial
import top.fifthlight.armorstand.network.PlayerModelUpdateS2CPayload
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.armorstand.state.NetworkModelSyncer
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher
import javax.swing.SwingUtilities

object ArmorStandClient : ArmorStand(), ClientModInitializer {
    private val LOGGER = LogUtils.getLogger()
    const val INSTANCE_SIZE = 64
    const val MAX_ENABLED_MORPH_TARGETS = 32

    override lateinit var scope: CoroutineScope
        private set

    override fun onInitializeClient() {
        super.onInitialize()
        if (System.getProperty("armorstand.debug") == "true") {
            RenderPassImpl.IS_DEVELOPMENT = true
            if (System.getProperty("armorstand.debug.gui") == "true") {
                ResourceCountTracker.initialize()
                ObjectPoolTracker.initialize()
                System.setProperty("java.awt.headless", "false")
                SwingUtilities.invokeLater {
                    try {
                        ResourceCountTrackerFrame().isVisible = true
                        ModelManagerDebugFrame().isVisible = true
                        ObjectCountTrackerFrame().isVisible = true
                    } catch (ex: Exception) {
                        LOGGER.info("Failed to show debug windows", ex)
                    }
                }
            }
        }

        ConfigHolder.read()

        RenderMaterial.initialize()

        WorldRenderEvents.START.register { context ->
            PlayerRenderer.flipObjectPools()
        }
        WorldRenderEvents.BEFORE_ENTITIES.register { context ->
            PlayerRenderer.startRenderWorld()
        }
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            PlayerRenderer.executeDraw(context)
        }

        ClientLifecycleEvents.CLIENT_STARTED.register { client ->
            scope = CoroutineScope(SupervisorJob() + ThreadExecutorDispatcher(MinecraftClient.getInstance()))
            NetworkModelSyncer.initialize()
            ModelInstanceManager.initialize()
        }
        ClientPlayNetworking.registerGlobalReceiver(PlayerModelUpdateS2CPayload.ID) { payload, context ->
            ModelInstanceManager.updatePlayerModel(payload.uuid, payload.path)
        }
    }
}
