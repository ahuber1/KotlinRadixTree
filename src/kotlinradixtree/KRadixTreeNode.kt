package kotlinradixtree

import org.testng.annotations.Test
import java.io.File
import java.lang.StringBuilder
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class KRadixTreeNode {

    private var string: String?
    private var endOfWord: Boolean
    private var children = HashSet<KRadixTreeNode>()

    internal constructor() {
        string = null
        endOfWord = false
    }

    internal constructor(string: String, endOfWord: Boolean) {
        this.string = string
        this.endOfWord = endOfWord
    }

    internal fun add(string: String) {
        check(this.string == null) { "One cannot call add(String) on anything other than the root node" }
        check(string.isNotEmpty()) { "Invalid String \"$string\": one cannot add an empty string into a radix tree" }
        check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
            "Invalid String \"$string\": one cannot add strings that contain whitespace characters or uppercase letters."
        }
        add(this, string)
    }

    internal fun childrenAreEmpty() = children.isEmpty()

    internal operator fun contains(string: String): Boolean {
        if (string.isEmpty())
            return false // empty strings cannot be added into the radix tree
        if (string.toCharArray().any { it.isWhitespace() || it.isUpperCase() } )
            return false // cannot have characters with whitespace or with uppercase letters

        return contains(this, string)
    }

    internal fun remove(string: String) : Boolean {
        check(this.string == null) { "You cannot call remove(String) on anything other than the root node" }
        check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
            "Invalid String \"$string\": one cannot remove strings that contain whitespace characters or uppercase letters."
        }

        return if (string.isNotEmpty()) remove(this, string) else false // you cannot add empty strings; as such, you cannot remove them
    }

    override fun toString(): String {
        val childrenString = "[ ${children.map { if (it.endOfWord) "${it.string!!}*" else it.string!! }.joinToString(", ")} ]"
        var string = this.string ?: ""

        if (this.endOfWord)
            string += "*"

        string = "\"$string\""

        return "KRadixTreeNode(string = $string, children = $childrenString)"
    }

    companion object {
        private fun add(node: KRadixTreeNode, string: String): KRadixTreeNode? {
            if (string.isEmpty())
                return null

            val match = getChildContainingLongestSharedPrefix(node, string)

            if (match == null) {
                val newNode = KRadixTreeNode(string, true)
                node.children.add(newNode)
                return newNode
            }
            else {
                if (match.string!! == string) {
                    match.endOfWord = true
                    return match
                }

                val resultWithCharsInMatch = compareStringsWithSharedPrefix(string, match.string!!)
                val resultWithCharsInString = compareStringsWithSharedPrefix(match.string!!, string)

                return if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty() &&
                        resultWithCharsInString.suffixWhereStringsDiffer.isEmpty()) {
                    //println("Case 1")
                    match
                } else if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty()) {
                    //println("Case 2")
                    add(match, resultWithCharsInString.suffixWhereStringsDiffer)
                } else if (resultWithCharsInString.suffixWhereStringsDiffer.isNotEmpty()) {
                    //println("Case 3")
                    split(node, match, resultWithCharsInString, string)
                } else {
                    //println("Case 4")
                    split(node, match, resultWithCharsInMatch, string)
                }
            }
        }

        private fun combine(node: KRadixTreeNode, with: KRadixTreeNode) {
            node.children = with.children
            node.endOfWord = with.endOfWord
            node.string = node.string!! + with.string
        }

        private fun contains(node: KRadixTreeNode, str: String) : Boolean {
            val child = getChildContainingLongestSharedPrefix(node, str) ?: return false // not found
            val result = compareStringsWithSharedPrefix(child.string!!, str)

            if (child.string!! == result.prefixStringsShare) {
                if (result.suffixWhereStringsDiffer.isEmpty())
                    return child.endOfWord

                return contains(child, result.suffixWhereStringsDiffer)
            }

            return false
        }

        private fun getChildContainingLongestSharedPrefix(node: KRadixTreeNode, string: String) : KRadixTreeNode? {
            var index = 0
            var searchResult: KRadixTreeNode? = null
            var match: KRadixTreeNode? = null
            val builder = StringBuilder()

            do {
                builder.append(string[index++])
                searchResult = node.children.find { it.string!!.startsWith(builder) }

                if (searchResult != null)
                    match = searchResult

            }  while(builder.length < string.length && searchResult != null)

            return match
        }

        private fun getCompleteWords(node: KRadixTreeNode) : List<String> {
            val list = LinkedList<String>()

            for (child in node.children) {
                getCompleteWordsWorker(child, "", list)
            }

            return list
        }

        private fun getCompleteWordsWorker(node: KRadixTreeNode, builder: String, list: LinkedList<String>) {
            val str: String by lazy { builder + node.string!! }

            if (node.endOfWord)
                list.add(str)

            for (child in node.children) {
                getCompleteWordsWorker(child, str, list)
            }
        }

        // First - Removal was successful/unsuccessful
        // Second - Collapse was performed
        private fun remove(node: KRadixTreeNode, str: String) : Boolean {
            if (str.isEmpty())
                return true

            val otherNode = getChildContainingLongestSharedPrefix(node, str) ?: return false // no match found
            val result = compareStringsWithSharedPrefix(otherNode.string!!, str)
            val removalWasSuccessful = remove(otherNode, result.suffixWhereStringsDiffer)

            if (!removalWasSuccessful)
                return removalWasSuccessful

            if (otherNode.string!! == str) {
                if (otherNode.children.isEmpty())
                    node.children.remove(otherNode)
                else
                    otherNode.endOfWord = false
            }
            if (otherNode.children.size == 1 && !otherNode.endOfWord && otherNode.children.first().endOfWord) {
                combine(otherNode, otherNode.children.first())
            }
            // If node is not the root, otherNode is still one of node's children, and if node is not at the end of a
            // word, but otherNode is, collapse otherNode into node
            if (node.string != null && node.children.size == 1 && !node.endOfWord && node.children.first().endOfWord) {
                combine(node, node.children.first())
            }

            return true
        }

        private fun split(node: KRadixTreeNode, match: KRadixTreeNode, resultWithEmptySuffix: KRadixTreeStringComparisonResult,
                          stringBeingAdded: String): KRadixTreeNode? {
            val words = getCompleteWords(node)
            node.children.remove(match)
            add(node, resultWithEmptySuffix.prefixStringsShare)!!.endOfWord = false

            for (word in words) {
                add(node, word)
            }

            return add(node, stringBeingAdded)
        }
    }
}