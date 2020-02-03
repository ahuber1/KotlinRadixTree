package com.ahuber.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffResultTests {
    @Test
    fun `test diff with identical words`() {
        assertTrue("apple".diffWith("apple") is DiffResult.Identical)
    }

    @Test
    fun `test diff with common prefix`() {
        "apple".diffWith("application").apply {
            assertTrue(this is DiffResult.Shared)
            assertEquals("appl", this.sharedPrefix)
            assertEquals("e", this.remainder)
        }

        "application".diffWith("apple").apply {
            assertTrue(this is DiffResult.Shared)
            assertEquals("appl", this.sharedPrefix)
            assertEquals("ication", this.remainder)
        }
    }

    @Test
    fun `test diff for different words`() {
        assertTrue("apple".diffWith("banana") is DiffResult.Different)
    }
}