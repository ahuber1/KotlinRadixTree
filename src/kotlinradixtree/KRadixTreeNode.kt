package kotlinradixtree

import org.testng.annotations.Test
import sun.plugin.dom.exception.InvalidStateException
import java.lang.StringBuilder
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class KRadixTreeNode {

    protected var string: String?
    protected var parent: KRadixTreeNode?
    protected val children = ArrayList<KRadixTreeNode>()

    internal constructor() {
        parent = null
        string = null
    }

    internal constructor(string: String, parent: KRadixTreeNode) {
        this.string = string
        this.parent = parent
    }

    internal fun add(string: String) {
        if (string.isEmpty())
            throw IllegalArgumentException("One cannot add an empty string into a radix tree") // TODO Maybe allow this functionality in the future?
        else if (parent == null)
            addInternal(string)
        else
            throw UnsupportedOperationException("One cannot call add(String) on anything other than the root node")
    }

    private fun addInternal(string: String) {
        if (string.isEmpty())
            return

        val index = indexOfLongestStringInChildren(string)

        if (index == null) {
            val indexDataShouldBeAt = searchAmongChildren(string) as IndexDataShouldBeAt
            children.add(indexDataShouldBeAt.index, KRadixTreeNode(string, this))
        }
        else {
            val match = children[index]

            if (match.string!! == string)
                return // already added

            val result1 = compareStringsWithSharedPrefix(string, match.string!!)
            val result2 = compareStringsWithSharedPrefix(match.string!!, string)
            val originalNode = children[index]
            val neededPrefixExists = originalNode.string!! == result1.prefixStringsShare
            val matchIsLongerThanString = result1.suffixWhereStringsDiffer.isEmpty() && result2.suffixWhereStringsDiffer.isNotEmpty()
            val stringIsLongerThanMatch = result1.suffixWhereStringsDiffer.isNotEmpty() && result2.suffixWhereStringsDiffer.isEmpty()

            if (matchIsLongerThanString && !neededPrefixExists) {
                children.removeAt(index)
                val indexToInsert = searchAmongChildren(result1.prefixStringsShare) as IndexDataShouldBeAt
                val newNode = KRadixTreeNode(result1.prefixStringsShare, this)
                children.add(indexToInsert.index, newNode)
                newNode.addInternal(result2.suffixWhereStringsDiffer)
                for (child in originalNode.children) {
                    child.parent = newNode
                    newNode.children[0].children.add(child)
                }
            }
            else if (stringIsLongerThanMatch) {
                originalNode.addInternal(result1.suffixWhereStringsDiffer)
            }
            else if (neededPrefixExists) {
                originalNode.addInternal(result2.suffixWhereStringsDiffer)
            }
            else {
                children.removeAt(index)
                val indexToInsert = searchAmongChildren(result1.prefixStringsShare) as IndexDataShouldBeAt
                val newNode = KRadixTreeNode(result1.prefixStringsShare, this)
                children.add(indexToInsert.index, newNode)
                newNode.addInternal(result1.suffixWhereStringsDiffer)
                for (child in originalNode.children) {
                    child.parent = newNode
                    newNode.children[0].children.add(child)
                }
                newNode.addInternal(result2.suffixWhereStringsDiffer)
            }
        }
    }

    internal operator fun contains(string: String): Boolean {
        if (string.isEmpty())
            return false

        return containsInternal(string, this)
    }

    private fun containsInternal(string: String, node: KRadixTreeNode) : Boolean {
        if (string.isEmpty())
            return true

        val index = node.indexOfLongestStringInChildren(string) ?: return false // not found
        val result = compareStringsWithSharedPrefix(node.children[index].string!!, string)

        return if (node.children[index].string!! == result.prefixStringsShare)
            containsInternal(result.suffixWhereStringsDiffer, node.children[index])
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

        if (last != null)
            return children.indexOf(last)
        else
            return null
    }

    internal fun remove(string: String) : Boolean {
        if (string.isEmpty()) {
            throw IllegalArgumentException("An empty string cannot be added into a radix tree. " +
                    "As such, an empty string cannot be removed from a radix tree") // TODO Maybe add this functionality in the future?
        }
        else if (parent == null)
            return removeInternal(string, this)
        else
            throw UnsupportedOperationException("You cannot call remove(String) on anything other than the root node")
    }

    private fun removeInternal(string: String, node: KRadixTreeNode) : Boolean {
        if (string.isEmpty())
            return true

        val index = node.indexOfLongestStringInChildren(string) ?: return false // no match found
        val otherNode = node.children[index]
        val result = compareStringsWithSharedPrefix(otherNode.string!!, string)
        val removalWasSuccessful = removeInternal(result.suffixWhereStringsDiffer, otherNode)

        if (!removalWasSuccessful)
            return false


        if (otherNode.string!! == string) {
            node.children.removeAt(index)
            moveChildrenUp(otherNode)
        }
        if (node.parent != null && node.children.count() == 1) {
            val onlyChild = node.children.first()

            node.string = (node.string ?: "") + onlyChild.string!!
            node.children.removeAt(0)

            for (child in onlyChild.children) {
                val i = node.searchAmongChildren(child.string!!)

                if (!i.isInNode()) {
                    node.children.add(i.index, child)
                }

                child.parent = node
            }
        }

        return removalWasSuccessful
    }

    private fun moveChildrenUp(node: KRadixTreeNode) {
        moveChildrenUp(node, "")
    }

    private fun moveChildrenUp(node: KRadixTreeNode, string: String) {
        if (node.children.isEmpty())
            addInternal(string)
        else {
            for (child in node.children) {
                moveChildrenUp(child, string + child.string!!)
            }
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
        val parentString = "\"${parent?.string ?: ""}\""
        val childrenString = "[ ${children.map { it.string }.joinToString(", ")} ]"
        val string = "\"${this.string ?: ""}\""

        return "KRadixTreeNode(string = $string, parent = $parentString, children = $childrenString"
    }

    @Test
    internal fun testBasicInsertion() {
        val strings = arrayOf( "application", "apply", "apple" )
        runTestWithStrings(strings)
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
                                                            println("[ ${stringsInTest.joinToString(", ")} ]")
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


}