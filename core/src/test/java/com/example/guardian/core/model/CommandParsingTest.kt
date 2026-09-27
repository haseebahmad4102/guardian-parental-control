package com.example.guardian.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandParsingTest {

    @Test
    fun `known command type parses`() {
        val cmd = Command(type = "LOCK", status = "PENDING")
        assertEquals(CommandType.LOCK, cmd.commandType)
        assertEquals(CommandStatus.PENDING, cmd.commandStatus)
    }

    @Test
    fun `unknown command type degrades to UNKNOWN instead of throwing`() {
        val cmd = Command(type = "TELEPORT", status = "???")
        assertEquals(CommandType.UNKNOWN, cmd.commandType)
        assertEquals(CommandStatus.UNKNOWN, cmd.commandStatus)
    }

    @Test
    fun `null owner mode degrades to UNKNOWN`() {
        assertEquals(OwnerMode.UNKNOWN, OwnerMode.fromValue(null))
        assertEquals(OwnerMode.DEVICE_OWNER, OwnerMode.fromValue("DEVICE_OWNER"))
    }
}
