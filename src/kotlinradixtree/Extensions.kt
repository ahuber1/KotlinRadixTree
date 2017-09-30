package kotlinradixtree

import org.testng.annotations.Test
import java.io.File
import java.util.*
import kotlin.test.assertEquals

fun File.readFileLazily(action: (String) -> Unit) {
    Scanner(this).use {
        while (it.hasNext()) {
            action(it.nextLine())
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

// Taken from: https://github.com/gazolla/Kotlin-Algorithm/blob/master/Shuffle/Shuffle.kt
fun <T:Comparable<T>>shuffle(items:MutableList<T>):List<T>{
    val rg = Random()
    for (i in 0 until items.size) {
        val randomPosition = rg.nextInt(items.size)
        val tmp : T = items[i]
        items[i] = items[randomPosition]
        items[randomPosition] = tmp
    }
    return items
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