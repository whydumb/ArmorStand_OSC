package top.fifthlight.armorstand

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import top.fifthlight.armorstand.server.ModelPathManager
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher

object ArmorStandServer: ArmorStand(), DedicatedServerModInitializer {
    override lateinit var scope: CoroutineScope
        private set

    override fun onInitializeServer() {
        super.onInitialize()
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            scope = CoroutineScope(SupervisorJob() + ThreadExecutorDispatcher(server))
        }
    }
}