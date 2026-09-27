package com.example.guardian.core.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingPayloadTest {

    @Test
    fun `encode then decode round-trips`() {
        val encoded = PairingPayload.encode("012345")
        assertEquals("guardian://pair?code=012345", encoded)
        assertEquals("012345", PairingPayload.decode(encoded))
    }

    @Test
    fun `decode tolerates a bare code`() {
        assertEquals("123456", PairingPayload.decode("123456"))
        assertEquals("123456", PairingPayload.decode("  123 456 "))
    }

    @Test
    fun `decode rejects junk and wrong-length codes`() {
        assertNull(PairingPayload.decode(null))
        assertNull(PairingPayload.decode("guardian://pair?code=12ab56"))
        assertNull(PairingPayload.decode("hello"))
        assertNull(PairingPayload.decode("12345"))
    }
}
