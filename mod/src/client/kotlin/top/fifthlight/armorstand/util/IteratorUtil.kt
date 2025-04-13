package top.fifthlight.armorstand.util

private object EmptyIterator: Iterator<Any?> {
    override fun next() = throw NoSuchElementException()

    override fun hasNext() = false
}

@Suppress("UNCHECKED_CAST")
fun <T> iteratorOf(): Iterator<T> = EmptyIterator as Iterator<T>

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
