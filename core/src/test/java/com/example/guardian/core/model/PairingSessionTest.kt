package com.example.guardian.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingSessionTest {

    @Test
    fun `status parses and degrades gracefully`() {
        assertEquals(PairingStatus.PENDING, PairingSession(status = "PENDING").pairingStatus)
        assertEquals(PairingStatus.CLAIMED, PairingSession(status = "CLAIMED").pairingStatus)
        assertEquals(PairingStatus.UNKNOWN, PairingSession(status = "???").pairingStatus)
    }

    @Test
    fun `isExpired respects the deadline and ignores unset expiry`() {
        val session = PairingSession(expiresAt = 1_000L)
        assertFalse(session.isExpired(999L))
        assertTrue(session.isExpired(1_000L))
        assertTrue(session.isExpired(2_000L))
        // expiresAt == 0 means "no deadline set" and must never read as expired.
        assertFalse(PairingSession(expiresAt = 0L).isExpired(Long.MAX_VALUE))
    }
}
