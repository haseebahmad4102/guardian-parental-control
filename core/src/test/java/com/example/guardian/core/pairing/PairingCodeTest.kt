package com.example.guardian.core.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PairingCodeTest {

    @Test
    fun `generated code is always six digits`() {
        val random = Random(42)
        repeat(1000) {
            val code = PairingCode.generate(random)
            assertEquals(6, code.length)
            assertTrue(PairingCode.isValid(code))
        }
    }

    @Test
    fun `small numbers are zero-padded`() {
        // Random seeded so the first draw is small; assert padding regardless of value.
        val code = PairingCode.generate(Random(0))
        assertEquals(6, code.length)
    }

    @Test
    fun `validation rejects wrong length and non-digits`() {
        assertFalse(PairingCode.isValid("12345"))
        assertFalse(PairingCode.isValid("1234567"))
        assertFalse(PairingCode.isValid("12a456"))
        assertFalse(PairingCode.isValid(null))
        assertTrue(PairingCode.isValid("000000"))
    }

    @Test
    fun `normalize strips spaces and trims`() {
        assertEquals("123456", PairingCode.normalize("  123 456 "))
    }
}
