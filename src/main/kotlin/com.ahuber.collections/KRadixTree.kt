package com.ahuber.collections

import com.ahuber.utils.*
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.util.*

class KRadixTree {
    private val root = KRadixTreeNode.Root()
    private var _size = 0
    var size: Int
        get() = _size
        private set(value) { _size = value }

    @Synchronized
    fun add(string: String) {
        root.add(string)
        size++
    }

    @Synchronized
    operator fun contains(string: String) : Boolean = root.contains(string)

    @Synchronized
    operator fun get(string: String) = root.getChildren(string)

    @Synchronized
    fun remove(string: String) : Boolean {
        if (root.remove(string)) {
            size--
            return true
        }

        return false
    }

    private data class KRadixTreeStringComparisonResult(val prefixStringsShare: String,
            val suffixWhereStringsDiffer: String)

    private sealed class KRadixTreeNode {
        protected var children = HashSet<Child>()

        class Root: KRadixTreeNode() {
            fun add(string: String): Child? {
                check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
                    "Invalid String \"$string\": one cannot add strings that contain whitespace characters or uppercase letters."
                }
                return add(this, this, string)
            }

            fun remove(string: String) : Boolean {
                check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
                    "Invalid String \"$string\": one cannot remove strings that contain whitespace characters or uppercase letters."
                }

