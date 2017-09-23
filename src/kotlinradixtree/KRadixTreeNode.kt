package kotlinradixtree

class KHashableNode<TData : Any, TTerminator : Any>(data: TData?) : KRadixTreeNode<TData, Int, TTerminator, KHashableNode<TData, TTerminator>>(data) {

    override fun dataToId(data: TData): Int = data.hashCode()
    override fun makeNewNode(data: TData): KHashableNode<TData, TTerminator> = KHashableNode(data)
}

class KComparableNode<TData : Comparable<TData>, TTerminator : Any>(data: TData?) : KRadixTreeNode<TData, TData, TTerminator, KComparableNode<TData, TTerminator>>(data) {

    override fun makeNewNode(data: TData): KComparableNode<TData, TTerminator> = KComparableNode<TData, TTerminator>(data)
    override fun dataToId(data: TData) = data
}

abstract class KRadixTreeNode<TData, TIdentifier, TTerminator, TNode>(private val data: TData?)
        where TIdentifier : Comparable<TIdentifier>, TTerminator : Any, TNode : KRadixTreeNode<TData, TIdentifier, TTerminator, TNode> {

    private val children = ArrayList<TNode>()

    protected abstract fun dataToId(data: TData) : TIdentifier
    protected abstract fun makeNewNode(data: TData) : TNode

    internal var id: TIdentifier = dataToId(data!!)
    internal var terminator: TTerminator? = null

    internal fun add(data: TData) {
        val index = indexOf(data)

        when {
            index is IndexDataWasFound -> throw UnsupportedOperationException("You cannot add an item that has already been added")
            else -> children.add(index.toInt(), makeNewNode(data))
        }
    }

    internal fun contains(data: TData) : Boolean = indexOf(data).isInNode()

    internal fun get(data: TData) : TNode? {
        val index = indexOf(data)

        if (index.isInNode())
            return children[index.toInt()]
        else
            return null
    }

    internal fun indexOf(data: TData) : KRadixTreeNodeIndex = search(dataToId(data))

    private fun search(id: TIdentifier) : KRadixTreeNodeIndex = search(id, 0, children.lastIndex / 2, children.lastIndex)

    private fun search(id: TIdentifier, startIndex: Int, middleIndex: Int, endIndex: Int) : KRadixTreeNodeIndex {
        val middleId = dataToId(children[middleIndex].data!!)

        when {
            id == middleId -> return IndexDataWasFound(middleIndex)
            startIndex == middleIndex -> return middleIndex.indexDataShouldBeAt(id < middleId)
            middleId < id -> return search(id, startIndex, (middleIndex - startIndex) / 2, middleIndex)
            else -> return search(id, middleIndex, (endIndex - middleIndex) / 2, endIndex)
        }
    }

    fun remove(data: TData) : Boolean {
        val index = indexOf(data)

        if (!index.isInNode())
            throw UnsupportedOperationException("You cannot remove an item that does not exist")

        val node = children[index.toInt()]

        if (node.children.any())
            return false
        else {
            children.removeAt(index.toInt())
            return true
        }
    }
}