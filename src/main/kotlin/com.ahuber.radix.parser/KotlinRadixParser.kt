package com.ahuber.radix.parser

import com.ahuber.collections.KRadixTree
import com.ahuber.utils.format
import java.io.File
import java.util.*
import kotlin.collections.HashMap

fun String.radixParse(onResultsFound: (Array<KotlinRadixParserResult>) -> Boolean) = KotlinRadixParser().parse(this, onResultsFound)

data class KotlinRadixParserResult(val string: String, val wasOnRecord: Boolean)

private class KotlinRadixParser {
    private val frequencyMap = HashMap<KotlinRadixParserResult, Int>()
    private var lengthOfLongestWord = Int.MIN_VALUE
    private val radixTree: KRadixTree by lazy {
        println("Loading Radix Tree...")
        val radixTree = KRadixTree()
        val lines = File("test_files/words.txt").readLines()

        for (line in lines) {
            val word = line.trim().toLowerCase()

            if (word.isEmpty()) {
                continue
            }

            println("Adding $word")
            radixTree.add(word)

            if (word.length > lengthOfLongestWord) {
                lengthOfLongestWord = word.length
            }
        }

        radixTree
    }

    fun parse(string: String, onResultsFound: (Array<KotlinRadixParserResult>) -> Boolean) {
        val startIndexStack = Stack<Int>()
        val resultsStack = Stack<KotlinRadixParserResult>()
        val results = LinkedList<KotlinRadixParserResult>()
        var continueLooking = true
        var startIndex: Int? = null
        startIndexStack.push(0)

        while (startIndexStack.isNotEmpty() && continueLooking) {
           if (results.isNotEmpty() && startIndexStack.peek() < (startIndex ?: Int.MIN_VALUE))
               results.removeLast()

            startIndex = startIndexStack.pop()

            println("${((startIndex.toDouble() * 100.0) / string.length.toDouble()).format(2)}%")

            if (resultsStack.isNotEmpty())
                results.addLast(resultsStack.pop())

            val parserResults = getKotlinRadixParserResultsForStringStartingAtIndex(string, startIndex)

            if (parserResults.none())
                continueLooking = onResultsFound(results.toTypedArray())
            else {
                for (parserResult in parserResults) {
                    frequencyMap[parserResult] = (frequencyMap[parserResult] ?: 0) + 1
                }

                val sortedParserResults = parserResults.groupBy { frequencyMap[it]!! }
                        .map { entry -> entry.value.sortedBy { it.string.length } }.flatten()

                for (parserResult in sortedParserResults) {
                    resultsStack.push(parserResult)
                    startIndexStack.push(startIndex + parserResult.string.length)
                }
            }
        }
    }

    private fun getKotlinRadixParserResultsForStringStartingAtIndex(string: String, startIndex: Int) : Sequence<KotlinRadixParserResult> {
        var results = getKotlinRadixParserResultsForStringStartingAtIndex(string, startIndex, false)

        if (results.any()) {
            return results.sortedByDescending { it.length }.map { KotlinRadixParserResult(it, true) }
        }

        for (i in (startIndex + 1)..string.length) {
            val match = getKotlinRadixParserResultsForStringStartingAtIndex(string, i, true).firstOrNull() ?: continue
            results += match
            break
        }

        return results.map { KotlinRadixParserResult(it, false) }
    }

    private fun getKotlinRadixParserResultsForStringStartingAtIndex(string: String, startIndex: Int,
            stopOnFirstMatch: Boolean): Sequence<String> = sequence {

            // Progressively make longer and longer strings, and only put the ones that are in the KRadixTree into the results
            var children: Sequence<String>? = null
            var lastItem: String? = null

            for (i in (startIndex + 1)..string.length) {
                if (i == (startIndex + lengthOfLongestWord) && lastItem != null) {
                    return@sequence
                }

                var substring = string.substring(startIndex, i)
                val sequence = radixTree[substring]

                if (sequence != null) {
                    children = sequence
                    lastItem = substring
                    yield(substring)

                    if (stopOnFirstMatch) {
                        return@sequence
                    }

                } else if (lastItem != null) {
                    substring = substring.substring(lastItem.length, substring.length)

                    if (children != null && children.none { it.startsWith(substring) }) {
                        return@sequence
                    }
                }
            }
        }
}