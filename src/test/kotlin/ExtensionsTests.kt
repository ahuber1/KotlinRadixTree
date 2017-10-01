import kotlinradixtree.readFileLazily
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File
import java.util.ArrayList

@Test
fun `Test readFileLazily`() {
    val originalContent = File("README.md").readLines().joinToString(separator = "\n")
    val lines = ArrayList<String>()
    File("README.md").readFileLazily { lines.add(it) }
    val lazilyReadContent = lines.joinToString(separator = "\n")
    Assert.assertEquals(originalContent, lazilyReadContent)

    println("originalContent = \n$originalContent")
    println()
    println("lazilyReadContent = \n$lazilyReadContent")
}