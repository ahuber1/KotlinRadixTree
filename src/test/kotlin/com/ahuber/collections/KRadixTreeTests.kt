package com.ahuber.collections

import com.ahuber.test.utils.getResourceAsFile
import com.ahuber.utils.halveLeft
import com.ahuber.utils.halveRight
import com.ahuber.utils.middle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KRadixTreeTests {
    data class SizedSequence<T>(val sequence: Sequence<T>, val size: Int): Sequence<T> {
        override fun iterator(): Iterator<T> = sequence.iterator()
    }

    @Test
    fun `test with words`() {
        val words = getWords()
        val tree = KRadixTree()
        val targetWord = "abdications"
        var targetWordAdded = false

        for ((index, word) in words.withIndex()) {
            println("[${index + 1}/${words.size}] $word")

            if (targetWord in tree && !targetWordAdded) {
                fail("Something terrible happened.")
            }

            assertTrue(tree.add(word))
            assertTrue(word in tree)

            if (targetWord == word) {
                targetWordAdded = true
            }

            assertEquals(index + 1, tree.size)
        }
    }

    private fun getWords(): SizedSequence<String> {
        val path = "words.txt"
        val file = getResourceAsFile(path) ?: fail("Unable to get resource $path")
        var words: MutableList<String?> = file.readText()
                .split(Regex("\\s"))
                .asSequence()
                .map { it.toLowerCase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()

        val sequence = sequence<String> {
            var leftRange: IntRange? = null
            var rightRange: IntRange? = null

            while (true) {
                if (leftRange == null && rightRange == null) {
                    words = words.filterNotNullTo(ArrayList())
                }

                leftRange = when (leftRange) {
                    null -> words.indices.halveLeft()
                    else -> leftRange.halveLeft()
                }

                rightRange = when (rightRange) {
                    null -> words.indices.halveRight()
                    else -> rightRange.halveRight()
                }

                if (leftRange == null && rightRange == null) {
                    break
                }

                val leftValue = when (val leftMiddle = leftRange?.middle) {
                    null -> null
                    else -> words[leftMiddle].also { words[leftMiddle] = null }
                }

                val rightValue = when (val rightMiddle = rightRange?.middle) {
                    null -> null
                    else -> words[rightMiddle].also { words[rightMiddle] = null }
                }

                if (leftValue != null) {
                    yield(leftValue)
                }

                if (rightValue != null) {
                    yield(rightValue)
                }
            }

            for (word in words.filterNotNull()) {
                yield(word)
            }
        }

        return SizedSequence(sequence, words.size)
    }
}