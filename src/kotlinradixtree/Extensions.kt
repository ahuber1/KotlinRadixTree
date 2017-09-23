package kotlinradixtree

import org.testng.annotations.Test
import java.io.File
import java.util.*
import kotlin.test.assertEquals

fun File.readFileLazily(action: (String) -> Unit) {
    Scanner(this).use {
        while (it.hasNext()) {
            action(it.nextLine())
        }
    }
}

@Test fun `Test readFileLazily`() {
    val originalContent = File("README.md").readLines().joinToString(separator = "\n")
    val lines = ArrayList<String>()
    File("README.md").readFileLazily { lines.add(it) }
    val lazilyReadContent = lines.joinToString(separator = "\n")
    assertEquals(originalContent, lazilyReadContent)

    println("originalContent = \n${originalContent}")
    println()
    println("lazilyReadContent = \n${lazilyReadContent}")
}