package kotlinradixparser

import kotlinradixtree.KRadixTree
import kotlinradixtree.format
import kotlinradixtree.readFileLazily
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
        File("test_files/words.txt").readFileLazily {
            val word = it.trim().toLowerCase()

            if (word.isNotEmpty()) {
                println("Adding $word")
                radixTree.add(word)

                if (word.length > lengthOfLongestWord)
                    lengthOfLongestWord = word.length
            }

        }
        return@lazy radixTree
    }

    fun parse(string: String, onResultsFound: (Array<KotlinRadixParserResult>) -> Boolean) =
            parse(string, 0, LinkedList(), onResultsFound)

    private fun parse(string: String,
                      startIndex: Int,
                      results: LinkedList<KotlinRadixParserResult>,
                      onResultsFound: (Array<KotlinRadixParserResult>) -> Boolean) : Boolean {
        println("${((startIndex.toDouble() * 100.0) / string.length.toDouble()).format(2)}%")

        var continueLooking = true
        val parserResults = getKotlinRadixParserResultsForStringStartingAtIndex(string, startIndex)

        if (parserResults.none())
            return onResultsFound(results.toTypedArray())

        for (parserResult in parserResults) {
            frequencyMap[parserResult] = (frequencyMap[parserResult] ?: 0) + 1
        }

        val sortedParserResults = parserResults.groupBy { frequencyMap[it]!! }
                .map { it.value.sortedByDescending { it.string.length } }.flatten()

        for (parserResult in sortedParserResults) {
            if (!continueLooking)
                return false

            results.addLast(parserResult)
            continueLooking = parse(string, startIndex + parserResult.string.length, results, onResultsFound)
            results.removeLast()
        }

        return continueLooking
    }

    private fun getKotlinRadixParserResultsForStringStartingAtIndex(string: String, startIndex: Int) : Iterable<KotlinRadixParserResult> {
        val results = getKotlinRadixParserResultsForStringStartingAtIndex(string, startIndex, false)

        if (results.isNotEmpty())
            return results.sortedByDescending { it.length }.mapLazy { KotlinRadixParserResult(it, true) }

        for (i in (startIndex + 1)..string.length) {
            if (results.isNotEmpty())
                continue

            val list = getKotlinRadixParserResultsForStringStartingAtIndex(string, i, true)

            if (list.isNotEmpty()) {
                results.addLast(list.first)
            }
        }

        return results.mapLazy { KotlinRadixParserResult(it, false) }
    }

    private fun getKotlinRadixParserResultsForStringStartingAtIndex(string: String,
                                                                    startIndex: Int,
                                                                    stopOnFirstMatch: Boolean): LinkedList<String> {

        // Progressively make longer and longer strings, and only put the ones that are in the KRadixTree into results
        var stopLooking = false
        var children: Iterable<String>? = null
        val results = LinkedList<String>()

        for (i in (startIndex + 1)..string.length) {
            if (i == (startIndex + lengthOfLongestWord) && results.isEmpty())
                stopLooking = true

            if (stopLooking)
                continue

            var substring = string.substring(startIndex, i)
            val (inTree, iterable) = radixTree.containsDetailed(substring)

            if (inTree) {
                children = iterable
                results.addLast(substring)

                if (stopOnFirstMatch)
                    stopLooking = true
            } else {
                val lastItem = results.lastOrNull() ?: continue
                substring = substring.substring(lastItem.length, substring.length)
                children = children?.toList()
                stopLooking = children?.none { it.startsWith(substring) } ?: false
            }
        }
        return results
    }
}