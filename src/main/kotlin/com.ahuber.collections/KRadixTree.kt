package com.ahuber.collections

import com.ahuber.utils.*
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.HashSet

class KRadixTree : MutableSet<String> {
    private val logger = this.getLogger()
    private val root = Node.Root()
    private lateinit var version: UUID

    override var size = -1
        private set(value) {
            logger.debug("'size' setter called.")

            if (field == value) {
                logger.debug("Not updating 'size'. No change in value.")
                return
            }

            logger.debug("Changing value from $field to $value")
            field = value

            version = UUID.randomUUID().also {
                when {
                    this::version.isInitialized -> logger.debug("Changing version from $version to $it")
                    else -> logger.debug("Initializing version to $it")
                }
            }
        }

    init {
        size = 0 // Triggers the setter to run
    }

    override fun add(element: String): Boolean {
        logger.info("Adding '$element'")
        val string = element.normalize {
            "Cannot add a string with whitespace characters. String was ${element.withQuotationMarks}"
        }

        val successful = add(root, string)

        if (successful) {
            logger.info("Adding '$element' was successful. Incrementing size.")
            size++
        } else {
            logger.info("Adding '$element' was unsuccessful.")
        }

        return successful
    }

    override fun addAll(elements: Collection<String>): Boolean {
        logger.info("Adding ${elements.size} elements in 'addAll'")
        return elements.fold { add(it) }.also { logger.debug("${elements.size} elements were added in 'addAll'") }
    }

    override fun clear() {
        logger.info("Clearing $size elements from the tree.")
        root.children.clear()
        size = 0
    }

    override fun iterator(): MutableIterator<String> = KRadixTreeIterator(this)

    override fun remove(element: String): Boolean {
        logger.info("Removing '$element'")
        val string = element.catchInvalidInputString { return false }
        val successful = remove(root, string)
        if (successful) {
            logger.info("Removing '$element' was successful.")
            size--
        } else {
            logger.info("Removing '$element' was unsuccessful.")
        }
        return successful
    }

    override fun removeAll(elements: Collection<String>): Boolean {
        logger.info("Removing ${elements.size} elements in 'removeAll'")
        return elements.fold { remove(it) }
                .also { logger.info("${elements.size} elements were removed in 'removeAll'") }
    }

    override fun retainAll(elements: Collection<String>): Boolean {
        logger.info("Retaining ${elements.size} in 'retainAll'")
        return this.filter { it !in elements }.fold { remove(it) }
                .also {logger.info("Retained ${elements.size} in 'retainAll'") }
    }

    override fun contains(element: String): Boolean {
        logger.info("Checking to see if '$element' is in this tree.")
        return contains(root, element.catchInvalidInputString { return false } )
                .also {
                    val string = if (it) "contains" else "does not contain"
                    logger.info("The tree $string '$element'")
                }
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        logger.info("Checking if this tree contains ${elements.size} elements in 'containsAll'")
        return elements.all { it in this }.also {
            if (it) {
                logger.info("This tree contains all ${elements.size} elements in the provided collection.")
            }
            else {
                logger.info("This tree does not contain all ${elements.size} elements in the provided collection.")
            }

            logger.info("Returning $it from 'containsAll'")
        }
    }

    override fun isEmpty(): Boolean = size == 0

    private inline fun Iterable<String>.fold(action: (String) -> Boolean): Boolean =
            this.fold(true) { result, string -> action(string) && result }

    private sealed class Node {
        class Root : Node()

        class Child private constructor(var string: String, var endOfWord: Boolean) : Node() {

            override fun equals(other: Any?): Boolean = when (super.equals(other)) {
                false -> false
                true -> when (val child = other as? Child) {
                    null -> false
                    else -> this.string == child.string && this.endOfWord == child.endOfWord
                }
            }

            override fun hashCode(): Int = Objects.hash(super.hashCode(), string.hashCode())

            companion object {
                operator fun invoke(string: String, endOfWord: Boolean): Child? {
                    return if (string.isEmpty()) null else Child(string, endOfWord)
                }
            }
        }

