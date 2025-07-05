package top.fifthlight.blazerod

import com.mojang.logging.LogUtils
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.gl.RenderPassImpl
import top.fifthlight.armorstand.debug.ObjectCountTrackerFrame
import top.fifthlight.armorstand.debug.ResourceCountTrackerFrame
import top.fifthlight.blazerod.debug.ObjectPoolTracker
import top.fifthlight.blazerod.debug.ResourceCountTracker
import top.fifthlight.blazerod.event.RenderEvents
import top.fifthlight.blazerod.model.RenderMaterial
import top.fifthlight.blazerod.model.uniform.UniformBuffer
import top.fifthlight.blazerod.util.cleanupPools
import javax.swing.SwingUtilities

object BlazeRod: ClientModInitializer {
    private val LOGGER = LogUtils.getLogger()

    const val INSTANCE_SIZE = 256
    const val MAX_ENABLED_MORPH_TARGETS = 32
    const val MAX_TRANSFORM_DEPTH = 64

    override fun onInitializeClient() {
        RenderMaterial.initialize()

        if (System.getProperty("blazerod.debug") == "true") {
            RenderPassImpl.IS_DEVELOPMENT = true
            if (System.getProperty("blazerod.debug.gui") == "true") {
                ResourceCountTracker.initialize()
                ObjectPoolTracker.initialize()
                System.setProperty("java.awt.headless", "false")
                SwingUtilities.invokeLater {
                    try {
                        ResourceCountTrackerFrame().isVisible = true
                        ObjectCountTrackerFrame().isVisible = true
                    } catch (ex: Exception) {
                        LOGGER.info("Failed to show debug windows", ex)
                    }
                }
            }
        }

        RenderEvents.FLIP_FRAME.register {
            UniformBuffer.clear()
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register { client ->
            cleanupPools()
            UniformBuffer.close()
        }
    }
}