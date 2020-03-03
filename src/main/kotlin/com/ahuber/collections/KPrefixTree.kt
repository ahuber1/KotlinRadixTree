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
                val list = Node.Companion.NodeIterator(this, node).asSequence().toList()
                when {
                    index == string.lastIndex && node.endOfWord-> WordSearchResult.ExactMatch(string, list)
                    else -> WordSearchResult.PartialMatch(string, list)
                }
            }
        }
    }

    @Synchronized
    override fun containsAll(elements: Collection<String>) = elements.all { it in this }

    @Synchronized
    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<String> =
            Node.Companion.NodeIterator(this, root)

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

    private sealed class Node {
        var wordCount = 0
            private set

        private val children = HashMap<Option<Char>, Child>()

        object Root : Node()

        data class Child(val character: Char? = null) : Node(), Comparable<Child> {
            override fun compareTo(other: Child): Int {
                return when {
                    this.character != null && other.character != null -> this.character.compareTo(other.character)
                    this.character == null && other.character == null -> 0
                    this.character == null && other.character != null -> -1
                    else -> 1
                }
            }
        }

        val endOfWord get() = Option.None() in this.children

        fun clear() {
            children.clear()
            wordCount = 0
        }

        operator fun get(character: Char) = this.children[Option.Some(character)].asOption()

        operator fun contains(string: String): Boolean = when {
            string.isEmpty() -> Option.None() in this.children
            else -> when (val child = this.children.getOrNone(string.first().some())) {
                is Option.None -> false
                is Option.Some -> child.value.contains(string.substring(1))
            }
        }

        fun childIterator(): Iterator<Child> =
                children.keys.sortedWith(Comparator { o1, o2 ->
                    when {
                        o1 is Option.None && o2 is Option.None -> 0
                        o1 is Option.Some && o2 is Option.Some -> o1.value.compareTo(o2.value)
                        o1 is Option.Some && o2 is Option.None -> 1
                        else -> -1
                    }
                }).map { children[it]!! }.iterator()

        fun add(string: String): Boolean {
            if (string.isEmpty()) {
                return when {
                    Option.None() in this.children -> false
                    else -> {
                        this.children[Option.None()] = Child()
                        true
                    }
                }
            }

            val firstChar = string.first()

            val child = when (val option = this.children.getOrNone(firstChar.some())) {
                is Option.Some -> option.value
                else -> {
                    val child = Child(firstChar)
                    this.children[firstChar.some()] = child
                    child
                }
            }

            if (!child.add(string.substring(1))) {
                return false
            }

            this.wordCount++
            return true
        }

        fun remove(string: String): Boolean {
            if (string.isEmpty()) {
                return this.children.remove(Option.None()) != null
            }

            val child = this.children.getOrNone(string.first().some()).nullIfNone() ?: return false

            if (!child.remove(string.substring(1))) {
                return false
            }

            this.wordCount--
            val iterator = this.children.iterator()

            while (iterator.hasNext()) {
                if (iterator.next().value.wordCount == 0) {
                    iterator.remove()
                }
            }

            return true
        }
    }

    private class NodeIterator(private val tree: KPrefixTree, private val root: Node) : MutableIterator<String> {
        private val ancestors = LinkedList<NodeWrapper<*>>()
        private lateinit var cachedVersion: UUID

        init {
            ancestors.push(NodeWrapper(root))
            invalidate()
        }

        override fun hasNext(): Boolean {
            if (tree.version != cachedVersion) {
                invalidate()
            }


        }

        override fun next(): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun remove() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private fun invalidate() {
            ancestors.peek()?.resetIterator()
            cachedVersion = tree.version
        }
    }

    private data class NodeWrapper<T : Node>(val node: T) {
        private lateinit var iterator: Iterator<Map.Entry<Option<Char>, Node.Child>>

        init {
            resetIterator()
        }

        val next: NodeWrapper<Node.Child>?
            get() {
                while (iterator.hasNext()) {
                    val (key, value) = iterator.next()

                    if (key is Option.Some) {
                        return NodeWrapper(value)
                    }
                }

                return null
            }

        fun resetIterator() {
            iterator = node.childIterator()
        }
    }

    companion object {
        private inline fun String.check(onInvalidString: (String) -> Nothing) = when (this.containsWhitespace) {
            true -> onInvalidString(this)
            false -> this
        }
    }
}