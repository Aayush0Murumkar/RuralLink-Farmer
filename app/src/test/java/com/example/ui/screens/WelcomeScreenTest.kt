package com.example.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWelcomeScreenRenders() {
        composeTestRule.setContent {
            WelcomeScreen(onContinueAsFarmer = {})
        }
        composeTestRule.mainClock.advanceTimeBy(1000L)
    }
}
