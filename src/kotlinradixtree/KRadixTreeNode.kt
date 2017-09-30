package kotlinradixtree

import org.testng.annotations.Test
import java.io.File
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

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

                if (match.string!! == string)
                    return match

                val resultWithCharsInMatch = compareStringsWithSharedPrefix(string, match.string!!)
                val resultWithCharsInString = compareStringsWithSharedPrefix(match.string!!, string)

                return if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty() &&
                        resultWithCharsInString.suffixWhereStringsDiffer.isEmpty()) {
                    //println("Case 1")
                    match
                } else if (resultWithCharsInMatch.suffixWhereStringsDiffer.isEmpty()) {
                    //println("Case 2")
                    add(match, resultWithCharsInString.suffixWhereStringsDiffer)
                } else if (resultWithCharsInString.suffixWhereStringsDiffer.isEmpty()) {
                    //println("Case 3")
                    split(node, index, resultWithCharsInString, string)
                } else {
                    //println("Case 4")
                    split(node, index, resultWithCharsInMatch, string)
                }
            }
        }

//        private fun collapse(node: KRadixTreeNode) {
//            val onlyChild = node.children.first()
//            val words = gatherWords(onlyChild)
//            node.string += onlyChild.string
//            node.endOfWord = onlyChild.endOfWord
//            node.children.removeAt(0)
//
//            for (word in words) {
//                add(node, word)
//            }
//        }

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

        private fun gatherWords(node: KRadixTreeNode) : ArrayList<String> {
            val list = ArrayList<String>()

            for (child in node.children) {
                gatherWordsWorker(child, "", list)
            }

            return list
        }

        private fun gatherWordsWorker(node: KRadixTreeNode, builder: String, list: ArrayList<String>) {
            val str = builder + node.string!!
            list.add(str)

            for (child in node.children) {
                gatherWordsWorker(child, str, list)
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
            // If node is not the root, otherNode is still one of node's children, and if node is not at the end of a
            // word, but otherNode is, collapse otherNode into node
            else if (node.string != null && node.children[index] == otherNode && !node.endOfWord && otherNode.endOfWord) {
                swap(node, otherNode)
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
            val words = gatherWords(node)
            node.children.removeAt(index)
            add(node, resultWithEmptySuffix.prefixStringsShare)!!

            for (word in words) {
                add(node, word)
            }

            return add(node, stringBeingAdded)
        }

        private fun swap(node: KRadixTreeNode, with: KRadixTreeNode) {
            node.children = with.children
            node.endOfWord = with.endOfWord
            node.string = node.string!! + with.endOfWord
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
    internal fun testBasicInsertion() {
        val strings = arrayOf( "application", "apply", "apple" )
        runTestWithStrings(strings.asIterable())
    }

    ///@Test
    internal fun foo() {
        runTestWithStrings("""
undertaken
clamer
all-maker
renwick
water-laid
squadroning
latinism
sruti
franco-annamese
aiguillesque
dunkard
cardiopathic
cleopatre
insusceptibilities
rhapsodists
ornithichnite
lophura
closehauled
sustenances
heraclitus
ultimated
respeaks
overvoltage
torpidness
lithiasis
antilocapridae
peelhouse
hyperovaria
sheaf
colophene
jargonize
adjournal
melanthy
reordaining
disinflated
anamnestic
rachelle
unparticularising
subintroduce
deeses
dur.
spermatocyte
nearliest
kashima
unpriced
hallowedly
bretagne
mansioneer
hanya
reformism
readjustments
chiropodist
punchlike
dead-drifting
humiliant
isogenotypic
trophospongium
hamlinite
unenigmatically
interring
sylis
encina
termagantism
prinks
unrash
fat-choy
euroky
anomatheca
crumpy
endangerer
backspear
premonishment
favoured
atriopore
quintuplicating
guet-apens
resumptively
infants
hoarded
tandan
honestete
russo-serbian
antimystical
cherubim
bifold
anschauung
cisc
ramack
bugbeardom
demissness
colfin
hugged
'em
clitoridectomy
interferric
septation
katik
graphium
morga
salinas
aleger
reenforced
call-out
retroserrulate
woolie
duralumin
forevouch
rainbird
owd
communital
elettaria
bleeping
bobtail
naked-tailed
occlusometer
cross-bond
crosswind
extranormal
hotelier
house-top
monoblepsia
alcimedes
luzern
conceptible
subtiliation
schizophrene
armamentary
ehrlich
calcifying
convulsion's
nonassentation
orthometric
laming
flirted
renotarized
thymia
brass-plated
attentat
unurged
peyotism
unconditionedly
spondylexarthrosis
sandalwort
catano
hatchers
infantility
bearce
immuration
palaeoencephala
menthan
dying
cravenhearted
emperish
cutty-stool
rereign
neuroepidermal
pseudochronism
intersqueeze
centrodesmus
minibus
alining
preliquidate
labiovelarized
reacquaintance
honobia
leucotic
harbingers-of-spring
megathermal
relaxable
cold-type
graecized
collegiately
blankety
daunii
coregence
thanking
superadorn
hemorrhea
hydrochemistry
imploded
ljutomer
pinter
tephrosis
thermanesthesia
calcified
felicia
presuggestive
ethnozoological
palembang
variation's
thibetan
awaft
loose-driving
piedness
beth
            """.split("\n").map { it.trim() }.filter { it.isNotEmpty() })
    }

    ///@Test
    internal fun testComplexInsertion() {
        val root = KRadixTreeNode()
        val fileName = "test_files/words.txt"

        File(fileName).readFileLazily {
            val word = it.trim().toLowerCase()

            if (word.isNotEmpty()) {
                root.add(word)
                println("Added $word")
                assertTrue { word in root }
            }
        }

        File(fileName).readFileLazily {
            val word = it.trim().toLowerCase()

            if (word.isNotEmpty()) {
                root.remove(word)
                println("Removed $word")
                assertFalse { word in root }
            }
        }
    }

    @Test
    internal fun testComplexInsertionWithShuffle() {
        val root = KRadixTreeNode()
        val fileName = "test_files/words.txt"
        val numberOfLists = 10
        println("Shuffling lines in \"$fileName\" 10 times")
        val shuffledLists = List(numberOfLists) { shuffle(File(fileName).readLines().toMutableList()) }

        for ((index, shuffledList) in shuffledLists.withIndex()) {
            val listNumber = index + 1

            for (item in shuffledList) {
                val word = item.trim().toLowerCase()

                if (word.isNotEmpty()) {
                    root.add(word)
                    println("[$listNumber of $numberOfLists] Added $word")
                    check(word in root) { dump("$word is not in root", root) }
                }
            }

            for (item in shuffledList) {
                val word = item.trim().toLowerCase()

                if (word.isNotEmpty()) {
                    root.remove(word)
                    println("[$listNumber of $numberOfLists] Removed $word")
                    check(!(word in root)) { dump("$word IS in root", root) }
                }
            }

            assert(root.children.isEmpty())
        }
    }

    private fun dump(errorMessage: String?, node: KRadixTreeNode, indentation: String = "", lines: LinkedList<String> = LinkedList()) {
        lines.add("$indentation$node")

        for (child in node.children) {
            dump(null, child, "$indentation\t", lines)
        }

        if (errorMessage == null)
            return

        File("test_files/log.txt").writeText(lines.joinToString("\n"))
        File("test_files/log2.txt").writeText(gatherWords(node).joinToString { "\n" })
        System.err.println("Error log is in test_files/log.txt")
        fail(errorMessage)
    }

    private fun runTestWithStrings(strings: Iterable<String>) {
        val root = KRadixTreeNode()
        val stringsProcessedSoFar = ArrayList<String>()

        for (string in strings) {
            //print("Adding $string...")
            root.add(string)
            //println("$string added!")
            stringsProcessedSoFar.add(string)

            for (s in stringsProcessedSoFar) {
                //print("\tAsserting that $s is in the node...")
                assertTrue(s in root)
                //println("$s is in the node!")
            }
        }

        while (stringsProcessedSoFar.isNotEmpty()) {
            val string = stringsProcessedSoFar.first()

            //print("Removing $string from the node...")
            root.remove(string)
            //println("$string removed from the node!")
            stringsProcessedSoFar.removeAll { it == string }

            //print("Asserting that $string is no longer in the node...")
            assertFalse(string in root)
            //println("$string is not in root!")

            for (s in stringsProcessedSoFar) {
                //print("\tAsserting that $s is still in the node...")
                assertTrue(s in root)
                //println("$s is still in the node!")
            }
        }
    }
}