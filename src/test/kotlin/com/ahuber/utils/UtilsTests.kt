package com.ahuber.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTests {
    @Test
    fun `test string repeat`() {
        assertTrue("-".repeat(0).isEmpty())
        assertEquals("-", "-".repeat(1))
        assertEquals("--", "-".repeat(2))
    }
}