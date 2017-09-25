package kotlinradixtree

import org.testng.annotations.Test
import java.io.File
import java.util.*
import kotlin.NoSuchElementException
import kotlin.test.assertEquals

fun File.readFileLazily(action: (String) -> Unit) {
    Scanner(this).use {
        while (it.hasNext()) {
            action(it.nextLine())
        }
    }
}

fun <T> List<T>.indiciesThatMatchPredicate(predicate: (T) -> Boolean) : Iterable<Int> = object: Iterable<Int> {
    override fun iterator(): Iterator<Int> = object: Iterator<Int> {
        private val iterator = this@indiciesThatMatchPredicate.listIterator()
        private var nextItem: Int? = null
        private var findNext = false

        override fun next(): Int {
            if (findNext) {
                nextItem = findNext()
            }

            findNext = true
            return nextItem ?: throw NoSuchElementException()
        }

        override fun hasNext(): Boolean {
            nextItem = findNext()
            findNext = false
            return nextItem != null
        }

        private fun findNext() : Int? {
            while (iterator.hasNext()) {
                val item = iterator.next()
                val index = iterator.nextIndex()

                if (predicate(item))
                    return index
            }

            return null
        }
    }

}

fun String.toCharArrayWithFill(newLength: Int, fillerChar: Char = '\u0000') : CharArray =
        CharArray(newLength, { index -> if (index < this.length) this[index] else fillerChar } )
infix fun <A, B> Iterable<A>.iterateSimultaneouslyWith(otherIterable: Iterable<B>) : Iterable<Pair<A, B>>  = object: Iterable<Pair<A, B>> {
    override fun iterator(): Iterator<Pair<A, B>> = object: Iterator<Pair<A, B>> {
        var iterator1 = this@iterateSimultaneouslyWith.iterator()
        var iterator2 = otherIterable.iterator()

        override fun hasNext(): Boolean = iterator1.hasNext() && iterator2.hasNext()

        override fun next(): Pair<A, B> = Pair(iterator1.next(), iterator2.next())
    }

}

@Test fun `Test readFileLazily`() {
    val originalContent = File("README.md").readLines().joinToString(separator = "\n")
    val lines = ArrayList<String>()
    File("README.md").readFileLazily { lines.add(it) }
    val lazilyReadContent = lines.joinToString(separator = "\n")
    assertEquals(originalContent, lazilyReadContent)

    println("originalContent = \n$originalContent")
    println()
    println("lazilyReadContent = \n$lazilyReadContent")
}