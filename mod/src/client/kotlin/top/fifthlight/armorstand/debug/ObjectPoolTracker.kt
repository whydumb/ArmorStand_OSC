package top.fifthlight.armorstand.debug

import net.minecraft.util.Identifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ObjectPoolTracker {
    val itemCount: ConcurrentMap<Identifier, Item> = ConcurrentHashMap()

    data class Item(
        val allocatedItem: Int = 0,
        val pooledItem: Int = 0,
        val failedItem: Int = 0,
    )

    inline fun compute(identifier: Identifier, crossinline func: Item.() -> Item) {
        itemCount.compute(identifier) { key, value -> func(value ?: Item()) }
    }

    fun dumpData() = itemCount.toMap()

    companion object {
        @Volatile
        var instance: ObjectPoolTracker? = null
            private set

        fun initialize() {
            synchronized(Companion) {
                if (instance != null) {
                    return
                }
                instance = ObjectPoolTracker()
            }
        }
    }
}