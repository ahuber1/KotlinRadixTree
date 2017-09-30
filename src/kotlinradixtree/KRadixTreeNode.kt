package kotlinradixtree

import org.testng.annotations.Test
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class KRadixTreeNode {

    private var string: String?
    private var endOfWord: Boolean
    private val children = ArrayList<KRadixTreeNode>()

    internal constructor() {
        string = null
        endOfWord = false
    }

    internal constructor(string: String, endOfWord: Boolean) {
        this.string = string
        this.endOfWord = endOfWord
    }

    internal fun add(string: String) {
        when {
            string.isEmpty() -> throw IllegalArgumentException("One cannot add an empty string into a radix tree") // TODO Maybe allow this functionality in the future?
            this.string == null -> add(this, string)
            else -> throw UnsupportedOperationException("One cannot call add(String) on anything other than the root node")
        }
    }

    internal operator fun contains(string: String): Boolean {
        if (string.isEmpty())
            return false

        return contains(this, string)
    }

    internal fun remove(str: String) : Boolean {
        return if (str.isEmpty()) {
            throw IllegalArgumentException("An empty string cannot be added into a radix tree. " +
                    "As such, an empty string cannot be removed from a radix tree") // TODO Maybe add this functionality in the future?
        }
        else if (string == null)
            remove(this, str).first
        else
            throw UnsupportedOperationException("You cannot call remove(String) on anything other than the root node")
    }

    companion object {
        private fun add(node: KRadixTreeNode, string: String): KRadixTreeNode? {
            if (string.isEmpty())
                return null

            val index = indexOfLongestStringInChildren(node, string)

            if (index == null) {
                val indexDataShouldBeAt = searchAmongChildren(node, string) as IndexDataShouldBeAt
                val newNode = KRadixTreeNode(string, true)
                node.children.add(indexDataShouldBeAt.index, newNode)
                return newNode
            }
            else {
                val match = node.children[index]

                if (match.string!! == string)
                    return match

                val resultWithCharsInMatch = compareStringsWithSharedPrefix(string, match.string!!)
                val resultWithCharsInString = compareStringsWithSharedPrefix(match.string!!, string)

                return if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty() &&
                        resultWithCharsInString.suffixWhereStringsDiffer.isEmpty()) {
                    println("$string: Case 1")
                    match
                } else if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty()) {
                    println("$string: Case 2")
                    add(match, resultWithCharsInString.suffixWhereStringsDiffer)
                } else if (resultWithCharsInString.suffixWhereStringsDiffer.isEmpty()) {
                    println("$string: Case 3")
                    split(node, index, resultWithCharsInString, string)
                } else {
                    println("$string: Case 4")
                    split(node, index, resultWithCharsInMatch, string)
                }
            }
        }

        private fun collapse(node: KRadixTreeNode) {
            val onlyChild = node.children.first()
            val words = gatherWords(onlyChild)
            node.string += onlyChild.string
            node.children.removeAt(0)

            for (word in words) {
                add(node, word)
            }
        }

        private fun contains(node: KRadixTreeNode, str: String) : Boolean {
            val index = indexOfLongestStringInChildren(node, str) ?: return false // not found
            val result = compareStringsWithSharedPrefix(node.children[index].string!!, str)

            if (node.children[index].string!! == result.prefixStringsShare) {
                if (result.suffixWhereStringsDiffer.isEmpty())
                    return node.children[index].endOfWord

                return contains(node.children[index], result.suffixWhereStringsDiffer)
            }

            return false
        }

        private fun indexOfLongestStringInChildren(node: KRadixTreeNode, string: String) : Int? {
            if (string.isEmpty())
                throw IllegalArgumentException("string cannot be empty")

            var index = 0
            val matches = LinkedList<KRadixTreeNode?>()
            val builder = StringBuilder()

            do {
                builder.append(string[index++])
                matches.addLast(node.children.find { it.string!!.startsWith(builder) })
            }  while(builder.length < string.length && matches.last != null)

            val last = matches.findLast { it != null }

            return if (last != null)
                node.children.indexOf(last)
            else
                null
        }

        private fun gatherWords(node: KRadixTreeNode) : ArrayList<String> {
            val list = ArrayList<String>()

            for (child in node.children) {
                gatherWordsWorker(child, "", list)
            }

            return list
        }

        private fun gatherWordsWorker(node: KRadixTreeNode, builder: String, list: ArrayList<String>) {
            val str = builder + node.string!!
            list.add(str)

            for (child in node.children) {
                gatherWordsWorker(child, str, list)
            }
        }

        // First - Removal was successful/unsuccessful
        // Second - Collapse was performed
        private fun remove(node: KRadixTreeNode, str: String) : Pair<Boolean, Boolean> {
            if (str.isEmpty())
                return Pair(true, false)

            val index = indexOfLongestStringInChildren(node, str) ?: return Pair(false, false) // no match found
            val otherNode = node.children[index]
            val result = compareStringsWithSharedPrefix(otherNode.string!!, str)
            var (removalWasSuccessful, collapseWasPerformed) = remove(otherNode, result.suffixWhereStringsDiffer)

            if (!removalWasSuccessful)
                return Pair(removalWasSuccessful, collapseWasPerformed)

            if (otherNode.string!! == str && otherNode.children.isEmpty()) {
                node.children.removeAt(index)
            }
            if (!collapseWasPerformed && otherNode.string != null && otherNode.children.count() == 1) {
                collapse(otherNode)
                collapseWasPerformed = true
            }
            if (!collapseWasPerformed && node.string != null && node.children.count() == 1) {
                collapse(node)
                collapseWasPerformed = true
            }

            return Pair(removalWasSuccessful, collapseWasPerformed)
        }

        private fun searchAmongChildren(node: KRadixTreeNode, string: String) : KRadixTreeNodeIndex {
            return if (node.children.isEmpty())
                IndexDataShouldBeAt(0)
            else
                searchAmongChildren(node, string, 0, node.children.lastIndex / 2, node.children.lastIndex)
        }

        private fun searchAmongChildren(node: KRadixTreeNode, string: String, startIndex: Int, middleIndex: Int, endIndex: Int) : KRadixTreeNodeIndex {
            val middleString = node.children[middleIndex].string!!

            return when {
                string == middleString -> IndexDataWasFound(middleIndex)
                endIndex - startIndex == 0 -> middleIndex.indexDataShouldBeAt(string < middleString)
                else -> {
                    val newStartIndex: Int
                    val newEndIndex: Int

                    if (string < middleString) {
                        newStartIndex = startIndex
                        newEndIndex = middleIndex
                    }
                    else {
                        newStartIndex = middleIndex + 1
                        newEndIndex = endIndex
                    }

                    val newMiddleIndex = ((newEndIndex - newStartIndex) / 2) + newStartIndex

                    return searchAmongChildren(node, string, newStartIndex, newMiddleIndex, newEndIndex)
                }
            }
        }

        private fun split(node: KRadixTreeNode, index: Int, resultWithEmptySuffix: KRadixTreeStringComparisonResult,
                          stringBeingAdded: String): KRadixTreeNode? {
            val words = gatherWords(node)
            node.children.removeAt(index)
            add(node, resultWithEmptySuffix.prefixStringsShare)!!

            for (word in words) {
                add(node, word)
            }

            return add(node, stringBeingAdded)
        }
    }

    override fun toString(): String {
        val childrenString = "[ ${children.map { if (it.endOfWord) "${it.string!!}*" else it.string!! }.joinToString(", ")} ]"
        var string = this.string ?: ""

        if (this.endOfWord)
            string += "*"

        string = "\"$string\""

        return "KRadixTreeNode(string = $string, children = $childrenString)"
    }

    @Test
    internal fun testBasicInsertion() {
        val strings = arrayOf( "application", "apply", "apple" )
        runTestWithStrings(strings)
    }

    @Test
    internal fun foo() {
        runTestWithStrings(arrayOf("application", "application", "application", "application", "application",
                "application", "application", "application", "application", "application", "band",
                "bandana", "bands" ))
    }

    @Test
    internal fun testComplexInsertion() {
        val strings = arrayOf( "application", "apple", "apply", "band", "bandana", "bands", "ban", "applications",
                "apples", "applies", "bands", "bandanas", "bans" )

        for (s1 in strings) {
            for (s2 in strings) {
                for (s3 in strings) {
                    for (s4 in strings) {
                        for (s5 in strings) {
                            for (s6 in strings) {
                                for (s7 in strings) {
                                    for (s8 in strings) {
                                        for (s9 in strings) {
                                            for (s10 in strings) {
                                                for (s11 in strings) {
                                                    for (s12 in strings) {
                                                        for (s13 in strings) {
                                                            val stringsInTest = arrayOf(s1, s2, s3, s4, s5, s6, s7, s8,
                                                                    s9, s10, s11, s12, s13 )
                                                            println("=================================================")
                                                            println("RUNNING TEST WITH THE FOLLOWING ITEMS")
                                                            println("[ ${stringsInTest.map{ "\"$it\"" }.joinToString(", ")} ]")
                                                            println("=================================================")
                                                            runTestWithStrings(stringsInTest)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun runTestWithStrings(strings: Array<String>) {
        val root = KRadixTreeNode()
        val stringsProcessedSoFar = ArrayList<String>()

        for (string in strings) {
            print("Adding $string...")
            root.add(string)
            println("$string added!")
            stringsProcessedSoFar.add(string)

            for (s in stringsProcessedSoFar) {
                print("\tAsserting that $s is in the node...")

                assertTrue(s in root)
                println("$s is in the node!")
            }
        }

        while (stringsProcessedSoFar.isNotEmpty()) {
            val string = stringsProcessedSoFar.first()

            print("Removing $string from the node...")
            root.remove(string)
            println("$string removed from the node!")
            stringsProcessedSoFar.removeAll { it == string }

            print("Asserting that $string is no longer in the node...")
            assertFalse(string in root)
            println("$string is not in root!")

            for (s in stringsProcessedSoFar) {
                print("\tAsserting that $s is still in the node...")
                assertTrue(s in root)
                println("$s is still in the node!")
            }
        }
    }
}