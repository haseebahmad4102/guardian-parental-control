package com.example.guardian.parent

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.guardian.core.usage.AppUsage
import com.example.guardian.parent.ui.device.UsageChart
import com.example.guardian.parent.ui.theme.GuardianTheme
import org.junit.Rule
import org.junit.Test

class UsageChartTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTopApps_andCollapsesTheRest() {
        val apps = (1..10).map { AppUsage(pkg = "com.app$it", label = "App $it", minutes = (11 - it) * 10L) }

        composeRule.setContent {
            GuardianTheme { UsageChart(apps = apps, maxItems = 8) }
        }

        // Largest first, and its formatted duration is shown.
        composeRule.onNodeWithText("App 1").assertIsDisplayed()
        composeRule.onNodeWithText("1h 40m").assertIsDisplayed() // 100 minutes
        // The 9th and 10th apps are collapsed into a summary line.
        composeRule.onNodeWithText("+ 2 more apps").assertIsDisplayed()
    }
}
