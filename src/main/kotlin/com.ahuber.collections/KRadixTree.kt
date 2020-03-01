package com.ahuber.collections

import com.ahuber.utils.DiffResult
import com.ahuber.utils.containsWhitespace
import com.ahuber.utils.diffWith
import com.ahuber.utils.removeSharedPrefix
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class KRadixTree() : MutableSet<String> {
    private val root = Node.Root()
    private lateinit var version: UUID

    override var size = 0
        private set

    constructor(iterable: Iterable<String>) : this() {
        this.addAll(iterable)
    }

    constructor(sequence: Sequence<String>) : this() {
        this.addAll(sequence)
    }

    init {
        newVersion()
    }

    override fun add(element: String): Boolean {
        val string = element.check {
            throw IllegalArgumentException("Cannot add a string with whitespace characters. String was \"$element\"")
        }
        val successful = add(root, string)
        if (successful) {
            newVersion()
            size++
        }
        return successful
    }

    override fun addAll(elements: Collection<String>): Boolean = elements.fold { add(it) }

    override fun clear() {
        root.clearChildren()
        newVersion()
        size = 0
    }

    override fun iterator(): MutableIterator<String> = KRadixTreeIterator(this)

    override fun remove(element: String): Boolean {
        val string = element.check { return false }

        if (!remove(null, root, string)) {
            return false
        }

        newVersion()
        size--
        root.cleanup()
        return true
    }

    override fun removeAll(elements: Collection<String>): Boolean = elements.fold { remove(it) }

    override fun retainAll(elements: Collection<String>): Boolean = this.filter { it !in elements }.fold { remove(it) }

    override fun contains(element: String): Boolean = contains(root, element.check { return false })

    override fun containsAll(elements: Collection<String>): Boolean = elements.all { it in this }

    override fun isEmpty(): Boolean = size == 0

    private inline fun Iterable<String>.fold(action: (String) -> Boolean): Boolean =
            this.fold(true) { result, string -> action(string) && result }

    private fun newVersion() {
        version = UUID.randomUUID()
    }

    private sealed class Node {
        //region Properties
        /**
         * Gets the number of direct descendants of this node.
         */
        val childCount: Int get() = children.size
        protected var children: MutableList<Child> = ArrayList()
        //endregion

        //region Nested Members
        class Root : Node()

        class Child private constructor(
                var string: String,
                var endOfWord: Boolean
        ) : Node(), Comparable<Child> {

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
                operator fun invoke(string: String, endOfWord: Boolean): Child? {
                    return when {
                        string.isEmpty() -> null
                        else -> Child(string, endOfWord)
                    }
                }
            }
        }
        //endregion

        //region Methods in Node
        final override fun toString(): String {
            val prefix = when (this) {
                is Root -> "Root("
                is Child -> "Child(string = $string, endOfWord: $endOfWord, "
            }
            val childrenString = children.joinToString(", ") { it.string }
            return prefix + "children = [$childrenString])"
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

        fun childAt(index: Int): Child? = children.getOrNull(index)

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

        fun cleanup() {
            val orphanedGrandchildren = this.children.filter { !it.endOfWord && it.childCount == 0 }

            for (orphanedGrandchild in orphanedGrandchildren) {
                removeChild(orphanedGrandchild)
            }

            if (childCount != 1 || this !is Child) {
                return
            }
            // Now we know there is only one grandchild :-)

            // If the grandchild marks the end of a word
            if (this.children[0].endOfWord) {
                // If the child does NOT mark the end of a word
                if (!endOfWord) {
                    string += this.children[0].string
                    endOfWord = this.children[0].endOfWord
                    this.children[0].moveChildrenTo(this)
                }
            }
            // If the grandchild does NOT mark the end of a word
            else {
                // If the child marks the end of a word
                if (endOfWord && this.children[0].childCount == 0) {
                    removeChild(this.children[0])
                }
                // If the child does NOT mark the end of the word
                else if (!endOfWord) {
                    string += this.children[0].string
                    endOfWord = this.children[0].endOfWord
                    this.children[0].moveChildrenTo(this)
                }
            }
        }
        //endregion
    }

    private class KRadixTreeIterator(private val tree: KRadixTree) : MutableIterator<String> {
        private lateinit var cachedVersion: UUID
        private val invalidated: Boolean get() = tree.version != cachedVersion
        private val ancestors = Stack<NodeWrapper>()
        private val returnedWords = HashSet<String>()
        private var next: NodeWrapper? = null
        private var nextRetrieved = false

        init {
            invalidate()
            ancestors.push(NodeWrapper(tree.root, null))
        }

        override fun hasNext(): Boolean {
            if (invalidated) {
                invalidate()
            }

            if (next != null && !nextRetrieved) {
                return true
            }

            nextRetrieved = false
            next = findNext()
            return next != null
        }

        override fun next(): String {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            val word = next!!.word
            returnedWords.add(word)
            nextRetrieved = true
            return word
        }

        override fun remove() {
            val next = next
            val removeSuccessful = when {
                !nextRetrieved || next == null -> return
                else -> tree.remove(next.word)
            }
            check(removeSuccessful) { "Removal was not successful." }
        }

        private fun findNext(): NodeWrapper? {
            while (ancestors.isNotEmpty()) {
                val child = ancestors.peek()?.nextChild()

                if (child == null) {
                    ancestors.pop()
                    continue
                }

                ancestors.push(child)

                if (child.node is Node.Child && child.node.endOfWord && child.word !in returnedWords && child.word in tree) {
                    return child
                }
            }

            return null
        }

        private fun invalidate() {
            ancestors.forEach { it.resetIndex() }
            cachedVersion = tree.version
            nextRetrieved = false
            next = null

//            ancestors.clear()
//            ancestors.push(NodeWrapper(tree.root, null))
//            cachedVersion = tree.version
//            nextRetrieved = false
//            next = null
        }
    }

    private data class NodeWrapper(val node: Node, val parent: NodeWrapper?) {
        val word: String
        private var childIndex = 0

        init {
            val ancestorWord = parent?.word ?: ""
            val currentWord = (node as? Node.Child)?.string ?: ""
            word = ancestorWord + currentWord
        }

        fun nextChild(): NodeWrapper? = when (val next = node.childAt(childIndex++)) {
            null -> null
            else -> NodeWrapper(next, this)
        }

        fun resetIndex() {
            childIndex = 0
        }
    }

    companion object {

        private inline fun String.check(onInvalidString: (String) -> Nothing): String {
            return when {
                this.containsWhitespace -> onInvalidString(this)
                else -> this
            }
        }

        private fun add(node: Node, string: String): Boolean {
            if (string.isEmpty()) {
                return when (node) {
                    is Node.Root -> false
                    is Node.Child -> {
                        node.endOfWord = true
                        true
                    }
                }
            }

            val (child, diffResult) = node.findChild(string) ?: return when (val child = Node.Child(string, true)) {
                null -> throw IllegalStateException("Something terrible happened.")
                else -> {
                    node.addChild(child)
                    true
                }
            }


            if (diffResult is DiffResult.Identical) {
                return when (child.endOfWord) {
                    true -> false
                    false -> {
                        child.endOfWord = true
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
                    !wasEndOfWord
                }
                child.string == diffResult.sharedPrefix -> add(child, trimmedString)
                else -> {
                    split(child, diffResult, string)
                    true
                }
            }
        }

        private fun remove(ancestor: Node?, node: Node, string: String): Boolean {
            if (string.isEmpty()) {
                return when (node) {
                    is Node.Root -> false
                    is Node.Child -> {
                        node.endOfWord = true
                        if (ancestor != null) node.cleanup()
                        true
                    }
                }
            }

            val (child, diffResult) = node.findChild(string) ?: return false

            if (diffResult is DiffResult.Identical) {
                return when (child.endOfWord) {
                    true -> {
                        child.endOfWord = false
                        child.cleanup()
                        true
                    }
                    false -> false
                }
            }

            check(diffResult is DiffResult.Shared)

            if (diffResult.remainder.isNotEmpty()) return false

            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null)

            val successful = when {
                trimmedString.isEmpty() && diffResult.remainder.isEmpty() -> {
                    val wasEndOfWord = child.endOfWord
                    child.endOfWord = false
                    wasEndOfWord
                }
                else -> remove(node, child, trimmedString)
            }

            child.cleanup()
            return successful
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

            if (diffResult.remainder.isNotEmpty()) return false

            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null)

            return when {
                trimmedString.isEmpty() -> (child.endOfWord && diffResult.remainder.isEmpty())
                else -> contains(child, trimmedString)
            }
        }

        private fun split(child: Node.Child, diffResult: DiffResult.Shared, string: String) {
            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null)

            val previousEndOfWord = child.endOfWord
            val newChild1 = Node.Child(diffResult.remainder, previousEndOfWord)
            val newChild2 = Node.Child(trimmedString, true)

            if (newChild1 != null) {
                child.moveChildrenTo(newChild1)
            }

            child.string = diffResult.sharedPrefix
            child.endOfWord = newChild2 == null
            child.clearChildren()

            if (newChild1 != null) {
                child.addChild(newChild1)
            }

            if (newChild2 != null) {
                child.addChild(newChild2)
            }
        }
    }
}