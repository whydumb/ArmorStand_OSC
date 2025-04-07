package top.fifthlight.armorstand.state

import net.minecraft.client.network.ClientPlayerEntity
import top.fifthlight.armorstand.model.ModelInstance

sealed class ModelController {
    open fun update(player: ClientPlayerEntity) = Unit
    abstract fun apply(instance: ModelInstance)

    data class LiveUpdated(
        var bodyRotation: Float = 0f,
        var headYaw: Float = 0f,
        var headPitch: Float = 0f,
    ): ModelController() {
        override fun update(player: ClientPlayerEntity) {

        }

        override fun apply(instance: ModelInstance) {

        }
    }

    class Predefined(

    ): ModelController() {
        override fun apply(instance: ModelInstance) {

        }
    }
}