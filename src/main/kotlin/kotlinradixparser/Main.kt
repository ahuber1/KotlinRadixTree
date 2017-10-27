package kotlinradixparser;

import kotlinradixtree.readFileLazilyCharacterByCharacter
import java.io.File
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val time = measureTimeMillis {
//        println("Reading file...")
//        val content = File("test_files/shakespeare-romeo.txt").readText().toLowerCase()
//        println("File read!")

        println("Reading file...")
        val buffer = StringBuffer()

        File("test_files/shakespeare-romeo.txt").readFileLazilyCharacterByCharacter {
            if (!it.isWhitespace()) {
                print(it)
                buffer.append(it.toLowerCase())
            }
        }

        println("File read!")

        val content = buffer.toString()

        content.radixParse {
            for (kotlinRadixParserResult in it) {
                println(kotlinRadixParserResult)
            }
            return@radixParse false
        }
    }

    val seconds = time.toDouble() / 1000.0
    println("$seconds seconds")

}