        var children = LinkedList<Child>()

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
    }

    private class KRadixTreeIterator(private val tree: KRadixTree): MutableIterator<String> {
        private val logger = this.getLogger()
        private val ancestors = Stack<NodeWrapper>()
        private val returnedWords = HashSet<String>()
        private var next: String? = null
        private var nextRetrieved = false
        private var cachedVersion = tree.version

        init {
            logger.debug("Started initialization.")
            invalidate()
            logger.debug("Ended initialization.")
        }

        private val currentWord: String?
            get() = ancestors.mapNotNull { (it.node as? Node.Child)?.string }
                    .fold(StringBuilder()) { builder, chunk -> builder.append(chunk)}
                    .reverse()
                    .toString()
                    .let { if (it.isEmpty()) null else it }
                    .also { logger.debug("'currentWord' is $it") }

        private inline val wasInvalidated get() =
            (cachedVersion != tree.version).also { logger.debug("'wasInvalidated' is $it") }

        override fun hasNext(): Boolean {
            logger.debug("'hasNext' was called.")

            if (wasInvalidated) {
                logger.debug("The tree was invalidated. Calling invalidate().")
                invalidate()
            }

            // If we have already computed the next value and it has not been returned via next(), return true
            if (this.next != null && !nextRetrieved) {
                logger.debug("We have already computed the next value, but it has not been returned via next().")
                logger.debug("The cached value will be used, which means this iterator has the next element ready to go.")
                return true

            }
            // Otherwise, find the next item (if any) and return a Boolean indicating whether we were able to find
            // the next item in the iterator
            nextRetrieved = false
            var next: Node.Child?
            logger.debug("Searching for next element.")

            do {
                next = findNext()
            } while (next != null && currentWord in returnedWords)


            this.next = currentWord

            logger.debug("The next word that will be returned in this iterator is '${this.next}'")

            return (this.next != null).also {
                when(it) {
                    true -> logger.debug("Found the next value")
                    false -> logger.debug("Did not find the next value")
                }
            }
        }

        override fun next(): String = when (hasNext()) {
            false -> {
                logger.error("There is no element in the iterator!")
                throw NoSuchElementException()
            }
            true -> when (val next = this.next) {
                null -> {
                    logger.error("Something terrible has happened...")
                    throw NoSuchElementException() // Should never happen but added for Smart Cast
                }
                else -> {
                    logger.error("Retrieving next string in the iterator, that being '$next'")
                    nextRetrieved = true
                    returnedWords.add(next)
                    next
                }
            }
        }

        override fun remove() {
            when (val next = this.next) {
                null -> {
                    logger.warn("Unable to remove item in iterator.")
                    return
                }
                else -> {
                    logger.warn("Removing '$next'")
                    tree.remove(next)
                }
            }
        }

        private fun findNext(): Node.Child? {
            while (true) {
                logger.debug("At top of loop in findNext()")

                if (ancestors.isEmpty()) {
                    logger.debug("No more ancestors on record. All items have been iterated through. Returning null.")
                    return null
                }

                logger.debug("Ancestor queue has ${ancestors.size} items")
                logger.debug("Peeking item at head of ancestor queue.")
                val children = ancestors.peek().children

                if (children.isEmpty()) {
                    logger.debug("There are children in the peeked ancestor.")
                    logger.debug("Popping child and analyzing it.")
                    val child = ancestors.pop().node as? Node.Child

                    if (child != null) {
                        if (child.endOfWord) {
                            logger.debug("Popped child represents the end of a word. Returning this child.")
                            return child
                        }
                        else {
                            logger.debug("Popped child represents only a part of a word. Continuing...")
                        }
                    }
                    else {
                        logger.debug("Popped child was the root node. Continuing...")
                    }

                }
                else {
                    logger.debug("There are no children in the peeked ancestor. Continuing...")
                }

                logger.debug("Adding child to ancestors.")
                ancestors.push(children.poll().wrap())
            }
        }

