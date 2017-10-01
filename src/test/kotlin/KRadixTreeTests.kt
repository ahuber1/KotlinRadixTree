import kotlinradixtree.format
import kotlinradixtree.shuffle
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

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

private fun runTestWithWords(list: List<String>, tree: KRadixTree, listNumber: Int, numberOfLists: Int) {
    var stepsComplete = 1.0
    val stepsToComplete = (list.size * 2).toDouble()
    var size = tree.size
    for (item in list) {
        val word = item.trim().toLowerCase()

        if (word.isNotEmpty()) {
            tree.add(word)
            println("[$listNumber of $numberOfLists - ${((stepsComplete * 100.0) / stepsToComplete).format(2)}%] Added $word")
            Assert.assertTrue(word in tree)
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
            Assert.assertFalse(word in tree)
            assert(tree.size - size == -1)
            size = tree.size
            stepsComplete += 1.0
        }
    }

    assert(tree.childrenAreEmpty())
}