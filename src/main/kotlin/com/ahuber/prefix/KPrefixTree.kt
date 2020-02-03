package com.ahuber.prefix

import com.ahuber.utils.*
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.HashMap

class KPrefixTree : MutableSet<String> {
    override val size
        @Synchronized get() = root.wordCount

    private val root = Node.Root

    @Synchronized
    override fun add(element: String) = root.add(element.toLowerCase())

    @Synchronized
    override fun remove(element: String) = root.remove(element.toLowerCase())

    @Synchronized
    override operator fun contains(element: String) = root.contains(element.toLowerCase())

    @Synchronized
    override fun containsAll(elements: Collection<String>) = elements.all { it in this }

    @Synchronized
    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<String> =
            Node.Companion.NodeIterator(this)

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

        private val children = HashMap<Char?, Child>()

        object Root: Node()

        data class Child(val character: Char? = null): Node(), Comparable<Child> {
            override fun compareTo(other: Child): Int {
                return when {
                    this.character != null && other.character != null -> this.character.compareTo(other.character)
                    this.character == null && other.character == null -> 0
                    this.character == null && other.character != null -> -1
                    else -> 1
                }
            }
        }

        fun clear() {
            children.clear()
            wordCount = 0
        }

        operator fun contains(string: String) : Boolean = when {
            string.containsWhitespace -> false
            string.isEmpty() -> null in this.children
            else -> when (val child = this.children.getOrNone(string.first())) {
                is Option.None -> false
                is Option.Some -> child.value.contains(string.substring(1))
            }
        }

        fun add(string: String) : Boolean {
            if (string.containsWhitespace) {
                throw invalidStringException
            }

            if (string.isEmpty()) {
                return when {
                    null in this.children -> false
                    else -> {
                        this.children[null] = Child()
                        true
                    }
                }
            }

            val firstChar = string.first()

            val child = when (val option = this.children.getOrNone(firstChar)) {
                is Option.Some -> option.value
                else -> {
                    val child = Child(firstChar)
                    this.children[firstChar] = child
                    child
                }
            }

            if (!child.add(string.substring(1))) {
                return false
            }

            this.wordCount++
            return true
        }

        fun remove(string: String) : Boolean {
            if (string.containsWhitespace) {
                throw invalidStringException
            }

            if (string.isEmpty()) {
                return this.children.remove(null) != null
            }

            val child = this.children.getOrNone(string.first()).nullIfNone() ?: return false

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

            private val invalidStringException: IllegalArgumentException
                    by lazy { IllegalArgumentException("Strings with whitespace characters are not permitted") }

            class NodeIterator(private val tree: KPrefixTree) : MutableIterator<String> {
                data class NodeWrapper<T : Node>(val node: T) {
                    val children: LinkedList<NodeWrapper<Child>> by lazy {
                        node.children.values.asSequence().sortedBy { it }.map {
                            NodeWrapper(
                                    it)
                        }.toCollection(
                                LinkedList())
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
                    node =
                            NodeWrapper(
                                    tree.root)
                    ancestors.clear()
                    nextString = null
                }

                private fun findNextString() : String? {
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
}