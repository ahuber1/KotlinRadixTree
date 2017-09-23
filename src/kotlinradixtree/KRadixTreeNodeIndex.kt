package kotlinradixtree

internal interface KRadixTreeNodeIndex {
    val value : Int
}

internal data class IndexDataWasFound(override val value: Int) : KRadixTreeNodeIndex
internal data class IndexDataShouldBeAt(override val value: Int) : KRadixTreeNodeIndex

internal fun Int.indexDataWasFound() = IndexDataWasFound(this)
internal fun Int.indexDataShouldBeAt(comesBefore: Boolean) : IndexDataShouldBeAt {
    when {
        comesBefore -> when {
            this - 1 < 0 -> return IndexDataShouldBeAt(0)
            else -> return IndexDataShouldBeAt(this - 1)
        }
        else -> return IndexDataShouldBeAt(this + 1)
    }
}

internal fun KRadixTreeNodeIndex.toInt() : Int = this.value
internal fun KRadixTreeNodeIndex.isInNode() : Boolean = this is IndexDataWasFound