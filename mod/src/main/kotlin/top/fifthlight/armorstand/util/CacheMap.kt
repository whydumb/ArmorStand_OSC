package top.fifthlight.armorstand.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CacheMap<K, V> {
    private val lock = Mutex()
    private val map = mutableMapOf<K, Deferred<V>>()

    suspend operator fun get(key: K) = lock.withLock { map[key] }?.await()

    suspend fun compute(key: K, func: suspend (K) -> V): V {
        lock.lock()
        return when (val value = map[key]) {
            null -> coroutineScope {
                try {
                    val pending = async {
                        func(key)
                    }
                    map[key] = pending
                    return@coroutineScope pending.await()
                } finally {
                    lock.unlock()
                }
            }

            else -> {
                lock.unlock()
                value.await()
            }
        }
    }

    suspend fun clear() = lock.withLock { map.clear() }
}