        private fun invalidate() {
            logger.debug("Invalidating...")
            ancestors.clear()
            ancestors.push(tree.root.wrap())
            next = null
            nextRetrieved = false
            cachedVersion = tree.version
            logger.debug("Invalidation complete.")
        }

        private fun Node.wrap() = NodeWrapper(this)

        data class NodeWrapper(val node: Node) {
            val children: Queue<Node.Child> = LinkedList(node.children)
        }
    }

    companion object {
        private val logger = this.getLogger()

        private inline fun String.normalize(lazyMessage: () -> String): String =
                catchInvalidInputString { throw IllegalArgumentException(lazyMessage()) }

        private inline fun String.catchInvalidInputString(onInvalidString: (String) -> Nothing): String {
            return when {
                this.containsWhitespace -> onInvalidString(this)
                else -> this
            }
        }

        private fun add(node: Node, string: String): Boolean {
            logger.debug("--------------------------------------------------------")
            logger.debug("add(node = $node, string = \"$string\")")

            if (string.isEmpty()) {
                logger.debug("String is empty")
                return when (node) {
                    is Node.Root -> {
                        logger.debug("Current node is the root node. Returning false.")
                        false
                    }
                    is Node.Child -> {
                        logger.debug("Marking child node as the end of the word. Returning true")
                        node.endOfWord = true
                        true
                    }
                }
            }

            logger.debug("Looking for child...")

            val (child, diffResult) = findChild(node.children, string) ?:
                    return when (val child = Node.Child(string, true)) {
                        null -> throw IllegalStateException("Something terrible happened.")
                        else -> {
                            logger.debug("Adding child and returning true")
                            node.children.add(child)
                            true
                        }
                    }

            logger.debug("Found child")
            logger.debug("child: $child")
            logger.debug("diffResult: $diffResult")

            if (diffResult is DiffResult.Identical) {
                return when(child.endOfWord) {
                    true -> {
                        logger.debug("Found identical string. Returning false to indicate the string is not new.")
                        false
                    }
                    false -> {
                        logger.debug("Found identical string. However, node was not marked as the end of a word.")
                        child.endOfWord = true
                        logger.debug("Node marked as end of word.")
                        true
                    }
                }
            }

            check(diffResult is DiffResult.Shared) { "Something terrible happened" }

            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null) { "Something terrible happened" }
            logger.debug("Trimmed string is \"$trimmedString\"")

            return when {
                trimmedString.isEmpty() && diffResult.remainder.isEmpty() -> {
                    logger.debug("Trimmed string and remainder are empty.")
                    logger.debug("child.endOfWord = ${child.endOfWord}")
                    val wasEndOfWord = child.endOfWord

                    logger.debug("Setting child.endOfWord to true")
                    child.endOfWord = true
                    !wasEndOfWord.also { logger.debug("Returning $it") }
                }
                child.string == diffResult.sharedPrefix -> {
                    logger.debug("The child and the shared prefix are the same. Performing recursive call...")
                    add(child, trimmedString)
                }
                else -> split(child, diffResult, string)
            }
        }

        private fun split(child: Node.Child, diffResult: DiffResult.Shared, string: String): Boolean {
            logger.debug("--------------------------------------------------------")
            logger.debug("split(child = $child, diffResult = $diffResult, string = $string")

            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null) { "Something terrible happened" }

            val previousEndOfWord = child.endOfWord
            val newChild1 = Node.Child(diffResult.remainder, previousEndOfWord)
            val newChild2 = Node.Child(trimmedString, true)

            newChild1?.children = child.children
            child.string = diffResult.sharedPrefix
            child.endOfWord = newChild2 == null
            child.children = LinkedList()

            if (newChild1 != null) child.children.add(newChild1)
            if (newChild2 != null) child.children.add(newChild2)

