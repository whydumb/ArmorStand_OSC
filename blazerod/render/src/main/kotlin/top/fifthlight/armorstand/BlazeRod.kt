package top.fifthlight.armorstand

import net.fabricmc.api.ClientModInitializer
import top.fifthlight.armorstand.model.RenderMaterial

object BlazeRod: ClientModInitializer {
    const val INSTANCE_SIZE = 256
    const val MAX_ENABLED_MORPH_TARGETS = 32
    const val MAX_TRANSFORM_DEPTH = 64

    override fun onInitializeClient() {
        RenderMaterial.initialize()
    }
}