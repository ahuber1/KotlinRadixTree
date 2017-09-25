package kotlinradixtree

import com.sun.org.apache.xpath.internal.operations.Bool
import sun.plugin.dom.exception.InvalidStateException

internal class KRadixTreeNode {

    private val string: String?
    private val parent: KRadixTreeNode?
    private val children = ArrayList<KRadixTreeNode>()

    internal constructor() {
        parent = null
        string = null
    }

    internal constructor(string: String, parent: KRadixTreeNode) {
        this.string = string
        this.parent = parent
    }

    internal fun add(string: String) {
        if (parent == null && string == null)
            addInternal(string)
        else
            throw UnsupportedOperationException("You cannot call add(String) on anything other than the root node")
    }

    private fun addInternal(string: String) {

        if (string.isEmpty())
            return
        // If any of the children contain string
        if (children.any { it.string!! == string } )
            return

        var iterable: Iterable<Int>? = null
        var longer = true

        val matchIndiciesLonger = children.indiciesThatMatchPredicate { it.string!!.startsWith(string) }

        if (matchIndiciesLonger.any())
            iterable = matchIndiciesLonger
        else {
            val matchIndiciesShorter = children.indiciesThatMatchPredicate { string.startsWith(it.string!!) }

            if (matchIndiciesShorter.any())
                iterable = matchIndiciesShorter

            longer = false
        }

        if (iterable != null) {
            val iterator = iterable.iterator()
            val index = iterator.next()

            when {
                iterator.hasNext() -> throw InvalidStateException("foo")
                longer -> addLongerString(string, index)
                else -> addShorterString(string, index)
            }
        }
        else {
            children.add(searchAmongChildren(string).index, KRadixTreeNode(string, this))
        }
    }

    private fun addLongerString(longerString: String, indexOfShorterString: Int) {
        val shorterString = children[indexOfShorterString].string!!
        val comparisonResult = shorterString kRadixTreeStringCompare longerString
        children[indexOfShorterString].addInternal(comparisonResult.prefixStringsShare)
    }

    private fun addShorterString(shorterString: String, indexOfLongerString: Int) {
        val longerString = children[indexOfLongerString].string!!
        val comparisonResult = shorterString kRadixTreeStringCompare longerString

        children.removeAt(indexOfLongerString)

        val index = searchAmongChildren(shorterString) // This will always be an IndexDataShouldBeAt
        val newNode = KRadixTreeNode(comparisonResult.prefixStringsShare, this)
        children.add(index.index, newNode)
        newNode.addInternal(comparisonResult.suffixWhereStringsDiffer)
    }

    internal fun remove(string: String) : Boolean {
        if (parent == null && string == null)
            return removeInternal(string)
        else
            throw UnsupportedOperationException("You cannot call remove(String) on anything other than the root node")
    }

    private fun removeInternal(string: String) : Boolean {
        if (string.isEmpty())
            return true

        var iterable: Iterable<Int>? = null
        var longer = true

        val matchIndiciesLonger = children.indiciesThatMatchPredicate { it.string!!.startsWith(string) }

        if (matchIndiciesLonger.any())
            iterable = matchIndiciesLonger
        else {
            val matchIndiciesShorter = children.indiciesThatMatchPredicate { string.startsWith(it.string!!) }

            if (matchIndiciesShorter.any())
                iterable = matchIndiciesShorter

            longer = false
        }

        if (iterable != null) {
            val iterator = iterable.iterator()
            val index = iterator.next()

            when {
                iterator.hasNext() -> throw InvalidStateException("foo")
                longer -> removeLongerString(string, index)
                else -> removeShorterString(string, index)
            }
        }
        else {
            children.add(searchAmongChildren(string).index, KRadixTreeNode(string, this))
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
}