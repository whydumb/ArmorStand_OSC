package top.fifthlight.armorstand.util

import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue

class SlottedMap<T : RefCount>(initialCapacity: Int = 1) : AbstractInt2ObjectMap<T>(), RefCount {
    override var size: Int = 0
    private val items = ArrayList<T?>(initialCapacity)
    private val freeIndices = IntHeapPriorityQueue(initialCapacity)

    @Suppress("OVERRIDE_DEPRECATION")
    override operator fun get(key: Int): T? {
        requireNotClosed()
        if (key !in items.indices) return null
        return items[key]
    }

    fun put(value: T): Int {
        requireNotClosed()
        val index = if (freeIndices.isEmpty) {
            items.size
        } else {
            freeIndices.dequeueInt()
        }
        when (index) {
            items.size -> {
                value.increaseReferenceCount()
                items.add(value)
                size++
            }
            else -> when (val existing = items[index]) {
                null -> {
                    value.increaseReferenceCount()
                    items[index] = value
                    size++
                }
                value -> return index
                else -> {
                    value.increaseReferenceCount()
                    existing.decreaseReferenceCount()
                    items[index] = value
                }
            }
        }
        return index
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun remove(key: Int): T? {
        requireNotClosed()
        if (key !in items.indices) return null

        return items[key]?.also { value ->
            value.decreaseReferenceCount()
            items[key] = null
            freeIndices.enqueue(key)
            size--
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun int2ObjectEntrySet() = throw UnsupportedOperationException()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun put(key: Int, value: T?) = throw UnsupportedOperationException()

    override var closed: Boolean = false

    override var referenceCount: Int = 0

    private fun requireNotClosed() = require(!closed) { "Object $this is already closed." }

    override fun increaseReferenceCount() {
        requireNotClosed()
        if (referenceCount < 0) {
            throw InvalidReferenceCountException(this, referenceCount)
        }
        referenceCount++
    }

    override fun decreaseReferenceCount() {
        when (referenceCount) {
            0 -> close()
            else -> referenceCount--
        }
    }

    private fun close() {
        closed = true
        items.forEach { item ->
            item?.decreaseReferenceCount()
        }
    }
}
