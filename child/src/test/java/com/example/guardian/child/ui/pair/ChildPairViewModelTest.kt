package com.example.guardian.child.ui.pair

import com.example.guardian.child.MainDispatcherRule
import com.example.guardian.child.data.ChildPairingRepository
import com.example.guardian.child.data.ClaimResult
import com.example.guardian.child.data.DeviceIdentity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChildPairViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<ChildPairingRepository>()

    @Test
    fun `invalid code shows an error and never calls the repository`() = runTest {
        val vm = ChildPairViewModel(repo)

        vm.submit("12ab")

        assertNotNull(vm.ui.value.error)
        assertFalse(vm.ui.value.submitting)
        coVerify(exactly = 0) { repo.claim(any()) }
    }

    @Test
    fun `valid code claims via the repository`() = runTest {
        coEvery { repo.claim("123456") } returns
            ClaimResult.Success(DeviceIdentity("d1", "p1", "Amir"))
        val vm = ChildPairViewModel(repo)

        vm.submit("123456")

        assertNull(vm.ui.value.error)
        assertFalse(vm.ui.value.submitting)
        coVerify(exactly = 1) { repo.claim("123456") }
    }

    @Test
    fun `claim error is surfaced`() = runTest {
        coEvery { repo.claim(any()) } returns ClaimResult.Error("That code has expired.")
        val vm = ChildPairViewModel(repo)

        vm.submit("123456")

        assertNotNull(vm.ui.value.error)
        assertFalse(vm.ui.value.submitting)
    }
}
