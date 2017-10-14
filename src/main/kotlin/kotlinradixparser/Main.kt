package kotlinradixparser;

fun main(args: Array<String>) {
    for (list in ".the quick brown fox jumped over the lazy sheep dog".radixParse()) {
        println("Result ===========================================================")
        for (kotlinRadixParserResult in list) {
            println(kotlinRadixParserResult)
            Thread.sleep(1500)
        }
        println("==================================================================")
    }
}
