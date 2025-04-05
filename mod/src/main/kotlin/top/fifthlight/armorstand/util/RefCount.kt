package top.fifthlight.armorstand.util

import kotlinx.atomicfu.atomic

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
                onClosed()
            }
        }
    }

    protected open fun onClosed() {}
}

inline fun <R> RefCount.use(crossinline block: () -> R): R {
    try {
        increaseReferenceCount()
        return block()
    } finally {
        decreaseReferenceCount()
    }
}