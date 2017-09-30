package kotlinradixtree

import org.testng.annotations.Test
import kotlin.test.assertEquals

internal interface KRadixTreeNodeIndex {
    val index: Int
}

internal data class IndexDataWasFound(override val index: Int) : KRadixTreeNodeIndex
internal data class IndexDataShouldBeAt(override val index: Int) : KRadixTreeNodeIndex

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

@Test fun `test index data was found extension function`() {
    val indexDataWasFound = 0.indexDataWasFound()
    assertEquals(0, indexDataWasFound.index)
}

@Test fun `test index data should be at extension function`() {
    val beforeIndexThatShouldBeZero = 0.indexDataShouldBeAt(true)
    val beforeIndexThatShouldBeOne = 2.indexDataShouldBeAt(true)
    val afterIndex = 0.indexDataShouldBeAt(false)

    assertEquals(0, beforeIndexThatShouldBeZero.index)
    assertEquals(1, beforeIndexThatShouldBeOne.index)
    assertEquals(1, afterIndex.index)
}