import kotlinradixtree.*
import org.testng.annotations.Test
import java.io.File
import java.lang.StringBuilder
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KRadixTree {
    private val root = KRadixTreeNode()
    private var _size = 0
    var size: Int
        get() = _size
        private set(value) { _size = value }

    fun add(string: String) {
        root.add(string)
        size++
    }

    operator fun contains(string: String) : Boolean = root.contains(string)

    fun remove(string: String) : Boolean {
        if (root.remove(string)) {
            size--
            return true
        }

        return false
    }

    internal fun childrenAreEmpty() = root.childrenAreEmpty()

    private class KRadixTreeNode {

        private var string: String?
        private var endOfWord: Boolean
        private var children = HashSet<KRadixTreeNode>()

        constructor() {
            string = null
            endOfWord = false
        }

        constructor(string: String, endOfWord: Boolean) {
            this.string = string
            this.endOfWord = endOfWord
        }

        fun add(string: String) {
            check(this.string == null) { "One cannot call add(String) on anything other than the root node" }
            check(string.isNotEmpty()) { "Invalid String \"$string\": one cannot add an empty string into a radix tree" }
            check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
                "Invalid String \"$string\": one cannot add strings that contain whitespace characters or uppercase letters."
            }
            add(this, string)
        }

        fun childrenAreEmpty() = children.isEmpty()

        operator fun contains(string: String): Boolean {
            if (string.isEmpty())
                return false // empty strings cannot be added into the radix tree
            if (string.toCharArray().any { it.isWhitespace() || it.isUpperCase() } )
                return false // cannot have characters with whitespace or with uppercase letters

            return contains(this, string)
        }

        fun remove(string: String) : Boolean {
            check(this.string == null) { "You cannot call remove(String) on anything other than the root node" }
            check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
                "Invalid String \"$string\": one cannot remove strings that contain whitespace characters or uppercase letters."
            }

            return if (string.isNotEmpty()) remove(this, string) else false // you cannot add empty strings; as such, you cannot remove them
        }

        override fun toString(): String {
            val childrenString = "[ ${children.map { if (it.endOfWord) "${it.string!!}*" else it.string!! }.joinToString(", ")} ]"
            var string = this.string ?: ""

            if (this.endOfWord)
                string += "*"

            string = "\"$string\""

            return "KRadixTreeNode(string = $string, children = $childrenString)"
        }

        companion object {
            private fun add(node: KRadixTreeNode, string: String): KRadixTreeNode? {
                if (string.isEmpty())
                    return null

                val match = getChildContainingLongestSharedPrefix(node, string)

                if (match == null) {
                    val newNode = KRadixTreeNode(string, true)
                    node.children.add(newNode)
                    return newNode
                }
                else {
                    if (match.string!! == string) {
                        match.endOfWord = true
                        return match
                    }

                    val resultWithCharsInMatch = compareStringsWithSharedPrefix(string, match.string!!)
                    val resultWithCharsInString = compareStringsWithSharedPrefix(match.string!!, string)

                    return if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty() &&
                            resultWithCharsInString.suffixWhereStringsDiffer.isNotEmpty()) {
                        add(match, resultWithCharsInString.suffixWhereStringsDiffer)
                    } else {
                        split(node, match, resultWithCharsInString, string)
                    }
                }
            }

            private fun combine(node: KRadixTreeNode, with: KRadixTreeNode) {
                node.children = with.children
                node.endOfWord = with.endOfWord
                node.string = node.string!! + with.string
            }

            fun compareStringsWithSharedPrefix(string1: String, string2: String) : KRadixTreeStringComparisonResult {
                val charBuffLen = maxOf(string1.length, string2.length)
                val buffer1 = string1.toCharArrayWithFill(charBuffLen).asIterable()
                val buffer2 = string2.toCharArrayWithFill(charBuffLen).asIterable()
                val sharedBuffer = Array(buffer1.count(), { '\u0000' } )
                val differBuffer = sharedBuffer.copyOf()

                for ((index, pair) in (buffer1 iterateSimultaneouslyWith buffer2).withIndex()) {
                    val (c1, c2) = pair
                    sharedBuffer[index] = if (c1 == c2) c1 else '\u0000'
                    differBuffer[index] = if (c1 == c2) '\u0000' else c2
                }

                val trimmedSharedBuffer  = sharedBuffer.filter { it != '\u0000' }
                val trimmedDifferBuffer = differBuffer.filter { it != '\u0000' }
                val shareString = String(trimmedSharedBuffer.toCharArray())
                val differString = String(trimmedDifferBuffer.toCharArray())

                return KRadixTreeStringComparisonResult(shareString, differString)
            }

            private fun contains(node: KRadixTreeNode, str: String) : Boolean {
                val child = getChildContainingLongestSharedPrefix(node, str) ?: return false // not found
                val result = compareStringsWithSharedPrefix(child.string!!, str)

                if (child.string!! == result.prefixStringsShare) {
                    if (result.suffixWhereStringsDiffer.isEmpty())
                        return child.endOfWord

                    return contains(child, result.suffixWhereStringsDiffer)
                }

                return false
            }

            private fun getChildContainingLongestSharedPrefix(node: KRadixTreeNode, string: String) : KRadixTreeNode? {
                var index = 0
                var searchResult: KRadixTreeNode? = null
                var match: KRadixTreeNode? = null
                val builder = StringBuilder()

                do {
                    builder.append(string[index++])
                    searchResult = node.children.find { it.string!!.startsWith(builder) }

                    if (searchResult != null)
                        match = searchResult

                }  while(builder.length < string.length && searchResult != null)

                return match
            }

            private fun getCompleteWords(node: KRadixTreeNode) : List<String> {
                val list = LinkedList<String>()

                for (child in node.children) {
                    getCompleteWordsWorker(child, "", list)
                }

                return list
            }

            private fun getCompleteWordsWorker(node: KRadixTreeNode, builder: String, list: LinkedList<String>) {
                val str: String by lazy { builder + node.string!! }

                if (node.endOfWord)
                    list.add(str)

                for (child in node.children) {
                    getCompleteWordsWorker(child, str, list)
                }
            }

            // First - Removal was successful/unsuccessful
            // Second - Collapse was performed
            private fun remove(node: KRadixTreeNode, str: String) : Boolean {
                if (str.isEmpty())
                    return true

                val otherNode = getChildContainingLongestSharedPrefix(node, str) ?: return false // no match found
                val result = compareStringsWithSharedPrefix(otherNode.string!!, str)
                val removalWasSuccessful = remove(otherNode, result.suffixWhereStringsDiffer)

                if (!removalWasSuccessful)
                    return removalWasSuccessful

                if (otherNode.string!! == str) {
                    if (otherNode.children.isEmpty())
                        node.children.remove(otherNode)
                    else
                        otherNode.endOfWord = false
                }
                if (otherNode.children.size == 1 && !otherNode.endOfWord && otherNode.children.first().endOfWord) {
                    combine(otherNode, otherNode.children.first())
                }
                // If node is not the root, otherNode is still one of node's children, and if node is not at the end of a
                // word, but otherNode is, collapse otherNode into node
                if (node.string != null && node.children.size == 1 && !node.endOfWord && node.children.first().endOfWord) {
                    combine(node, node.children.first())
                }

                return true
            }

            private fun split(node: KRadixTreeNode, match: KRadixTreeNode, resultWithEmptySuffix: KRadixTreeStringComparisonResult,
                              stringBeingAdded: String): KRadixTreeNode? {
                val words = getCompleteWords(node)
                node.children.remove(match)
                add(node, resultWithEmptySuffix.prefixStringsShare)!!.endOfWord = false

                for (word in words) {
                    add(node, word)
                }

                return add(node, stringBeingAdded)
            }
        }
    }

    private data class KRadixTreeStringComparisonResult(val prefixStringsShare: String,
                                                         val suffixWhereStringsDiffer: String)

    @Test
    fun `Test KRadixTreeStringCompare`() {
        var string1 = "table"
        var string2 = "tables"
        var expectedSimilarPrefix = "table"
        var expectedDissimilarSuffix = "s"
        var result = KRadixTreeNode.compareStringsWithSharedPrefix(string1, string2)

        assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
        assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)

        string1 = "tables"
        string2 = "table"
        expectedSimilarPrefix = "table"
        expectedDissimilarSuffix = ""
        result = KRadixTreeNode.compareStringsWithSharedPrefix(string1, string2)

        assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
        assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)

        string1 = "table"
        string2 = "table"
        expectedSimilarPrefix = "table"
        expectedDissimilarSuffix = ""
        result = KRadixTreeNode.compareStringsWithSharedPrefix(string1, string2)

        assertEquals(expectedSimilarPrefix, result.prefixStringsShare)
        assertEquals(expectedDissimilarSuffix, result.suffixWhereStringsDiffer)
    }
}

