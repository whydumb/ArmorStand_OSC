package top.fifthlight.armorstand.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.network.ModelUpdateC2SPayload

object NetworkModelSyncer {
    private val packetSender = MutableStateFlow<PacketSender?>(null)

    fun initialize() {
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            packetSender.value = sender
        }
        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            packetSender.value = null
        }
        ArmorStand.instance.scope.launch {
            ConfigHolder.config.combine(packetSender, ::Pair).collect { (config, sender) ->
                if (config.sendModelData) {
                    sender?.sendPacket(ModelUpdateC2SPayload(config.modelPath?.toString()))
                }
            }
        }
    }
}