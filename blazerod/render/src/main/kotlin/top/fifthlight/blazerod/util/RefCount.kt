package top.fifthlight.blazerod.util

import kotlinx.atomicfu.atomic
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.debug.ResourceCountTracker

class InvalidReferenceCountException(
    obj: Any,
    count: Int,
) : Exception("Bad reference count $count for object $obj")

interface RefCount {
    val closed: Boolean
    val referenceCount: Int
    fun increaseReferenceCount()
    fun decreaseReferenceCount()
}

abstract class AbstractRefCount : RefCount {
    final override var closed: Boolean = false
        private set

    private val initializedAtomic = atomic(false)
    private var initialized by initializedAtomic

    private val referenceCountAtomic = atomic(0)
    final override var referenceCount by referenceCountAtomic

    abstract val typeId: Identifier

    protected fun requireNotClosed() = require(!closed) { "Object $this is already closed." }

    override fun increaseReferenceCount() {
        requireNotClosed()
        if (referenceCountAtomic.getAndIncrement() < 0) {
            throw InvalidReferenceCountException(this, referenceCount)
        }
        if (initializedAtomic.compareAndSet(expect = false, update = true)) {
            ResourceCountTracker.instance?.increase(typeId)
        }
    }

    override fun decreaseReferenceCount() {
        requireNotClosed()
        check(initialized) { "Object $this is not initialized." }
        val refCount = referenceCountAtomic.decrementAndGet()
        when {
            refCount < 0 -> throw InvalidReferenceCountException(this, referenceCount)
            refCount == 0 -> {
                closed = true
                ResourceCountTracker.instance?.decrease(typeId)
                onClosed()
            }
        }
    }

    /**
     * Reset the state.
     *
     * It is designed to be used for objects being pooled. When the object is returned to the pool,
     * you can call this method to reset closed state, so it can be reused.
     */
    protected fun resetState() {
        require(closed) { "Object $this is not closed when resetting reference count." }
        require(referenceCount == 0) { "Object $this has reference count $referenceCount when resetting reference count." }
        initialized = false
        closed = false
    }

    protected abstract fun onClosed()
}

inline fun <R> RefCount.use(crossinline block: () -> R): R {
    try {
        increaseReferenceCount()
        return block()
    } finally {
        decreaseReferenceCount()
    }
}

fun RefCount.checkInUse() = require(referenceCount > 0) { "Object $this is not in use." }
