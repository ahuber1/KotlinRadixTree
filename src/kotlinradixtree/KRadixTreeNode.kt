package kotlinradixtree

import org.testng.annotations.Test
import sun.plugin.dom.exception.InvalidStateException
import java.lang.StringBuilder
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal open class KRadixTreeNode {

    protected val string: String?
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
            val result2 = compareStringsWithSharedPrefix(match.string, string)
            val originalNode = children[index]

            // The match is longer than the string
            if (result1.suffixWhereStringsDiffer.isEmpty() && result2.suffixWhereStringsDiffer.isNotEmpty()) {
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
            // string is longer than the match
            else if (result1.suffixWhereStringsDiffer.isNotEmpty() && result2.suffixWhereStringsDiffer.isEmpty()) {
                originalNode.addInternal(result1.suffixWhereStringsDiffer)
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
        return containsInternal(result.suffixWhereStringsDiffer, node.children[index])
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
            return removeInternal(string)
        else
            throw UnsupportedOperationException("You cannot call remove(String) on anything other than the root node")
    }

    private fun removeInternal(string: String) : Boolean {
        if (string.isEmpty())
            return true

        val index = indexOfLongestStringInChildren(string) ?: return false // no match found
        val otherNode = children[index]
        val result = compareStringsWithSharedPrefix(otherNode.string!!, string)
        val returnValue = removeInternal(result.suffixWhereStringsDiffer)
        children.removeAt(index)
        moveChildrenUp(otherNode)

        if (returnValue && children.isEmpty())
            parent?.children?.remove(this)

        return returnValue
    }

    private fun moveChildrenUp(node: KRadixTreeNode) {
        moveChildrenUp(node, "")
    }

    private fun moveChildrenUp(node: KRadixTreeNode, string: String) {
        if (node.children.isEmpty())
            add(string)
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

    @Test
    internal fun testInsertion() {
        val strings = arrayOf( "application", "apply", "apple" )
        val root = KRadixTreeNode()
        val stringsProcessedSoFar = ArrayList<String>()

        for (string in strings) {
            root.add(string)
            stringsProcessedSoFar.add(string)

            for (s in stringsProcessedSoFar) {
                assertTrue(s in root)
            }
        }

        for (string in strings) {
            root.remove(string)
            stringsProcessedSoFar.remove(string)
            assertFalse(string in root)

            for (s in stringsProcessedSoFar) {
                assertTrue(s in root)
            }
        }
    }
}