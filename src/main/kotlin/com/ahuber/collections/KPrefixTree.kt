package com.ahuber.collections

import com.ahuber.utils.*
import java.util.*
import kotlin.Comparator
import kotlin.NoSuchElementException
import kotlin.collections.HashMap

class KPrefixTree() : MutableSet<String>, WordSearchTree {

    constructor(elements: Iterable<String>) : this() {
        addAll(elements)
    }

    constructor(elements: Sequence<String>) : this() {
        addAll(elements)
    }

    constructor(vararg elements: String) : this() {
        addAll(elements)
    }

    override val size
        @Synchronized get() = root.wordCount

    private val root = Node.Root
    private lateinit var version: UUID

    init {
        newVersion()
    }

    @Synchronized
    override fun add(element: String) = root.add(element.check { return false })

    @Synchronized
    override fun remove(element: String) = root.remove(element.check { return false })

    @Synchronized
    override operator fun contains(element: String) = root.contains(element.check { return false })

    @Synchronized
    override fun search(string: String): WordSearchResult {
        string.check { return WordSearchResult.NoMatch(string) }
        var node: Node = root
        var index = 0

        loop@ for (c in string) {
            when (val child = node[c]) {
                is Option.None -> break@loop
                is Option.Some -> node = child.value
            }

            index++
        }

        return when (index) {
            0 -> WordSearchResult.NoMatch(string)
            else -> {
                val list = NodeIterator(this, node).asSequence().toList()
                when {
                    index == string.length && node.childIterator().asSequence().any { it.isTerminator } -> WordSearchResult.ExactMatch(string, list)
                    else -> WordSearchResult.PartialMatch(string, list)
                }
            }
        }
    }

    @Synchronized
    override fun containsAll(elements: Collection<String>) = elements.all { it in this }

    @Synchronized
    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<String> = NodeIterator(this, root)

    @Synchronized
    override fun addAll(elements: Collection<String>) =
            elements.fold(true) { result, element -> add(element) && result }

    @Synchronized
    override fun clear() {
        root.clear()
    }

    @Synchronized
    override fun removeAll(elements: Collection<String>) =
            elements.fold(true) { result, element -> remove(element) && result }

    @Synchronized
    override fun retainAll(elements: Collection<String>) =
            removeAll(this.filter { it !in elements }.toList())

    @Synchronized
    private fun newVersion() {
        version = UUID.randomUUID()
    }

    private sealed class Node : Comparable<Node> {
        class Root: Node()
        data class Child(
                val character: Char,
                /**
                 * Represents whether this node indicates the end of a word.
                 */
                var endOfWord: Boolean = false
        ) : Node()

        val children = mutableListOf<Child>()

        /**
         * The number of words in the subtree of this [KPrefixTree] where this node is the root.
         */
        var wordCount: Int = 0

        override fun compareTo(other: Node): Int {
            return when {
                this is Root && other is Root -> 0
                this is Root && other is Child -> -1
                this is Child && other is Root -> 1
                this is Child && other is Child -> this.character.compareTo(other.character)
                else -> error("This node and the other node are from an unknown combination. This node is of type " +
                        "${this.javaClass.typeName.wrapInQuotes()}, but the other node is of type " +
                        other.javaClass.typeName.wrapInQuotes())
            }
        }
    }

    private class KPrefixTreeIterator(private val tree: KPrefixTree, root: Node) : MutableIterator<String> {
        private lateinit var cachedVersion: UUID
        private val ancestors = LinkedList<NodeWrapper<*>>()
        private val returnedWords = HashSet<String>()
        private var next: NodeWrapper<*>? = null
        private var nextRetrieved = false

        init {
            invalidate()
            ancestors.push(NodeWrapper(root))
        }

        override fun hasNext(): Boolean {
            synchronized(tree) {
                if (tree.version != cachedVersion) {
                    invalidate()
                }

                if (next != null && !nextRetrieved) {
                    return true
                }

                nextRetrieved = false
                next = findNext()
                return next != null
            }
        }

        override fun next(): String {

        }

        override fun remove() {
            TODO("Not yet implemented")
        }

        private fun invalidate() {

        }
    }

    private class NodeWrapper<T : Node>(val node: T) {
        private var index: Int = 0

        fun nextChild(): NodeWrapper<Node.Child>? = when (val child = node.children.getOrNull(index)) {
            null -> null
            else -> {
                index++
                NodeWrapper(child)
            }
        }

        fun resetIndex() {
            index = 0
        }
    }

    companion object {
        private inline fun String.check(onInvalidString: (String) -> Nothing) = when (this.containsWhitespace) {
            true -> onInvalidString(this)
            false -> this
        }
    }
}