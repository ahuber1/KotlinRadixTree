package kotlinradixtree

import org.testng.annotations.Test
import kotlin.test.assertEquals

internal data class KRadixTreeStringComparisonResult(val prefixStringsShare: String,
                                                        val suffixWhereStringsDiffer: String)

internal fun compareStringsWithSharedPrefix(string1: String, string2: String) : KRadixTreeStringComparisonResult {
    val charBuffLen = maxOf(string1.length, string2.length)
    val buffer1 = string1.toCharArrayWithFill(charBuffLen).asIterable()
    val buffer2 = string2.toCharArrayWithFill(charBuffLen).asIterable()
    val sharedBuffer = Array(buffer1.count(), { '\u0000' } )
    val differBuffer = sharedBuffer.copyOf()

    for ((index, pair) in (buffer1 iterateSimultaneouslyWith buffer2).withIndex()) {
        val (c1, c2) = pair
        sharedBuffer[index] = if (c1 == c2) c1 else '\u0000'
        differBuffer[index] = if (c1 == c2) '\u0000' else c2
    }

    val trimmedSharedBuffer  = sharedBuffer.filter { it != '\u0000' }
    val trimmedDifferBuffer = differBuffer.filter { it != '\u0000' }
    val shareString = String(trimmedSharedBuffer.toCharArray())
    val differString = String(trimmedDifferBuffer.toCharArray())

    return KRadixTreeStringComparisonResult(shareString, differString)
}

@Test
fun `Test KRadixTreeStringCompare`() {
    var string1 = "table"
    var string2 = "tables"
    var expectedSimilarPrefix = "table"
    var expectedDissimilarSuffix = "s"
    var result = compareStringsWithSharedPrefix(string1, string2)

    assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
    assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)

    string1 = "tables"
    string2 = "table"
    expectedSimilarPrefix = "table"
    expectedDissimilarSuffix = ""
    result = compareStringsWithSharedPrefix(string1, string2)

    assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
    assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)

    string1 = "table"
    string2 = "table"
    expectedSimilarPrefix = "table"
    expectedDissimilarSuffix = ""
    result = compareStringsWithSharedPrefix(string1, string2)

    assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
    assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)
}