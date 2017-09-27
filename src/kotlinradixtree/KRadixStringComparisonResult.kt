package kotlinradixtree

import org.testng.annotations.Test
import kotlin.test.assertEquals

internal data class KRadixTreeStringComparisonResult(val prefixStringsShare: String,
                                                        val suffixWhereStringsDiffer: String)

internal fun compareStringsWithSharedPrefix(string1: String, string2: String) : KRadixTreeStringComparisonResult {
    val charBuffLen = maxOf(string1.length, string2.length)
    val buffer1 = string1.toCharArrayWithFill(charBuffLen).map { it.toInt() }
    val buffer2 = string2.toCharArrayWithFill(charBuffLen).map { it.toInt() }
    val sharedBuffer = Array(buffer1.count(), { 0 } )
    val differBuffer = sharedBuffer.copyOf()

    for ((index, pair) in (buffer1 iterateSimultaneouslyWith buffer2).withIndex()) {
        sharedBuffer[index]  = pair.first and pair.second
        differBuffer[index] = pair.first xor pair.second
    }

    val trimmedSharedBuffer  = sharedBuffer.filter { it != 0 }.map { it.toChar() }
    val trimmedDifferBuffer = differBuffer.filter { it != 0 }.map { it.toChar() }
    val shareString = String(trimmedSharedBuffer.toCharArray())
    val differString = String(trimmedDifferBuffer.toCharArray())

    return KRadixTreeStringComparisonResult(shareString, differString)
}

@Test fun `Test KRadixTreeStringCompare`() {
    val string1 = "table"
    val string2 = "tables"
    val expectedSimilarPrefix = "table"
    val expectedDissimilarSuffix = "s"
    val result = compareStringsWithSharedPrefix(string1, string2)

    assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
    assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)
}