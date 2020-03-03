package com.ahuber.test.utils

import com.ahuber.utils.DiffResultTests
import java.io.File
import java.net.URI
import java.net.URL

fun getResourceAsUrl(name: String): URL? = DiffResultTests::class.java.classLoader.getResource(name)

fun getResourceAsUri(name : String): URI? = getResourceAsUrl(name)?.toURI()

fun getResourceAsFile(name: String): File? = when (val uri = getResourceAsUri(name)) {
    null -> null
    else -> File(uri)
}