package com.ahuber.collections

class KPrefixTreeTests : WordSearchTreeTests<KPrefixTree>() {

    override fun createEmptySet(): KPrefixTree = KPrefixTree()

    override fun createSetWith(vararg elements: String): KPrefixTree = KPrefixTree(*elements)

    override fun createSetWith(elements: Sequence<String>): KPrefixTree = KPrefixTree(elements)

    override fun createSetWith(elements: Iterable<String>): KPrefixTree = KPrefixTree(elements)
}