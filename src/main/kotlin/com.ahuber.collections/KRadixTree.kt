package com.ahuber.collections

import com.ahuber.utils.*
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class KRadixTree : MutableSet<String> {
    private val root = Node.Root()
    private lateinit var version: UUID

    override val size get() = root.descendantCount

    init {
        newVersion()
    }

    override fun add(element: String): Boolean {
        val string = element.normalize {
            "Cannot add a string with whitespace characters. String was \"$element\""
        }

        val successful = add(null, root, string)
        if (successful) newVersion()
        return successful
    }

    override fun addAll(elements: Collection<String>): Boolean = elements.fold { add(it) }

    override fun clear() {
        root.clearChildren()
        root.descendantCount = 0
        newVersion()
    }

    override fun iterator(): MutableIterator<String> = KRadixTreeIterator(this)

    override fun remove(element: String): Boolean {
        val string = element.catchInvalidInputString { return false }
        val successful = remove(null, root, string)
        if (successful) newVersion()
        return successful
    }

    override fun removeAll(elements: Collection<String>): Boolean = elements.fold { remove(it) }

    override fun retainAll(elements: Collection<String>): Boolean = this.filter { it !in elements }.fold { remove(it) }

    override fun contains(element: String): Boolean = contains(root, element.catchInvalidInputString { return false } )

    override fun containsAll(elements: Collection<String>): Boolean = elements.all { it in this }

    override fun isEmpty(): Boolean = size == 0

    private inline fun Iterable<String>.fold(action: (String) -> Boolean): Boolean =
            this.fold(true) { result, string -> action(string) && result }

    private fun newVersion() {
        version = UUID.randomUUID()
    }

    private sealed class Node {
        var descendantCount = 0
        protected var children = ArrayList<Child>()

        final override fun toString(): String {
            val prefix = when (this) {
                is Root -> "Root("
                is Child -> "Child(string = $string, endOfWord: $endOfWord, "
            }
            val childrenString = children.joinToString(", ") { it.string }
            return prefix + "descendantCount = $descendantCount, children = [$childrenString])"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Node) return false
            if (children != other.children) return false
            return true
        }

        override fun hashCode(): Int = children.hashCode()

        fun addChild(child: Child) {
            children.add(child)
            children.sort()
        }

        fun removeChild(child: Child): Boolean {
            val index = children.binarySearch { it.compareTo(child) }

            return when {
                index < 0 -> false
                else -> {
                    children.removeAt(index)
                    true
                }
            }
        }

        fun clearChildren() = children.clear()

        fun moveChildrenTo(other: Child) {
            other.children = this.children
            this.children = ArrayList()
        }

        fun copyChildrenTo(collection: MutableCollection<Child>) {
            collection.addAll(children)
        }

        fun findChild(string: String): Pair<Child, DiffResult>? {
            val index = children.binarySearch {
                when (it.string.diffWith(string)) {
                    is DiffResult.Identical, is DiffResult.Shared -> 0
                    is DiffResult.Different -> it.string.compareTo(string)
                }
            }

            return when (val child = children.getOrNull(index)) {
                null -> null
                else -> child to child.string.diffWith(string)
            }
        }

        class Root : Node()
        class Child private constructor(var string: String, var endOfWord: Boolean, descendantCount: Int = 0) : Node(), Comparable<Child> {

            init {
                this.descendantCount = descendantCount
            }

            override fun equals(other: Any?): Boolean = when (super.equals(other)) {
                false -> false
                true -> when (val child = other as? Child) {
                    null -> false
                    else -> this.string == child.string && this.endOfWord == child.endOfWord
                }
            }

            override fun hashCode(): Int = Objects.hash(super.hashCode(), string.hashCode())

            override fun compareTo(other: Child): Int {
                return this.string.compareTo(other.string)
            }

            companion object {
                operator fun invoke(string: String, endOfWord: Boolean, descendantCount: Int = 0): Child? {
                    return if (string.isEmpty() || descendantCount < 0) null else Child(string, endOfWord, descendantCount)
                }
            }
        }
    }

    private class KRadixTreeIterator(private val tree: KRadixTree): MutableIterator<String> {
        private val ancestors = Stack<NodeWrapper>()
        private val returnedWords = HashSet<String>()
        private var next: String? = null
        private var nextRetrieved = false
        private var cachedVersion = tree.version

        init {
            invalidate()
        }

        private val currentWord: String?
            get() = ancestors.mapNotNull { (it.node as? Node.Child)?.string }
                    .fold(StringBuilder()) { builder, chunk -> builder.append(chunk)}
                    .reverse()
                    .toString()
                    .let { if (it.isEmpty()) null else it }

        private inline val wasInvalidated get() = cachedVersion != tree.version

        override fun hasNext(): Boolean {
            if (wasInvalidated) invalidate()

            // If we have already computed the next value and it has not been returned via next(), return true
            if (this.next != null && !nextRetrieved) return true

            // Otherwise, find the next item (if any) and return a Boolean indicating whether we were able to find
            // the next item in the iterator
            nextRetrieved = false
            var next: Node.Child?

            do {
                next = findNext()
            } while (next != null && currentWord in returnedWords)


            this.next = currentWord
            return this.next != null
        }

        override fun next(): String = when (hasNext()) {
            false -> throw NoSuchElementException()
            true ->
                // Should never happen but added for Smart Cast
                when (val next = this.next) {
                null -> throw NoSuchElementException()
                else -> {
                    nextRetrieved = true
                    returnedWords.add(next)
                    next
                }
            }
        }

        override fun remove() {
            when (val next = this.next) {
                null -> return
                else -> tree.remove(next)
            }
        }

        private fun findNext(): Node.Child? {
            while (true) {
                if (ancestors.isEmpty()) return null

                val children = ancestors.peek().children

                if (children.isEmpty()) {
                    val child = ancestors.pop().node as? Node.Child

                    if (child != null && child.endOfWord) {
                        return child
                    }

                }

                ancestors.push(children.poll().wrap())
            }
        }

        private fun invalidate() {
            ancestors.clear()
            ancestors.push(tree.root.wrap())
            next = null
            nextRetrieved = false
            cachedVersion = tree.version
        }

        private fun Node.wrap() = NodeWrapper(this)

        data class NodeWrapper(val node: Node) {
            val children: Queue<Node.Child> = LinkedList()

            init {
                node.copyChildrenTo(children)
            }
        }
    }

    companion object {
        private inline fun String.normalize(lazyMessage: () -> String): String =
                catchInvalidInputString { throw IllegalArgumentException(lazyMessage()) }

        private inline fun String.catchInvalidInputString(onInvalidString: (String) -> Nothing): String {
            return when {
                this.containsWhitespace -> onInvalidString(this)
                else -> this
            }
        }

        private fun add(ancestor: Node?, node: Node, string: String): Boolean {
            if (string.isEmpty()) {
                return when (node) {
                    is Node.Root -> false
                    is Node.Child -> {
                        node.endOfWord = true
                        if (ancestor != null) ancestor.descendantCount++
                        true
                    }
                }
            }

            val (child, diffResult) = node.findChild(string) ?:
                    return when (val child = Node.Child(string, true)) {
                        null -> throw IllegalStateException("Something terrible happened.")
                        else -> {
                            node.addChild(child)
                            node.descendantCount++
                            true
                        }
                    }


            if (diffResult is DiffResult.Identical) {
                return when(child.endOfWord) {
                    true -> false
                    false -> {
                        child.endOfWord = true
                        node.descendantCount++
                        true
                    }
                }
            }

            check(diffResult is DiffResult.Shared)
            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null) { "Something terrible happened" }

            return when {
                trimmedString.isEmpty() && diffResult.remainder.isEmpty() -> {
                    val wasEndOfWord = child.endOfWord
                    child.endOfWord = true

                    when (wasEndOfWord) {
                        true -> false
                        false -> {
                            if (ancestor != null) ancestor.descendantCount++
                            true
                        }
                    }
                }
                child.string == diffResult.sharedPrefix -> {
                    val successful = add(node, child, trimmedString)
                    if (successful) node.descendantCount++
                    successful
                }
                else -> {
                    val descendantCountDifference = split(child, diffResult, string)
                    node.descendantCount += descendantCountDifference - 1
                    true
                }
            }
        }

        private fun split(child: Node.Child, diffResult: DiffResult.Shared, string: String): Int {
            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null)

            val previousEndOfWord = child.endOfWord
            val previousDescendantCount = child.descendantCount

            val newChild1 = Node.Child(diffResult.remainder, previousEndOfWord, child.descendantCount)
            val newChild2 = Node.Child(trimmedString, true)

            if (newChild1 != null) {
                child.moveChildrenTo(newChild1)
            }

            child.string = diffResult.sharedPrefix
            child.endOfWord = newChild2 == null
            child.descendantCount = 0
            child.clearChildren()

            if (newChild1 != null) {
                child.addChild(newChild1)
                child.descendantCount += newChild1.descendantCount + 1
            }

            if (newChild2 != null) {
                child.addChild(newChild2)
                child.descendantCount
                child.descendantCount++
            }

            return child.descendantCount - previousDescendantCount
        }

        private fun remove(ancestor: Node?, node: Node, string: String): Boolean {
            TODO()
        }

        private fun contains(node: Node, string: String): Boolean {
            if (string.isEmpty()) {
                return when (node) {
                    is Node.Root -> false
                    is Node.Child -> return node.endOfWord
                }
            }

            val (child, diffResult) = node.findChild(string) ?: return false

            if (diffResult is DiffResult.Identical) return child.endOfWord

            check(diffResult is DiffResult.Shared)
            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null)

            return when {
                trimmedString.isEmpty() -> child.endOfWord
                else -> contains(child, trimmedString)
            }
        }
    }
}