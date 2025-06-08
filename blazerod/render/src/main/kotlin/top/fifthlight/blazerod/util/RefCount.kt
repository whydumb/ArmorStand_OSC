package top.fifthlight.blazerod.util

import kotlinx.atomicfu.atomic
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.debug.ResourceCountTracker

class InvalidReferenceCountException(
    obj: Any,
    count: Int
) : Exception("Bad reference count $count for object $obj")

interface RefCount {
    val closed: Boolean
    val referenceCount: Int
    fun increaseReferenceCount()
    fun decreaseReferenceCount()
}

abstract class AbstractRefCount : RefCount {
    override var closed: Boolean = false
        protected set
    private val referenceCountAtomic = atomic(0)
    final override var referenceCount by referenceCountAtomic

    abstract val typeId: Identifier

    init {
        ResourceCountTracker.instance?.increase(typeId)
    }

    protected fun requireNotClosed() = require(!closed) { "Object $this is already closed." }

    override fun increaseReferenceCount() {
        requireNotClosed()
        if (referenceCountAtomic.getAndIncrement() < 0) {
            throw InvalidReferenceCountException(this, referenceCount)
        }
    }

    override fun decreaseReferenceCount() {
        requireNotClosed()
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