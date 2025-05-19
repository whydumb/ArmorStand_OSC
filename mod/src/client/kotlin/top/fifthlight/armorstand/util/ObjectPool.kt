package top.fifthlight.armorstand.util

import net.minecraft.util.Identifier
import top.fifthlight.armorstand.debug.ObjectPoolTracker
import java.util.ArrayDeque

interface Pool<T> {
    fun acquire(): T
    fun release(obj: T)
}

open class ObjectPool<T: Any>(
    protected val identifier: Identifier,
    protected val create: () -> T,
    protected val onAcquired: (T.() -> Unit)? = null,
    protected val onReleased: (T.() -> Unit)? = null,
) : Pool<T> {
    protected val pool = ArrayDeque<T>()

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

class FramedObjectPool<T : Any>(
    identifier: Identifier,
    create: () -> T,
    onAcquired: (T.() -> Unit)? = null,
    onReleased: (T.() -> Unit)? = null,
) : ObjectPool<T>(identifier, create, onAcquired, onReleased) {
    private val pendingRelease = ArrayDeque<T>()

    override fun release(obj: T) {
        ObjectPoolTracker.instance?.compute(identifier) {
            copy(
                allocatedItem = allocatedItem - 1,
                pooledItem = pooledItem + 1,
            )
        }
        onReleased?.invoke(obj)
        pendingRelease.addLast(obj)
    }

    fun frame() {
        if (pendingRelease.isNotEmpty()) {
            ObjectPoolTracker.instance?.compute(identifier) {
                copy(pooledItem = pooledItem + pendingRelease.size)
            }
            pool.addAll(pendingRelease)
            pendingRelease.clear()
        }
    }

    init {
        POOLS.add(this)
    }

    companion object {
        private val POOLS = mutableListOf<FramedObjectPool<*>>()

        fun frame() {
            POOLS.forEach { it.frame() }
        }
    }
}