@Test
internal fun foo() {
    val words =  """|scratchback
                        |passion-flower
                        |woleai
                        |marwar
                        |unobediently
                        |flourescent
                        |spinode
                        |atomisation
                        |epiglottitis
                        |blather
                        |anatomise
                        |philocynicism
                        |nonretiring
                        |butterbox
                        |odessa
                        |held
                        |coffle
                        |overcontraction
                        |notedly
                        |erick
                        |showrooms
                        |by-paths
                        |sulpha
                        |interments
                        |scandaled
                        |usis
                        |wamuses
                        |pelagia
                        |remission
                        |takhaar
                        |wrestle
                        |oysterbird
                        |aesthetically
                        |flounder-man
                        |belltail
                        |mitten
                        |proudishly
                        |osteophlebitis
                        |kirimon
                        |corylin
                        |amenorrhea
                        |fusain
                        |bicornate
                        |delirament
                        |centration
                        |admissible
                        |comdr
                        |mugwort
                        |badb
                        |multitentaculate
                        |rheostatics
                        |ecchondrotome
                        |rhinolalia
                        |nitroso-
                        |mauvine
                        |heatstroke
                        |petune
                        |ungambled
                        |equidivision
                        |pothooks
                        |duskiest
                        |ultra-argumentative
                        |mau-mau
                        |triturator
                        |acknew
                        |curator
                        |ciardi
                        |physed
                        |sentimentaliser
                        |interoceptor
                        |doting
                        |anti-freudianism
                        |drolet
                        |aproning
                        |undermark
                        |saponite
                        |rhino""".trimMargin().split('\n')
    runTestWithWords(words, KRadixTree(), 1, 1)
}

