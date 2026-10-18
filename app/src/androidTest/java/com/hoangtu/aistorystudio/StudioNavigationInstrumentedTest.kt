package com.hoangtu.aistorystudio

import androidx.lifecycle.Lifecycle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioNavigationInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensAllSixScreens() {
        val routes = listOf(
            "home",
            "roleplay",
            "novel",
            "characters",
            "library",
            "settings"
        )

        routes.forEach { route ->
            composeRule.onNodeWithTag("navigation_" + route).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("screen_" + route).assertIsDisplayed()
        }
    }

    @Test
    fun systemBackReturnsFromTopLevelScreenToHome() {
        composeRule.onNodeWithTag("navigation_roleplay").performClick()
        composeRule.onNodeWithTag("screen_roleplay").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    }

    @Test
    fun selectedScreenIsRestoredAfterActivityRecreation() {
        composeRule.onNodeWithTag("navigation_library").performClick()
        composeRule.onNodeWithTag("screen_library").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_library").assertIsDisplayed()
    }

    @Test
    fun selectedScreenRemainsAfterBackgroundAndResume() {
        composeRule.onNodeWithTag("navigation_novel").performClick()
        composeRule.onNodeWithTag("screen_novel").assertIsDisplayed()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_novel").assertIsDisplayed()
    }
}
