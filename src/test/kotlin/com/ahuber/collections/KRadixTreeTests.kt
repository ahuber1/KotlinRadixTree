package com.ahuber.collections

class KRadixTreeTests : WordSearchTreeTests<KRadixTree>() {

    override fun createEmptySet(): KRadixTree = KRadixTree()

    override fun createSetWith(elements: Iterable<String>): KRadixTree = KRadixTree(elements)

    override fun createSetWith(elements: Sequence<String>): KRadixTree = KRadixTree(elements)

    override fun createSetWith(vararg elements: String): KRadixTree = KRadixTree(*elements)
}