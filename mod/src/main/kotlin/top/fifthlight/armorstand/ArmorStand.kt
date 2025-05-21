package top.fifthlight.armorstand

import kotlinx.coroutines.CoroutineScope
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import top.fifthlight.armorstand.network.ModelUpdateS2CPayload
import top.fifthlight.armorstand.network.PlayerModelUpdateS2CPayload
import top.fifthlight.armorstand.server.ModelPathManager


abstract class ArmorStand : ModInitializer {
    abstract val scope: CoroutineScope

    companion object {
        lateinit var instance: ArmorStand
    }

    private var server: MinecraftServer? = null

    override fun onInitialize() {
        instance = this
        PayloadTypeRegistry.playS2C().register(PlayerModelUpdateS2CPayload.ID, PlayerModelUpdateS2CPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ModelUpdateS2CPayload.ID, ModelUpdateS2CPayload.CODEC)

        ModelPathManager.onUpdateListener = { uuid, path ->
            server?.let { server ->
                val payload = PlayerModelUpdateS2CPayload(uuid, path)
                for (player in PlayerLookup.all(server)) {
                    if (player.uuid == uuid) {
                        continue
                    }
                    ServerPlayNetworking.send(player, payload)
                }
            }
        }
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            for ((uuid, path) in ModelPathManager.getModels()) {
                if (handler.player.uuid == uuid) {
                    continue
                }
                sender.sendPacket(PlayerModelUpdateS2CPayload(uuid, path))
            }
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            ModelPathManager.update(handler.player.uuid, null)
        }
        ServerPlayNetworking.registerGlobalReceiver(ModelUpdateS2CPayload.ID) { payload, context ->
            ModelPathManager.update(context.player().uuid, payload.path)
        }
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            ModelPathManager.clear()
        }
    }
}
