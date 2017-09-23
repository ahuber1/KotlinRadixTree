package kotlinradixtree

typealias KRadixComparableStringTree<T> = KRadixComparableTree<Iterable<Char>, Char, T>
typealias KRadixHashableStringTree<T> = KRadixHashableTree<Iterable<Char>, Char, T>

class KRadixComparableTree<TIterable, TIterableData, TTerminator> :
        KRadixTree<TIterable, TIterableData, TTerminator, TIterableData, KComparableNode<TIterableData, TTerminator>>()
             where TIterable : Iterable<TIterableData>, TIterableData : Comparable<TIterableData>, TTerminator : Any{

    override val root: KComparableNode<TIterableData, TTerminator>
        get() = KComparableNode<TIterableData, TTerminator>(null)

}

class KRadixHashableTree<TIterable, TIterableData, TTerminator> :
        KRadixTree<TIterable, TIterableData, TTerminator, Int, KHashableNode<TIterableData, TTerminator>>()
        where TIterable : Iterable<TIterableData>, TIterableData: Any, TTerminator : Any {
    override val root: KHashableNode<TIterableData, TTerminator>
        get() = KHashableNode<TIterableData, TTerminator>(null)
}

abstract class KRadixTree<TIterable, TIterableData, TTerminator, TIdentifier, TNode>
        where TIterable : Iterable<TIterableData>,
              TTerminator : Any,
              TIdentifier : Comparable<TIdentifier>,
              TNode : KRadixTreeNode<TIterableData, TIdentifier, TTerminator, TNode> {

    protected abstract val root: TNode

    fun contains(item: TIterable) : Boolean = get(item) != null

    fun get(item: TIterable) : TTerminator? = get(item.iterator(), root)

    fun get(iterator: Iterator<TIterableData>, currentNode: TNode): TTerminator? {
        if (iterator.hasNext()) {
            val data = iterator.next()
            val nextNode = currentNode.get(data)

            if (nextNode == null)
                return null
            else
                return get(iterator, nextNode)
        }
        else {
            return currentNode.terminator // this will be null if nothing was added
        }
    }

    fun insert(item: TIterable, terminator: TTerminator) = insert(item.iterator(), root, terminator)

    private fun insert(iterator: Iterator<TIterableData>, currentNode: TNode, terminator: TTerminator) {
        if (iterator.hasNext()) {
            val data = iterator.next()
            var nextNode = currentNode.get(data)

            if (nextNode == null) {
                currentNode.add(data)
                nextNode = currentNode.get(data)
            }

            insert(iterator, nextNode!!, terminator)
        }
        else {
            currentNode.terminator = terminator
        }
    }

    fun remove(item: TIterable) : Boolean = remove(item.iterator(), root)

    private fun remove(iterator: Iterator<TIterableData>, currentNode: TNode) : Boolean {
        if (iterator.hasNext()) {
            val data = iterator.next()
            val nextNode = currentNode.get(data)

            if (nextNode == null) {
                throw IllegalStateException("An attempt was made to remove an item that doesn't exist from the tree.")
            }
            else {
                return remove(iterator, nextNode) && currentNode.remove(data)
            }
        }

        return true
    }
}