            logger.debug("Child has been split.")
            logger.debug("    child: $child")
            logger.debug("newChild1: $newChild1")
            logger.debug("newChild2: $newChild2")
            logger.debug("Returning true.")
            return true
        }

        private fun remove(node: Node, string: String): Boolean {
            logger.debug("--------------------------------------------------------")
            logger.debug("remove(node = $node, string = \"$string\")")
            logger.debug("Looking for child...")
            val (child, diffResult) = findChild(node.children, string)
                    ?: return false.also { logger.debug("Returning null as child was not found.") }

            logger.debug("Found child")
            logger.debug("child: $child")
            logger.debug("diffResult: $diffResult")

            return when (diffResult) {
                is DiffResult.Shared -> {
                    logger.debug("The child and the target string share a common prefix. Performing recursive call...")
                    val removalSuccessful = diffResult.removeSharedPrefix(string)?.let { remove(child, it) }
                    check(removalSuccessful != null) { "Something terrible happened." }
                    logger.debug("Returned from recursive call")

                    if (node is Node.Child && node.children.size == 1 && !node.endOfWord) {
                        logger.debug("Found an only child.")
                        logger.debug("Node before absorbing only child: $node")
                        val onlyChild = node.children.remove()
                        node.string += onlyChild.string
                        node.endOfWord = onlyChild.endOfWord
                        node.children = onlyChild.children
                        logger.debug("Node after absorbing only child: $node")
                    }

                    logger.debug("Returning result from recursive call, that being $removalSuccessful")
                    removalSuccessful
                }
                else -> {
                    logger.debug("Found match. Removing child and returning true.")
                    node.children.remove(child)
                    true
                }
            }
        }

        private fun contains(node: Node, string: String): Boolean {
            logger.debug("--------------------------------------------------------")
            logger.debug("contains(node = $node, string = \"$string\")")

            if (string.isEmpty()) {
                logger.debug("String is empty")
                return when (node) {
                    is Node.Root -> {
                        logger.debug("Current node is the root node. Returning false.")
                        false
                    }
                    is Node.Child -> {
                        logger.debug("Current node is a child node. Returning ${node.endOfWord}")
                        return node.endOfWord
                    }
                }
            }

            logger.debug("Looking for child...")
            val result = findChild(node.children, string)

            if (result == null) {
                logger.debug("Could not find child.")
                return false
            }

            val (child, diffResult) = result
            logger.debug("Found child.")
            logger.debug("child: $child")
            logger.debug("diffResult: $diffResult")

            if (diffResult is DiffResult.Identical) {
                logger.debug("Found an identical match.")

                when (child.endOfWord) {
                    true -> logger.debug("Child represents the end of a word. Returning true.")
                    false -> logger.debug("Child does not represent the end of a word. Returning false.")
                }

                return child.endOfWord
            }

            check(diffResult is DiffResult.Shared) { "Something terrible happened" }
            val trimmedString = diffResult.removeSharedPrefix(string)
            check(trimmedString != null) { "Something terrible happened" }
            logger.debug("Trimmed string is \"$trimmedString\"")

            if (trimmedString.isEmpty()) {
                logger.debug("Trimmed string is empty.")

                when (child.endOfWord) {
                    true -> logger.debug("Child represents the end of a word. Returning true.")
                    false -> logger.debug("Child does not represent the end of a word. Returning false.")
                }

                return child.endOfWord
            }

            logger.debug("Performing recursive call...")
            return contains(child, trimmedString)
        }

        private fun findChild(children: Iterable<Node.Child>, string: String): Pair<Node.Child, DiffResult>? {
            logger.debug("Searching for child...")

            for (child in children) {
                logger.debug("Comparing \"${child.string}\" against \"$string\"")
                val result = child.string.diffWith(string)

                if (result is DiffResult.Identical || result is DiffResult.Shared) {
                    logger.debug("Found match")
                    logger.debug("child: $child")
                    logger.debug("result: $result")
                    return child to result
                }

                logger.debug("Didn't find match.")
            }

            logger.debug("Did not find child. Returning null.")
            return null
        }

        private fun constructPreOrderTraversalString(node: Node, builder: StringBuilder, level: Int) {
            val indentation = "|---".repeat(level)
            builder.appendln("$indentation$node")

            for (child in node.children) {
                constructPreOrderTraversalString(child, builder, level + 1)
            }
        }
    }
}