@Test
internal fun testComplexInsertionWithShuffle() {
    val tree = KRadixTree()
    val fileName = "test_files/words.txt"
    val numberOfLists = 10
    println("Shuffling lines in \"$fileName\" 10 times")
    val shuffledLists = List(numberOfLists) { shuffle(File(fileName).readLines().toMutableList()) }

    for ((index, shuffledList) in shuffledLists.withIndex()) {
        runTestWithWords(shuffledList, tree, index + 1, numberOfLists)
    }
}

private fun runTestWithWords(list: List<String>, tree: KRadixTree, listNumber: Int, numberOfLists: Int) {
    var stepsComplete = 1.0
    val stepsToComplete = (list.size * 2).toDouble()
    var size = tree.size
    for (item in list) {
        val word = item.trim().toLowerCase()

        if (word.isNotEmpty()) {
            tree.add(word)
            println("[$listNumber of $numberOfLists - ${((stepsComplete * 100.0) / stepsToComplete).format(2)}%] Added $word")
            assertTrue(word in tree)
            assert(tree.size - size == 1)
            size = tree.size
            stepsComplete += 1.0
        }
    }

    for (item in list) {
        val word = item.trim().toLowerCase()

        if (word == "wamuses")
            println("here we go!")

        if (word.isNotEmpty()) {
            tree.remove(word)
            println("[$listNumber of $numberOfLists - ${((stepsComplete * 100.0) / stepsToComplete).format(2)}%] Removed $word")
            assertFalse(word in tree)
            assert(tree.size - size == -1)
            size = tree.size
            stepsComplete += 1.0
        }
    }

    assert(tree.childrenAreEmpty())
}