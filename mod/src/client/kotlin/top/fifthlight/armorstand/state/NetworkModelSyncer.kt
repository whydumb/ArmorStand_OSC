package top.fifthlight.armorstand.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.manage.ModelManager
import top.fifthlight.armorstand.network.ModelUpdateS2CPayload

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
            ConfigHolder.config.combine(packetSender, ::Pair).collectLatest { (config, sender) ->
                val hash = config.modelPath?.let { ModelManager.getModel(it)?.hash }
                sender?.sendPacket(ModelUpdateS2CPayload(hash))
            }
        }
    }
}