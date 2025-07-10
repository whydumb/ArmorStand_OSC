package top.fifthlight.blazerod.util

import it.unimi.dsi.fastutil.ints.IntIterable

private object EmptyIterator: Iterator<Nothing> {
    override fun next() = throw NoSuchElementException()

    override fun hasNext() = false
}

fun <T> iteratorOf(): Iterator<T> = EmptyIterator

fun <T> iteratorOf(item: T) = object : Iterator<T> {
    private var finished = false

    override fun next(): T {
        if (finished) {
            throw NoSuchElementException()
        }
        finished = true
        return item
    }

    override fun hasNext() = !finished
}

inline fun IntIterable.forEachInt(action: (Int) -> Unit) {
    val iterator = intIterator()
    while (iterator.hasNext()) {
        action(iterator.nextInt())
    }
}

inline fun IntIterable.forEachIntIndexed(action: (Int, Int) -> Unit) {
    val iterator = intIterator()
    var index = 0
    while (iterator.hasNext()) {
        action(index, iterator.nextInt())
        index++
    }
}

inline fun <T, reified R> List<T>.mapToArray(func: (T) -> R): Array<R> {
    val iterator = iterator()
    return Array(size) { func(iterator.next()) }
}

inline fun <T, reified R> List<T>.mapToArrayIndexed(func: (Int, T) -> R): Array<R> {
    val iterator = iterator()
    return Array(size) { func(it, iterator.next()) }
}

inline fun <T, reified R> Array<T>.mapToArray(func: (T) -> R) = Array(size) { func(this[it]) }
