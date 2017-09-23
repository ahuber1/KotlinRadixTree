package kradixtree.node

interface KRadixTreeNodeIndex {
    val value : Int
}

data class IndexDataWasFound(override val value: Int) : KRadixTreeNodeIndex
data class IndexDataShouldBeAt(override val value: Int) : KRadixTreeNodeIndex

fun Int.indexDataWasFound() = IndexDataWasFound(this)
fun Int.indexDataShouldBeAt(comesBefore: Boolean) : IndexDataShouldBeAt {
    when {
        comesBefore -> when {
            this - 1 < 0 -> return IndexDataShouldBeAt(0)
            else -> return IndexDataShouldBeAt(this - 1)
        }
        else -> return IndexDataShouldBeAt(this + 1)
    }
}
fun KRadixTreeNodeIndex.int() : Int = this.value