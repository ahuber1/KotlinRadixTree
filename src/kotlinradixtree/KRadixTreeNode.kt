package kotlinradixtree

import org.testng.annotations.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KHashableNode<TData : Any, TTerminator : Any>(data: TData?, parent: KHashableNode<TData, TTerminator>?) :
        KRadixTreeNode<TData, Int, TTerminator, KHashableNode<TData, TTerminator>>(data, parent) {

    override fun dataToId(data: TData): Int = data.hashCode()
    override fun makeChildNode(data: TData): KHashableNode<TData, TTerminator> = KHashableNode(data, this)
}

class KComparableNode<TData : Comparable<TData>, TTerminator : Any>(data: TData?, parent: KComparableNode<TData, TTerminator>?) :
        KRadixTreeNode<TData, TData, TTerminator, KComparableNode<TData, TTerminator>>(data, parent) {

    override fun makeChildNode(data: TData): KComparableNode<TData, TTerminator> = KComparableNode<TData, TTerminator>(data, this)
    override fun dataToId(data: TData) = data
}

abstract class KRadixTreeNode<TData, TIdentifier, TTerminator, TNode>(internal val data: TData?, internal val parent: TNode?)
        where TIdentifier : Comparable<TIdentifier>,
              TTerminator : Any,
              TNode : KRadixTreeNode<TData, TIdentifier, TTerminator, TNode> {

    private val children = ArrayList<TNode>()

    protected abstract fun dataToId(data: TData) : TIdentifier
    protected abstract fun makeChildNode(data: TData) : TNode

    private var lazyId = lazy { dataToId(data!!) }
    internal val id : TIdentifier get() = lazyId.value
    internal var terminator: TTerminator? = null

    internal fun add(data: TData) {
        val index = indexOf(data)

        when {
            index is IndexDataWasFound -> throw UnsupportedOperationException("You cannot add an item that has already been added")
            else -> children.add(index.index, makeChildNode(data))
        }
    }

    internal fun contains(data: TData) : Boolean = indexOf(data).isInNode()

    internal fun get(data: TData) : TNode? {
        val index = indexOf(data)

        if (index.isInNode())
            return children[index.index]
        else
            return null
    }

    internal fun indexOf(data: TData) : KRadixTreeNodeIndex = search(dataToId(data))

    private fun search(id: TIdentifier) : KRadixTreeNodeIndex {
        if (children.isEmpty())
            return 0.indexDataShouldBeAt(true)
        else
            return search(id, 0, children.lastIndex / 2, children.lastIndex)
    }

    private fun search(id: TIdentifier, startIndex: Int, middleIndex: Int, endIndex: Int) : KRadixTreeNodeIndex {
        val middleId = dataToId(children[middleIndex].data!!)

        return when {
            id == middleId -> IndexDataWasFound(middleIndex)
            endIndex - startIndex == 0 -> middleIndex.indexDataShouldBeAt(id < middleId)
            else -> {
                val newStartIndex: Int
                val newEndIndex: Int

                if (id < middleId) {
                    newStartIndex = startIndex
                    newEndIndex = middleIndex
                }
                else {
                    newStartIndex = middleIndex + 1
                    newEndIndex = endIndex
                }

                val newMiddleIndex = ((newEndIndex - newStartIndex) / 2) + newStartIndex

                return search(id, newStartIndex, newMiddleIndex, newEndIndex)
            }
        }
    }

    fun remove(data: TData) : Boolean {
        var index = indexOf(data)

        if (!index.isInNode())
            throw UnsupportedOperationException("You cannot remove an item that does not exist")

        val node = children[index.index]
        node.terminator = null // remove the data at this location

        if (node.children.any())
            return false
        else {
            children.removeAt(index.index)

            // If this node no longer has any children, "tell" the parent to remove this node
            if (children.isEmpty() && this.data != null && this.parent != null) {
                index = parent.search(dataToId(this.data))

                when {
                    index is IndexDataWasFound -> parent.children.removeAt(index.index)
                    else -> throw IllegalStateException("The parent should have a reference to this node")
                }
            }

            return true
        }
    }
}

@Test fun `Test Nodes`() {
    val characters = 'A'..'z'
    val comparableNode = KComparableNode<Char, Char>(null, null)
    val hashableNode = KHashableNode<Char, Char>(null, null)

    // First, add all the characters in the both nodes, assert that each contains each character, and assert that
    // the property data has been set properly
    for (character in characters) {
        comparableNode.add(character)
        println("added $character to comparable node")
        hashableNode.add(character)
        println("added $character to hashable node")

        assertTrue(comparableNode.contains(character))
        println("$character is in comparable node")
        assertTrue(hashableNode.contains(character))
        println("$character is in comparable node")

        assert(comparableNode.get(character) != null && comparableNode.get(character)?.data == character)
        println("\tcomparable node contains $character as data")
        assert(hashableNode.get(character) != null && comparableNode.get(character)?.data == character)
        println("\thashable node contains $character as data")
    }

    for (character in characters) {
        // Assert that the items exist
        assertTrue(comparableNode.contains(character))
        println("$character is in comparable node")
        assertTrue(hashableNode.contains(character))
        println("$character is in comparable node")

        // Assert that removals are successful
        assertTrue(comparableNode.remove(character))
        println("$character is removed from comparable node")
        assertTrue(hashableNode.remove(character))
        println("$character is removed from comparable node")

        // Assert the items have been removed
        assertFalse(comparableNode.contains(character))
        println("$character is NOT in comparable node")
        assertFalse(hashableNode.contains(character))
        println("$character is NOT in comparable node")
    }
}