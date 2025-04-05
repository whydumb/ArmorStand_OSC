package top.fifthlight.armorstand.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import net.minecraft.client.MinecraftClient
import kotlin.coroutines.CoroutineContext

object ClientThreadDispatcher : CoroutineDispatcher() {
    private val client = MinecraftClient.getInstance()

    override fun isDispatchNeeded(context: CoroutineContext) = !client.isOnThread

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (client.isOnThread) {
            Dispatchers.Unconfined.dispatch(context, block)
        } else {
            client.execute(block)
        }
    }
}