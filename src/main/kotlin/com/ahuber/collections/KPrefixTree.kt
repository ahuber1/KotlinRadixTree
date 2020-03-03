package com.ahuber.collections

import com.ahuber.utils.*
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.HashMap

class KPrefixTree : MutableSet<String>, WordSearchTree {
    override val size
        @Synchronized get() = root.wordCount

    private val root = Node.Root

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

        companion object {
            class NodeIterator(private val tree: KPrefixTree, private val root: Node) : MutableIterator<String> {
                data class NodeWrapper<T : Node>(val node: T) {
                    val children: LinkedList<NodeWrapper<Child>> by lazy {
                        node.children.values.asSequence()
                                .sortedBy { it }
                                .map { NodeWrapper(it) }
                                .toCollection(LinkedList())
                    }
                }

                private val characters = LinkedList<Char>()
                private val returnedStrings = Stack<String>()
                private val ancestors = Stack<NodeWrapper<*>>()
                private var node: NodeWrapper<*>? = null
                private var nextString: String? = null

                init {
                    reset()
                }

                override fun hasNext(): Boolean {
                    if (nextString == null) {
                        nextString = findNextString()
                    }

                    if (nextString != null && nextString !in returnedStrings) {
                        returnedStrings.push(nextString)
                    }

                    return nextString != null
                }

                override fun next(): String = nextString ?: throw NoSuchElementException()

                override fun remove() {
                    tree.remove(next())
                    reset()
                }

                private fun reset() {
                    characters.clear()
                    node = NodeWrapper(root)
                    ancestors.clear()
                    nextString = null
                }

                private fun findNextString(): String? {
                    while (true) {
                        val node = this.node ?: return null
                        val child = node.children.removeFirstOrNone().nullIfNone()

                        if (child == null) {
                            this.node = ancestors.popOrNone().nullIfNone()

                            // using removeFirstOrNone makes the operation removeFirst()
                            // method not throw an exception if there are no elements in the linked list
                            this.characters.removeFirstOrNone()
                            continue
                        }

                        val character = child.node.character

                        if (character == null) {
                            val string = String(characters.toCharArray())
                            return if (string in returnedStrings) continue else string
                        }

                        ancestors.push(node)
                        this.node = child
                        characters.addLast(character)
                    }
                }
            }
        }
    }

    companion object {

        private inline fun String.check(onInvalidString: (String) -> Nothing) = when (this.containsWhitespace) {
            true -> onInvalidString(this)
            false -> this
        }
    }
}