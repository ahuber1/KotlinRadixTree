package kotlinradixparser

fun <F, T> Iterable<F>.mapLazy(block: (F) -> T) : Iterable<T> = object: Iterable<T> {
    override fun iterator(): Iterator<T> = object: Iterator<T> {
        val iterator = this@mapLazy.iterator()

        override fun hasNext(): Boolean = iterator.hasNext()

        override fun next() = block(iterator.next())
    }
}

fun <T> Iterable<T>.filterLazy(block: (T) -> Boolean) : Iterable<T> = object: Iterable<T> {
    override fun iterator(): Iterator<T> = object: Iterator<T> {
        val iterator = this@filterLazy.iterator()
        var nextValue: T? = null

        override fun hasNext(): Boolean {
            nextValue = null

            while (iterator.hasNext()) {
                val value = iterator.next()

                if (block(value)) {
                    nextValue = value
                    return true
                }
            }

            return false
        }

        override fun next(): T {
            if (nextValue == null && !hasNext())
                throw NoSuchElementException()
            else if (nextValue == null)
                throw NoSuchElementException()

            val returnValue = nextValue!!
            nextValue = null
            return returnValue
        }
    }
}

fun <T> toIterable(block: () -> T?) = object : Iterable<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val value: T? by lazy { block.invoke() }
        var valueReturned = false

        override fun hasNext(): Boolean = (!valueReturned && value != null)

        override fun next(): T {
            if (valueReturned || value == null)
                throw NoSuchElementException()
            else {
                valueReturned = true
                return value!!
            }
        }
    }
}

fun <T> concatenate(vararg iterables: Iterable<T>) : Iterable<T> = makeIterator(iterables.iterator())
fun <T> Iterable<Iterable<T>>.flattenLazy(): Iterable<T> = makeIterator(this.iterator())

private fun <T> makeIterator(outerIterator: Iterator<Iterable<T>>) : Iterable<T> = object: Iterable<T> {
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val innerIterator = outerIterator.iterator()
        var currentIterables: Iterator<T>? = null

        override fun hasNext(): Boolean {
            if (currentIterables == null) {
                if (innerIterator.hasNext()) {
                    currentIterables = innerIterator.next().iterator()
                    return currentIterables!!.hasNext()
                }
                else {
                    return false
                }
            }

            if (currentIterables!!.hasNext())
                return true

            if (innerIterator.hasNext()) {
                currentIterables = innerIterator.next().iterator()
                return currentIterables!!.hasNext()
            }

            return false
        }

        override fun next(): T = currentIterables?.next() ?: throw NoSuchElementException()
    }
}