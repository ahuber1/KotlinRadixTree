package com.ahuber.collections

import com.ahuber.test.utils.getResourceAsFile
import com.ahuber.utils.getLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KRadixTreeTests {
    @Test
    fun `test with words`() {
        val path = "words.txt"
        val file = getResourceAsFile(path) ?: fail("Unable to get resource $path")
        val words = file.readText()
                .split(Regex("\\s"))
                .asSequence()
                .map { it.toLowerCase() }
                .filter { it.isNotEmpty() }
                .toSortedSet()
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
}