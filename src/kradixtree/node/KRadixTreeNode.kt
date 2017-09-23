package kradixtree.node

class KHashableNode<TData : Any, TTerminator>(data: TData) : KRadixTreeNode<TData, Int, TTerminator, KHashableNode<TData, TTerminator>>(data) {

    override fun dataToId(data: TData): Int = data.hashCode()
    override fun makeNewNode(data: TData): KHashableNode<TData, TTerminator> = KHashableNode(data)
}

class KComparableNode<TData : Comparable<TData>, TTerminator>(data: TData) : KRadixTreeNode<TData, TData, TTerminator, KComparableNode<TData, TTerminator>>(data) {

    override fun makeNewNode(data: TData): KComparableNode<TData, TTerminator> = KComparableNode<TData, TTerminator>(data)
    override fun dataToId(data: TData) = data
}

abstract class KRadixTreeNode<TData, TIdentifier, TTerminator, TNode>(val data: TData)
        where TIdentifier : Comparable<TIdentifier>, TNode : KRadixTreeNode<TData, TIdentifier, TTerminator, TNode> {

    private val children = ArrayList<TNode>()

    protected abstract fun dataToId(data: TData) : TIdentifier
    protected abstract fun makeNewNode(data: TData) : TNode
    protected fun indexOfAsKRadixTreeNodeIndex(data: TData) : KRadixTreeNodeIndex = search(dataToId(data))

    var id: TIdentifier = dataToId(data)
    var terminator: TTerminator? = null

    fun add(data: TData) : TNode {
        val index = indexOfAsKRadixTreeNodeIndex(data)

        when {
            index is IndexDataWasFound -> throw UnsupportedOperationException("You cannot add an item that has already been added")
            else -> {
                children.add(index.int(), makeNewNode(data))
                return children[index.int()]
            }
        }
    }

    fun contains(data: TData) : Boolean = indexOfAsKRadixTreeNodeIndex(data) is IndexDataWasFound

    fun get(data: TData) : TNode? {
        val index = indexOf(data) ?: return null
        return children[index]
    }

    fun indexOf(data: TData) : Int? {
        val index = indexOfAsKRadixTreeNodeIndex(data)

        when {
            index is IndexDataWasFound -> return index.int()
            else -> return null
        }
    }

    private fun search(id: TIdentifier) : KRadixTreeNodeIndex = search(id, 0, children.lastIndex / 2, children.lastIndex)

    private fun search(id: TIdentifier, startIndex: Int, middleIndex: Int, endIndex: Int) : KRadixTreeNodeIndex {
        val middleId = dataToId(children[middleIndex].data)

        when {
            id == middleId -> return IndexDataWasFound(middleIndex)
            startIndex == middleIndex -> return middleIndex.indexDataShouldBeAt(id < middleId)
            middleId < id -> return search(id, startIndex, (middleIndex - startIndex) / 2, middleIndex)
            else -> return search(id, middleIndex, (endIndex - middleIndex) / 2, endIndex)
        }
    }
}