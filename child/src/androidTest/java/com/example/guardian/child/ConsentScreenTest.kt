package com.example.guardian.child

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.guardian.child.ui.ConsentScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConsentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun continueButton_disabledUntilAcknowledged_thenInvokesCallback() {
        var consented = false
        composeRule.setContent {
            ConsentScreen(onConsent = { consented = true })
        }

        // Continue is gated behind the acknowledgement checkbox.
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()

        composeRule.onNodeWithText("I understand this device is monitored.").performClick()
        composeRule.onNodeWithText("Continue").assertIsEnabled().performClick()

        assertTrue(consented)
    }
}
