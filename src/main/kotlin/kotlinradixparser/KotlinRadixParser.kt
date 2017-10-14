package kotlinradixparser

import kotlinradixtree.KRadixTree
import kotlinradixtree.readFileLazily
import java.io.File
import java.util.*

data class KotlinRadixParserResult(val string: String, val wasOnRecord: Boolean)

fun String.radixParse() : Iterable<Iterable<KotlinRadixParserResult>> = KotlinRadixParser(this).parse()

private class KotlinRadixParser(string: String) {

    private val string: String
    private var maximumNumberOfUnknownWords: Int? = null
    private val radixTree: KRadixTree by lazy {
        println("Loading Radix Tree...")
        val radixTree = KRadixTree()
        File("test_files/words.txt").readFileLazily {
            val word = it.trim().toLowerCase()

            if (word.isNotEmpty()) {
                println("Adding $word")
                radixTree.add(word)
            }

        }
        return@lazy radixTree
    }

    init {
        this.string = string.filter { !it.isWhitespace() }.toLowerCase()
    }

    fun parse() : Iterable<Iterable<KotlinRadixParserResult>> = parse(string, 0, 0).sortedBy { it.countKnownWords() }

    private fun parse(string: String, startIndex: Int, numberOfUnknownWords: Int) : LinkedList<LinkedList<KotlinRadixParserResult>> {
        val results = getKotlinRadixParserResultsForStringStartingAtIndex(string, startIndex)
        val returnVal = LinkedList<LinkedList<KotlinRadixParserResult>>()

        if (results.any()) {
            for (result in results) {
                println((0..startIndex).joinToString("") { " " } + result)

                if (maximumNumberOfUnknownWords != null && !result.wasOnRecord && numberOfUnknownWords + 1 > maximumNumberOfUnknownWords!!) {
                    continue
                }

                val newNumberOfUnknownWords = numberOfUnknownWords + (if (result.wasOnRecord) 0 else 1)
                val subresults = parse(string, startIndex + result.string.length, newNumberOfUnknownWords)

                if (subresults.any()) {
                    for (subresult in subresults) {
                        subresult.addFirst(result)
                    }

                    returnVal.addAll(subresults)
                }
            }
        }
        else if (maximumNumberOfUnknownWords == null || numberOfUnknownWords > (maximumNumberOfUnknownWords ?: Int.MIN_VALUE)) {
            maximumNumberOfUnknownWords = numberOfUnknownWords
        }

        return returnVal
    }

    private fun getKotlinRadixParserResultsForStringStartingAtIndex(string: String, startIndex: Int) : Iterable<KotlinRadixParserResult> {
        // Progressively make longer and longer strings, and only put the ones that are in the KRadixTree into results
        val results = (startIndex..string.length).mapLazy { string.substring(startIndex, it) }
                .filterLazy { !it.isEmpty() && it in radixTree }

        if (results.any())
            return results.sortedByDescending { it.length }.mapLazy { KotlinRadixParserResult(it, true) }

        val list = mutableListOf<KotlinRadixParserResult>()

        for (left in (startIndex + 1)..string.length) {
            for (right in (left + 1)..string.length) {
                var substring = string.substring(left, right)

                if (substring.isEmpty())
                    continue

                if (substring in radixTree) {
                    substring = string.substring(startIndex, left)

                    if (substring.isNotEmpty()) {
                        list.add(KotlinRadixParserResult(substring, false))
                        return list
                    }
                }
            }
        }

        return list
    }
}

private fun Iterable<KotlinRadixParserResult>.countKnownWords() = this.count { it.wasOnRecord }