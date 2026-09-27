package com.example.guardian.child.command

import android.content.Context
import com.example.guardian.child.admin.DeviceAdminManager
import com.example.guardian.child.data.PolicyRepository
import com.example.guardian.child.enforcement.LockController
import com.example.guardian.core.model.Command
import com.example.guardian.core.model.CommandType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandExecutorTest {

    private val context = mockk<Context>(relaxed = true)
    private val adminManager = mockk<DeviceAdminManager>(relaxed = true)
    private val policyRepository = mockk<PolicyRepository>(relaxed = true)
    private val lockController = mockk<LockController>(relaxed = true)
    private val executor = CommandExecutor(context, adminManager, policyRepository, lockController)

    @Test
    fun `LOCK raises the Guardian lock and turns the screen off when admin is active`() = runTest {
        every { adminManager.isActive() } returns true

        val result = executor.execute(Command(type = CommandType.LOCK.name))

        assertTrue(result is CommandResult.Success)
        verify { lockController.lock() }
        verify { adminManager.lockNow() }
    }

    @Test
    fun `LOCK still succeeds (shows lock screen) when admin is inactive`() = runTest {
        every { adminManager.isActive() } returns false

        val result = executor.execute(Command(type = CommandType.LOCK.name))

        assertTrue(result is CommandResult.Success)
        verify { lockController.lock() }
        verify(exactly = 0) { adminManager.lockNow() }
    }

    @Test
    fun `UNLOCK clears the lock`() = runTest {
        val result = executor.execute(Command(type = CommandType.UNLOCK.name))

        assertTrue(result is CommandResult.Success)
        verify { lockController.unlock() }
    }

    @Test
    fun `REFRESH_POLICY re-syncs the policy`() = runTest {
        coEvery { policyRepository.syncOnce() } returns null

        val result = executor.execute(Command(type = CommandType.REFRESH_POLICY.name))

        assertTrue(result is CommandResult.Success)
        coVerify(exactly = 1) { policyRepository.syncOnce() }
    }

    @Test
    fun `unknown command type fails loudly`() = runTest {
        val result = executor.execute(Command(type = "TELEPORT"))
        assertTrue(result is CommandResult.Failure)
    }
}
