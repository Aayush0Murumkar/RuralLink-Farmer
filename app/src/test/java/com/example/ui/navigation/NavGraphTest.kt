package com.example.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NavGraphTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationToDashboard() {
        composeTestRule.setContent {
            RuralLinkFarmerAppNavHost()
        }
        
        // Wait for Welcome Screen
        composeTestRule.waitForIdle()
        
        // Click Continue as Farmer
        composeTestRule.onNodeWithTag("continue_as_farmer_button").performClick()
        
        // Wait for Dashboard to render
        composeTestRule.waitForIdle()
    }
}
