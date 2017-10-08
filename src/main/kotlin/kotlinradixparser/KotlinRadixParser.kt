package kotlinradixparser

import java.io.File
import kotlinradixtree.KRadixTree
import kotlinradixtree.readFileLazily
import org.testng.annotations.Test
import java.util.*

data class KotlinRadixParserResult(val string: String, val wasOnRecord: Boolean)

fun main(args: Array<String>) {
    for (list in ".the quick brown fox jumped over the lazy sheep dog".radixParse()) {
        println("Result ===========================================================")
        for (kotlinRadixParserResult in list) {
            println(kotlinRadixParserResult)
            Thread.sleep(1500)
        }
        println("==================================================================")
    }
}

fun String.radixParse() : Iterable<Iterable<KotlinRadixParserResult>> {
    println("Formatting string...")
    val string = this.filter { !it.isWhitespace() }
    println("Loading Radix Tree...")
    val radixTree = KRadixTree()
    File("test_files/words.txt").readFileLazily {
        val word = it.trim().toLowerCase()

        if (word.isNotEmpty()) {
            println("Adding $word")
            radixTree.add(word)
        }
    }
    //"the quick brown fox jumped over the lazy sheep dog".split(' ').flatMap { it.asIterable() }.map { it.toString() }.forEach { radixTree.add(it) }
    return radixParse(string, 0, radixTree)
}

private fun radixParse(string: String, startIndex: Int, radixTree: KRadixTree) : MutableList<MutableList<KotlinRadixParserResult>> {
    val returnVal = mutableListOf<MutableList<KotlinRadixParserResult>>()
    val results = getKotlinRadixParserResultsForStringStartingAtIndex(string, startIndex, radixTree)

    for (result in results) {
        val subresults = radixParse(string, startIndex + result.string.length, radixTree)

        if (subresults.isNotEmpty()) {
            for (subresult in subresults) {
                subresult.add(0, result)
            }

            returnVal.addAll(subresults)
        }
        else {
            returnVal.add(mutableListOf(result))
        }
    }

    return returnVal
}

private fun getKotlinRadixParserResultsForStringStartingAtIndex(string: String, startIndex: Int, radixTree: KRadixTree) : MutableList<KotlinRadixParserResult> {
    val results = mutableListOf<KotlinRadixParserResult>()
    
    // Progressively make longer and longer strings, and only put the ones that are in the KRadixTree into results
    (startIndex..string.length).map { string.substring(startIndex, it) }
            .filter { !it.isEmpty() && it in radixTree }
            .mapTo(results) { KotlinRadixParserResult(it, true) }

    if (results.isNotEmpty())
        return results

    for (left in (startIndex + 1)..string.length) {
        for (right in (left + 1)..string.length) {
            var substring = string.substring(left, right)

            if (substring.isEmpty())
                continue

            if (substring in radixTree) {
               substring = string.substring(startIndex, left)

                if (substring.isNotEmpty())
                    results.add(KotlinRadixParserResult(substring, false))

                return results
            }
        }
    }

    return results
}

