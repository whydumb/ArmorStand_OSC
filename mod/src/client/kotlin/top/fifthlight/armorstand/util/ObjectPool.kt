package top.fifthlight.armorstand.util

import net.minecraft.util.Identifier
import top.fifthlight.armorstand.debug.ObjectPoolTracker
import java.util.ArrayDeque

interface Pool<T> {
    fun acquire(): T
    fun release(obj: T)
}

class ObjectPool<T: Any>(
    private val identifier: Identifier,
    private val create: () -> T,
    private val onAcquired: (T.() -> Unit)? = null,
    private val onReleased: (T.() -> Unit)? = null,
) : Pool<T> {
    private val pool = ArrayDeque<T>()

    override fun acquire(): T {
        return if (pool.isEmpty()) {
            ObjectPoolTracker.instance?.compute(identifier) {
                copy(allocatedItem = allocatedItem + 1)
            }
            create()
        } else {
            ObjectPoolTracker.instance?.compute(identifier) {
                copy(
                    allocatedItem = allocatedItem + 1,
                    pooledItem = pooledItem - 1,
                )
            }
            pool.removeFirst()
        }.also { obj ->
            onAcquired?.invoke(obj)
        }
    }

    override fun release(obj: T) {
        ObjectPoolTracker.instance?.compute(identifier) {
            copy(
                allocatedItem = allocatedItem - 1,
                pooledItem = pooledItem + 1,
            )
        }
        onReleased?.invoke(obj)
        pool.addLast(obj)
    }
}