                // you cannot add empty strings; as such, you cannot remove them
                return when {
                    string.isNotEmpty() -> remove(this, string)
                    else -> false
                }
            }
        }

        data class Child(var string: String, var endOfWord: Boolean): KRadixTreeNode()

        operator fun contains(string: String): Boolean = getChildren(string) != null

        fun getChildren(string: String) = when {
            // empty strings cannot be added into the radix tree
            string.isEmpty() -> null

            // cannot have characters with whitespace or with uppercase letters
            string.toCharArray().any { it.isWhitespace() || it.isUpperCase() } -> null

            else -> getChildren(this, string)
        }

        override fun toString(): String {
            val childrenString = "[ ${children.joinToString(", ") { if (it.endOfWord) "${it.string}*" else it.string }} ]"
            var string = when (this) {
                is Child -> this.string
                else -> ""
            }

            if (this is Child && this.endOfWord) {
                string += "*"
            }

            string = "\"$string\""

            return "KRadixTreeNode(string = $string, children = $childrenString)"
        }

        companion object {
            private val KRadixTreeNode.completeWords: List<String>
                get() {
                    fun Child.getCompleteWordsWorker(builder: String, list: LinkedList<String>) {
                        val str: String by lazy { builder + this.string }

                        if (this.endOfWord)
                            list.add(str)

                        for (child in this.children) {
                            child.getCompleteWordsWorker(str, list)
                        }
                    }

                    val list = LinkedList<String>()

                    for (child in this.children) {
                        child.getCompleteWordsWorker("", list)
                    }

                    return list
                }

            internal fun compareStringsWithSharedPrefix(string1: String, string2: String) : KRadixTreeStringComparisonResult {
                val charBuffLen = maxOf(string1.length, string2.length)
                val buffer1 = string1.toCharArrayWithFill(charBuffLen).asIterable()
                val buffer2 = string2.toCharArrayWithFill(charBuffLen).asIterable()
                val sharedBuffer = Array(buffer1.count()) { '\u0000' }
                val differBuffer = sharedBuffer.copyOf()

                for ((index, pair) in buffer1.zipUneven(buffer2).withIndex()) {
                    pair.first.ifSome { c1 ->
                        pair.second.ifSome { c2 ->
                            sharedBuffer[index] = if (c1 == c2) c1 else Char.NULL_CHARACTER
                            differBuffer[index] = if (c1 == c2) Char.NULL_CHARACTER else c2
                        }
                    }
                }

                val trimmedSharedBuffer  = sharedBuffer.filter { it != '\u0000' }
                val trimmedDifferBuffer = differBuffer.filter { it != '\u0000' }
                val shareString = String(trimmedSharedBuffer.toCharArray())
                val differString = String(trimmedDifferBuffer.toCharArray())

                return KRadixTreeStringComparisonResult(shareString, differString)
            }

            private fun getChildren(node: KRadixTreeNode, str: String) : Sequence<String>? {
                val child = node.getChildWithLongestSharedPrefix(str) ?: return null // not found
                val result = compareStringsWithSharedPrefix(child.string, str)

                if (child.string != result.prefixStringsShare) {
                    return null
                }

                if (result.suffixWhereStringsDiffer.isNotEmpty()) {
                    return getChildren(child, result.suffixWhereStringsDiffer)
                }

                return when {
                    child.endOfWord -> child.children.map { it.string }.asSequence()
                    else -> null
                }
            }

            // First - Removal was successful/unsuccessful
            // Second - Collapse was performed
            private fun remove(node: KRadixTreeNode, str: String) : Boolean {
                if (str.isEmpty()) {
                    return true
                }

                val otherNode = node.getChildWithLongestSharedPrefix(str) ?: return false // no match found
                val result = compareStringsWithSharedPrefix(otherNode.string, str)
                val removalWasSuccessful = remove(otherNode, result.suffixWhereStringsDiffer)

                if (!removalWasSuccessful) {
                    return removalWasSuccessful
                }

                if (otherNode.string == str) {
                    if (otherNode.children.isEmpty())
                        node.children.remove(otherNode)
                    else
                        otherNode.endOfWord = false
                }

                if (otherNode.children.size == 1 && !otherNode.endOfWord && otherNode.children.first().endOfWord) {
                    otherNode combineWith otherNode.children.first()
                }
                // If node is not the root, otherNode is still one of node's children, and if node is not at the end of a
                // word, but otherNode is, collapse otherNode into node
                if (node is Child && node.children.size == 1 && !node.endOfWord && node.children.first().endOfWord) {
                    node combineWith node.children.first()
                }

                return true
            }

            private fun split(root: Root, node: KRadixTreeNode, match: Child,
                    resultWithEmptySuffix: KRadixTreeStringComparisonResult,
                    stringBeingAdded: String): Child? {
                val words = match.completeWords
                node.children.remove(match)
                val newChild = Child(resultWithEmptySuffix.prefixStringsShare, false)
                node.children.add(newChild)

                for (word in words) {
                    add(root, newChild, word)
                }

                return add(root, newChild, resultWithEmptySuffix.suffixWhereStringsDiffer)
            }

            private infix fun Child.combineWith(other: Child) {
                this.children = other.children
                this.endOfWord = other.endOfWord
                this.string = this.string + other.string
            }

            private fun KRadixTreeNode.getChildWithLongestSharedPrefix(string: String): Child? {
                var index = 0
                var searchResult: KRadixTreeNode?
                var match: Child? = null
                val builder = StringBuilder()

                do {
                    builder.append(string[index++])
                    searchResult = this.children.find { it.string.startsWith(builder) }

                    if (searchResult != null) {
                        match = searchResult
                    }

                }  while(builder.length < string.length && searchResult != null)

                return match
            }

            private fun add(root: Root, node: KRadixTreeNode, string: String): Child? {
                if (string.isEmpty()) {
                    return null
                }

                val match = node.getChildWithLongestSharedPrefix(string)

                if (match == null) {
                    val newNode = Child(string, true)
                    node.children.add(newNode)
                    return newNode
                }

                if (match.string == string) {
                    match.endOfWord = true
                    return match
                }

                val resultWithCharsInMatch =
                        compareStringsWithSharedPrefix(string, match.string)
                val resultWithCharsInString =
                        compareStringsWithSharedPrefix(match.string, string)

                if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty() &&
                        resultWithCharsInString.suffixWhereStringsDiffer.isNotEmpty()) {
                    return add(root, match, resultWithCharsInString.suffixWhereStringsDiffer)
                }

                return split(root, node, match, resultWithCharsInString, string)
            }
        }
    }

    @Suppress("FunctionName")
    @Test
    fun `test KRadixTreeStringCompare`() {
        var string1 = "table"
        var string2 = "tables"
        var expectedSimilarPrefix = "table"
        var expectedDissimilarSuffix = "s"
        var result = KRadixTreeNode.compareStringsWithSharedPrefix(string1, string2)
        assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
        assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)

        string1 = "tables"
        string2 = "table"
        expectedSimilarPrefix = "table"
        expectedDissimilarSuffix = ""
        result = KRadixTreeNode.compareStringsWithSharedPrefix(string1, string2)

        assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
        assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)

        string1 = "table"
        string2 = "table"
        expectedSimilarPrefix = "table"
        expectedDissimilarSuffix = ""
        result = KRadixTreeNode.compareStringsWithSharedPrefix(string1, string2)

        assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
        assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)
    }
}