package com.ahuber.collections

import com.ahuber.test.utils.getResourceAsFile
import com.ahuber.utils.*
import org.junit.jupiter.api.Assertions
import java.time.Duration
import kotlin.test.*

typealias TestWithWordsBlock = (index: Int, word: String, set: MutableSet<String>) -> Unit

class KRadixTreeTests {
    enum class Direction { LR, RL }

    @Test
    fun `test with words`() = Direction.values().forEach { addThenRemove(it) }

    @Test
    fun `test iterator`() {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(60)) {
            for (direction in Direction.values()) {
                val words = getWords(direction).toList()
                val radixTree = KRadixTree(words)
                radixTree.assertContainsWords(words)
            }
        }
    }

    @Test
    fun `test iterator with remove`() {
        Assertions.assertTimeoutPreemptively(Duration.ofMinutes(1)) {
            val words = getWords()
            val characterSet = words.groupBy { it.first() }
                    .map { it.key to it.value.size }
                    .asSequence()
                    .sortedByDescending { it.second }
                    .take(3)
                    .map { it.first }
                    .toSet()
            val controlSet = words.toMutableSet()
            val testSet = KRadixTree(words)

            val controlIterator = controlSet.iterator()
            val testIterator = testSet.iterator()
            var index = -1L
            var removeFromControlCount = 0
            var removeFromTestCount = 0

            while (controlIterator.hasNext() || testIterator.hasNext()) {
                index++
                try {
                    val nextFromControl = controlIterator.next()
                    val nextFromTest = testIterator.next()

                    if (nextFromControl[0] in characterSet) {
                        controlIterator.remove()
                        removeFromControlCount++
                    }

                    if (nextFromTest[0] in characterSet) {
                        testIterator.remove()
                        removeFromTestCount++
                    }
                } catch (e: NoSuchElementException) {
                    System.err.apply {
                        println("NoSuchElementException")
                        printf("\tIndex: %,d\n", index)
                        println("\tRemove Counts:")
                        printf("\t\tControl: %,d\n", removeFromControlCount)
                        printf("\t\t   Test: %,d\n", removeFromTestCount)
                    }
                    throw e
                }
            }

            assertTrue(controlSet.none { it.first() in characterSet })
            assertTrue(testSet.none { it.first() in characterSet })
            assertEquals(removeFromControlCount, removeFromTestCount)
            assertEquals(controlSet.size, testSet.size)
            testSet.assertContainsWords(controlSet)
        }
    }

    @Test
    fun `test search`() {
        val tree = KRadixTree("apple", "application", "apply")
        val result = tree.search("apple")
        assertTrue(result is WordSearchResult.ExactMatch)
        assertEquals("apple", result.searchString)
        assertTrue(result.longerWords.isEmpty())
    }

    private val Direction.otherDirection
        get() = when (this) {
            Direction.LR -> Direction.RL
            Direction.RL -> Direction.LR
        }

    private fun addThenRemove(initialDirection: Direction) {
        var words = getWords(initialDirection)
        val sets: Array<MutableSet<String>> = arrayOf(HashSet(), KRadixTree())

        iterateThroughWordsAndSets(words, sets) { index, word, set ->
            assertTrue(set.add(word))
            assertTrue(word in set)
            assertEquals(index + 1, set.size)
        }

        words = getWords(initialDirection.otherDirection)
        val initialSize = words.size

        iterateThroughWordsAndSets(words, sets) { index, word, set ->
            assertTrue(set.remove(word))
            assertFalse(word in set)
            assertEquals(initialSize - index - 1, set.size)
        }
    }

    private inline fun iterateThroughWordsAndSets(words: Array<String>,
            sets: Array<MutableSet<String>>, block: TestWithWordsBlock) {
        for ((index, word) in words.withIndex()) {
            for (set in sets) {
                block(index, word, set)
            }
        }
    }

    private fun getWords(): Sequence<String> {
        val path = "words.txt"
        val file = getResourceAsFile(path) ?: fail("Unable to get resource $path")
        return file.readText()
                .split(Regex("\\s"))
                .asSequence()
                .map { it.toLowerCase() }
                .filter { it.isNotEmpty() }
                .distinct()
    }

    private fun getWords(direction: Direction): Array<String> {
        val words = getWords().toList().toTypedArray()
        val indices =words.generateIndices(direction)
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

private fun KRadixTree.assertContainsWords(words: Iterable<String>) {
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