package com.example.guardian.parent.ui.auth

import com.example.guardian.parent.data.AuthRepository
import com.example.guardian.parent.data.AuthResult
import com.example.guardian.parent.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<AuthRepository>(relaxed = true).also {
        every { it.authState() } returns flowOf(null)
    }

    @Test
    fun `sign-in error surfaces the message and clears loading`() = runTest {
        coEvery { repo.signIn(any(), any()) } returns AuthResult.Error("Incorrect email or password.")
        val vm = AuthViewModel(repo)

        vm.signIn("a@b.com", "secret1")

        assertEquals("Incorrect email or password.", vm.ui.value.error)
        assertFalse(vm.ui.value.loading)
    }

    @Test
    fun `sign-in success leaves no error`() = runTest {
        coEvery { repo.signIn(any(), any()) } returns AuthResult.Success(mockk<FirebaseUser>())
        val vm = AuthViewModel(repo)

        vm.signIn("a@b.com", "secret1")

        assertNull(vm.ui.value.error)
        assertFalse(vm.ui.value.loading)
    }

    @Test
    fun `clearError resets the error`() = runTest {
        coEvery { repo.signUp(any(), any()) } returns AuthResult.Error("An account already exists.")
        val vm = AuthViewModel(repo)
        vm.signUp("a@b.com", "secret1")
        assertEquals("An account already exists.", vm.ui.value.error)

        vm.clearError()

        assertNull(vm.ui.value.error)
    }
}
