package com.ahuber.test.utils

import com.ahuber.utils.halveLeft
import com.ahuber.utils.halveRight
import com.ahuber.utils.middle
import com.ahuber.utils.wrapInQuotes
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

object DataUtils {
    fun getWords(): Sequence<String> {
        val path = "words.txt"
        val file = getResourceAsFile(path) ?: fail("Unable to get resource $path")
        return file.readText()
                .split(Regex("\\s"))
                .asSequence()
                .map { it.toLowerCase() }
                .filter { it.isNotEmpty() }
                .distinct()
    }

    fun getWords(direction: Direction): Array<String> {
        val words = getWords().toList().toTypedArray()
        val indices = words.generateIndices(direction)
        return Array(indices.size) { words[it] }
    }

    /**
     * Generates an [IntArray] of indices pointing to elements in the original array. This method can be used to shuffle
     * the array, but does it in a manner that is consistent each time the tests are executed due to no random number
     * generator being used.
     *
     * The indices are added to the array by partitioning the array into two halves and adding the middle index of each
     * half to the index list. The half whose middle index is added to the index list first is determined by the value
     * of the [direction] parameter.
     *
     * - If [Direction.LR], the middle index of the *left* half is added before the middle index of the *right* half.
     * - If [Direction.RL]. the middle index of the *right* half is added before the middle index of the *left* half.
     *
     * This process is repeated recursively until all indices are added. Note that the returned [IntArray] *does not*
     * contain repeated indices.
     */
    private fun <T> Array<T>.generateIndices(direction: Direction): IntArray {
        fun generateIndices(range: IntRange, indices: MutableMap<Int, MutableMap<Int, Int>>, level: Int,
                            direction: Direction) {
            val list = indices.compute(level) { _, value -> value ?: mutableMapOf() }
            check(list != null)

            if (range.middle !in list) {
                list[range.middle] = list.size
            }

            val leftRange = range.halveLeft()
            val rightRange = range.halveRight()

            val (first, second) = when (direction) {
                Direction.LR -> leftRange to rightRange
                Direction.RL -> rightRange to leftRange
            }

            if (first != null) generateIndices(first, indices, level + 1, direction)
            if (second != null) generateIndices(second, indices, level + 1, direction)
        }

        return when (this.isEmpty()) {
            true -> IntArray(0)
            false -> {
                val map = HashMap<Int, MutableMap<Int, Int>>()
                generateIndices(this.indices, map, 0, direction)
                map.asSequence()
                        // Map each entry to a Pair<Int, Sequence<Int>>
                        //      - The first value is the key in the entry
                        //      - The second value is the corresponding value where the keys
                        //        (the indices) are ordered by their value (the order in which the index was
                        //        added to the map)
                        .map { entry ->
                            entry.key to entry.value.asSequence().sortedBy { it.value }.map { it.key }
                        }
                        // Sort each entry by their level
                        .sortedBy { it.first }
                        // Sort each
                        .flatMap { it.second }
                        .distinct()
                        .toList()
                        .toIntArray()
            }
        }
    }
}


fun Collection<String>.assertContainsWords(words: Iterable<String>) {
    val distinctWords = when (words) {
        is Set<String> -> words.asSequence()
        else -> words.asSequence().distinct()
    }
    val wordMap = distinctWords.map { it to false }.toMap(HashMap())
    assertEquals(wordMap.size, size)

    for (word in this) {
        assertTrue(word in wordMap,
                "The word ${word.wrapInQuotes()} was not in the original word list.")
        wordMap[word] = true
    }

    val wordLimit = 20
    val missingWords = wordMap.entries.asSequence().filter { !it.value }.map { it.key }.toList()
    val wordsString = missingWords.withIndex().joinToString(
            separator = "\n",
            limit = wordLimit,
            truncated = "And %,d more".format(missingWords.size - wordLimit)) { "\t[${it.index}] ${it.value}" }

    if (wordsString.isNotEmpty()) {
        val errorMessage = "Not all words that are in the radix tree were returned by the iterator.\n" +
                "The missing words are:\n$wordsString"
        fail(errorMessage)
    }
}