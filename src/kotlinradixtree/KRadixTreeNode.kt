package kotlinradixtree

import org.testng.annotations.Test
import java.io.File
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class KRadixTreeNode {

    private var string: String?
    private var endOfWord: Boolean
    private var children = ArrayList<KRadixTreeNode>()

    internal constructor() {
        string = null
        endOfWord = false
    }

    internal constructor(string: String, endOfWord: Boolean) {
        this.string = string
        this.endOfWord = endOfWord
    }

    internal fun add(string: String) {
        check(this.string == null) { "One cannot call add(String) on anything other than the root node" }
        check(string.isNotEmpty()) { "Invalid String \"$string\": one cannot add an empty string into a radix tree" }
        check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
            "Invalid String \"$string\": one cannot add strings that contain whitespace characters or uppercase letters."
        }
        add(this, string)
    }

    internal operator fun contains(string: String): Boolean {
        if (string.isEmpty())
            return false // empty strings cannot be added into the radix tree
        if (string.toCharArray().any { it.isWhitespace() || it.isUpperCase() } )
            return false // cannot have characters with whitespace or with uppercase letters

        return contains(this, string)
    }

    internal fun remove(string: String) : Boolean {
        check(this.string == null) { "You cannot call remove(String) on anything other than the root node" }
        check(!string.toCharArray().any { it.isWhitespace() || it.isUpperCase() }) {
            "Invalid String \"$string\": one cannot remove strings that contain whitespace characters or uppercase letters."
        }

        return if (string.isNotEmpty()) remove(this, string) else false // you cannot add empty strings; as such, you cannot remove them
    }

    companion object {
        private fun add(node: KRadixTreeNode, string: String): KRadixTreeNode? {
            if (string.isEmpty())
                return null

            val index = indexOfLongestStringInChildren(node, string)

            if (index == null) {
                val indexDataShouldBeAt = searchAmongChildren(node, string) as IndexDataShouldBeAt
                val newNode = KRadixTreeNode(string, true)
                node.children.add(indexDataShouldBeAt.index, newNode)
                return newNode
            }
            else {
                val match = node.children[index]

                if (match.string!! == string) {
                    match.endOfWord = true
                    return match
                }

                val resultWithCharsInMatch = compareStringsWithSharedPrefix(string, match.string!!)
                val resultWithCharsInString = compareStringsWithSharedPrefix(match.string!!, string)

                return if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty() &&
                        resultWithCharsInString.suffixWhereStringsDiffer.isEmpty()) {
                    //println("Case 1")
                    match
                } else if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty()) {
                    //println("Case 2")
                    add(match, resultWithCharsInString.suffixWhereStringsDiffer)
                } else if (resultWithCharsInString.suffixWhereStringsDiffer.isNotEmpty()) {
                    //println("Case 3")
                    split(node, index, resultWithCharsInString, string)
                } else {
                    //println("Case 4")
                    split(node, index, resultWithCharsInMatch, string)
                }
            }
        }

        private fun combine(node: KRadixTreeNode, with: KRadixTreeNode) {
            node.children = with.children
            node.endOfWord = with.endOfWord
            node.string = node.string!! + with.string
        }

        private fun contains(node: KRadixTreeNode, str: String) : Boolean {
            val index = indexOfLongestStringInChildren(node, str) ?: return false // not found
            val result = compareStringsWithSharedPrefix(node.children[index].string!!, str)

            if (node.children[index].string!! == result.prefixStringsShare) {
                if (result.suffixWhereStringsDiffer.isEmpty())
                    return node.children[index].endOfWord

                return contains(node.children[index], result.suffixWhereStringsDiffer)
            }

            return false
        }

        private fun indexOfLongestStringInChildren(node: KRadixTreeNode, string: String) : Int? {
            var index = 0
            val matches = LinkedList<KRadixTreeNode?>()
            val builder = StringBuilder()

            do {
                builder.append(string[index++])
                matches.addLast(node.children.find { it.string!!.startsWith(builder) })
            }  while(builder.length < string.length && matches.last != null)

            val last = matches.findLast { it != null }

            return if (last != null)
                node.children.indexOf(last)
            else
                null
        }

        private fun getCompleteWords(node: KRadixTreeNode) : List<String> {
            val list = LinkedList<String>()

            for (child in node.children) {
                getCompleteWordsWorker(child, "", list)
            }

            return list
        }

        private fun getCompleteWordsWorker(node: KRadixTreeNode, builder: String, list: LinkedList<String>) {
            val str = builder + node.string!!

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

            val index = indexOfLongestStringInChildren(node, str) ?: return false // no match found
            val otherNode = node.children[index]
            val result = compareStringsWithSharedPrefix(otherNode.string!!, str)
            val removalWasSuccessful = remove(otherNode, result.suffixWhereStringsDiffer)

            if (!removalWasSuccessful)
                return removalWasSuccessful

            if (otherNode.string!! == str) {
                if (otherNode.children.isEmpty())
                    node.children.removeAt(index)
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

        private fun searchAmongChildren(node: KRadixTreeNode, string: String) : KRadixTreeNodeIndex {
            return if (node.children.isEmpty())
                IndexDataShouldBeAt(0)
            else
                searchAmongChildren(node, string, 0, node.children.lastIndex / 2, node.children.lastIndex)
        }

        private fun searchAmongChildren(node: KRadixTreeNode, string: String, startIndex: Int, middleIndex: Int, endIndex: Int) : KRadixTreeNodeIndex {
            val middleString = node.children[middleIndex].string!!

            return when {
                string == middleString -> IndexDataWasFound(middleIndex)
                endIndex - startIndex == 0 -> middleIndex.indexDataShouldBeAt(string < middleString)
                else -> {
                    val newStartIndex: Int
                    val newEndIndex: Int

                    if (string < middleString) {
                        newStartIndex = startIndex
                        newEndIndex = middleIndex
                    }
                    else {
                        newStartIndex = middleIndex + 1
                        newEndIndex = endIndex
                    }

                    val newMiddleIndex = ((newEndIndex - newStartIndex) / 2) + newStartIndex

                    return searchAmongChildren(node, string, newStartIndex, newMiddleIndex, newEndIndex)
                }
            }
        }

        private fun split(node: KRadixTreeNode, index: Int, resultWithEmptySuffix: KRadixTreeStringComparisonResult,
                          stringBeingAdded: String): KRadixTreeNode? {
            val words = getCompleteWords(node)
            node.children.removeAt(index)
            add(node, resultWithEmptySuffix.prefixStringsShare)!!.endOfWord = false

            for (word in words) {
                add(node, word)
            }

            return add(node, stringBeingAdded)
        }
    }

    override fun toString(): String {
        val childrenString = "[ ${children.map { if (it.endOfWord) "${it.string!!}*" else it.string!! }.joinToString(", ")} ]"
        var string = this.string ?: ""

        if (this.endOfWord)
            string += "*"

        string = "\"$string\""

        return "KRadixTreeNode(string = $string, children = $childrenString)"
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
        runTestWithWords(words, KRadixTreeNode(), 1, 1)
    }

    @Test
    internal fun testComplexInsertionWithShuffle() {
        val root = KRadixTreeNode()
        val fileName = "test_files/words.txt"
        val numberOfLists = 10
        println("Shuffling lines in \"$fileName\" 10 times")
        val shuffledLists = List(numberOfLists) { shuffle(File(fileName).readLines().toMutableList()) }

        for ((index, shuffledList) in shuffledLists.withIndex()) {
            runTestWithWords(shuffledList, root, index + 1, numberOfLists)
        }
    }

    private fun runTestWithWords(list: List<String>, root: KRadixTreeNode, listNumber: Int, numberOfLists: Int) {
        var words = getCompleteWords(root)
        var stepsComplete = 1.0
        val stepsToComplete = (list.size * 2).toDouble()
        for (item in list) {
            val word = item.trim().toLowerCase()

            if (word == "rhino")
                println("here we go!")

            if (word.isNotEmpty()) {
                root.add(word)
                println("[$listNumber of $numberOfLists - ${((stepsComplete * 100.0) / stepsToComplete).format(2)}%] Added $word")
                //val list = getCompleteWords(root)
                //val difference = list.size - words.size
                assertTrue(word in root)
                //assert(difference == 1) { "Missing ${list.filter { it !in words }.joinToString(", ")}" }
                words = list
                stepsComplete += 1.0
            }
        }

        for (item in list) {
            val word = item.trim().toLowerCase()

            if (word.isNotEmpty()) {
                root.remove(word)
                println("[$listNumber of $numberOfLists - ${((stepsComplete * 100.0) / stepsToComplete).format(2)}%] Removed $word")
                //val list = getCompleteWords(root)
                //val difference = words.size - list.size
                assertFalse(word in root)
                //assert(difference == 1) { "Missing ${words.filter { it !in list }.joinToString(", ")}" }
                words = list
                stepsComplete += 1.0
            }
        }

        assert(root.children.isEmpty()) { root.toString() }
    }
}