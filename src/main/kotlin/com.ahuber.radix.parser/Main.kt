package com.ahuber.radix.parser

import java.io.File

fun main() {
    println("Reading file...")
    val buffer = StringBuffer()

    val characters = File("test_files/shakespeare-romeo.txt").readLines()
            .asSequence()
            .flatMap { it.toCharArray().asSequence() }

    for (character in characters) {
        if (character.isWhitespace()) {
            continue
        }
        print(character)
        buffer.append(character.toLowerCase())
    }

    println("File read!")

    val content = buffer.toString()
    var counter = 0

    content.radixParse {
        println("Found ${counter++} results")
        return@radixParse true
    }

    println("Done")
}