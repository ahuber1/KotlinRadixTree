package kotlinradixtree

import org.testng.annotations.Test
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class KRadixTreeNode {

    protected var string: String?
    //protected var parent: KRadixTreeNode?
    protected val children = ArrayList<KRadixTreeNode>()

    internal constructor() {
        string = null
    }

    internal constructor(string: String) {
        this.string = string
    }

    internal fun add(string: String) {
        when {
            string.isEmpty() -> throw IllegalArgumentException("One cannot add an empty string into a radix tree") // TODO Maybe allow this functionality in the future?
            this.string == null -> addInternal(string)
            else -> throw UnsupportedOperationException("One cannot call add(String) on anything other than the root node")
        }
    }

    private fun addInternal(string: String): KRadixTreeNode? {
        if (string.isEmpty())
            return null

        val index = indexOfLongestStringInChildren(string)

        if (index == null) {
            val indexDataShouldBeAt = searchAmongChildren(string) as IndexDataShouldBeAt
            val newNode = KRadixTreeNode(string)
            children.add(indexDataShouldBeAt.index, newNode)
            return newNode
        }
        else {
            val match = children[index]

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
                match.addInternal(resultWithCharsInString.suffixWhereStringsDiffer)
            } else if (resultWithCharsInString.suffixWhereStringsDiffer.isEmpty()) {
                println("$string: Case 3")
                split(index, resultWithCharsInString, string)
            } else {
                println("$string: Case 4")
                split(index, resultWithCharsInMatch, string)
            }
        }
    }

    private fun split(index: Int, resultWithEmptySuffix: KRadixTreeStringComparisonResult,
                      stringBeingAdded: String): KRadixTreeNode? {
        val words = gatherWords()
        children.removeAt(index)
        addInternal(resultWithEmptySuffix.prefixStringsShare)!!

        for (word in words) {
            addInternal(word)
        }

        return addInternal(stringBeingAdded)
    }

    internal operator fun contains(string: String): Boolean {
        if (string.isEmpty())
            return false

        return containsInternal(string)
    }

    private fun containsInternal(str: String) : Boolean {
        if (str.isEmpty())
            return true

        val index = indexOfLongestStringInChildren(str) ?: return false // not found
        val result = compareStringsWithSharedPrefix(children[index].string!!, str)

        return if (children[index].string!! == result.prefixStringsShare)
            children[index].containsInternal(result.suffixWhereStringsDiffer)
        else
            false
    }

    internal fun indexOfLongestStringInChildren(string: String) : Int? {
        if (string.isEmpty())
            throw IllegalArgumentException("string cannot be empty")

        var index = 0
        val matches = LinkedList<KRadixTreeNode?>()
        val builder = StringBuilder()

        do {
            builder.append(string[index++])
            matches.addLast(children.find { it.string!!.startsWith(builder) })
        }  while(builder.length < string.length && matches.last != null)

        val last = matches.findLast { it != null }

        return if (last != null)
            children.indexOf(last)
        else
            null
    }

    internal fun remove(str: String) : Boolean {
        return if (str.isEmpty()) {
            throw IllegalArgumentException("An empty string cannot be added into a radix tree. " +
                    "As such, an empty string cannot be removed from a radix tree") // TODO Maybe add this functionality in the future?
        }
        else if (string == null)
            removeInternal(str).first
        else
            throw UnsupportedOperationException("You cannot call remove(String) on anything other than the root node")
    }

    // First - Removal was successful/unsuccessful
    // Second - Collapse was performed
    private fun removeInternal(str: String) : Pair<Boolean, Boolean> {
        if (str.isEmpty())
            return Pair(true, false)

        val index = indexOfLongestStringInChildren(str) ?: return Pair(false, false) // no match found
        val otherNode = children[index]
        val result = compareStringsWithSharedPrefix(otherNode.string!!, str)
        var (removalWasSuccessful, collapseWasPerformed) = otherNode.removeInternal(result.suffixWhereStringsDiffer)

        if (!removalWasSuccessful)
            return Pair(removalWasSuccessful, collapseWasPerformed)

        if (otherNode.string!! == str && otherNode.children.isEmpty()) {
            children.removeAt(index)
        }
        if (!collapseWasPerformed && otherNode.string != null && otherNode.children.count() == 1) {
            otherNode.collapse()
            collapseWasPerformed = true
        }
        if (!collapseWasPerformed && string != null && children.count() == 1) {
            collapse()
            collapseWasPerformed = true
        }

        return Pair(removalWasSuccessful, collapseWasPerformed)
    }

    private fun collapse() {
        val onlyChild = children.first()
        val words = onlyChild.gatherWords()
        string += onlyChild.string
        children.removeAt(0)

        for (word in words) {
            addInternal(word)
        }
    }

    private fun gatherWords() : Iterable<String> = children.map {
        val list = ArrayList<String>()
        it.gatherWords("", list)
        return list
    }.flatten()

    private fun gatherWords(builder: String, list: ArrayList<String>) {
        val str = builder + string!!
        list.add(str)

        if (children.isEmpty()) {
            return
        }

        for (child in children) {
            child.gatherWords(str, list)
        }
    }

    private fun searchAmongChildren(string: String) : KRadixTreeNodeIndex {
        return if (children.isEmpty())
            IndexDataShouldBeAt(0)
        else
            search(string, 0, children.lastIndex / 2, children.lastIndex)
    }

    private fun search(string: String, startIndex: Int, middleIndex: Int, endIndex: Int) : KRadixTreeNodeIndex {
        val middleString = children[middleIndex].string!!

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

                return search(string, newStartIndex, newMiddleIndex, newEndIndex)
            }
        }
    }

    override fun toString(): String {
        val childrenString = "[ ${children.map { it.string }.joinToString(", ")} ]"
        val string = "\"${this.string ?: ""}\""

        return "KRadixTreeNode(string = $string, children = $childrenString"
    }

    @Test
    internal fun testBasicInsertion() {
        val strings = arrayOf( "application", "apply", "apple" )
        runTestWithStrings(strings)
    }

    @Test
    internal fun foo() {
        runTestWithStrings(arrayOf("application", "application", "application", "application", "application",
                "application", "application", "application", "application", "application", "application",
                "applications", "apple" ))
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
            if (string == "apple")
                println("Here we go!")

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