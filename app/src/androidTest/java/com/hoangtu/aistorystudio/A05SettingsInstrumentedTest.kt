package com.hoangtu.aistorystudio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A05SettingsInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun defaultProviderCanSaveHideValidateAndDeleteApiKey() {
        val testApiKey = "test-only-api-key-fixture-a05"
        composeRule.onNodeWithTag("navigation_settings").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Experiential Labs").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("provider_edit_experiential-labs").performClick()
        composeRule.onNodeWithTag("api_key_input").performTextInput(testApiKey)
        composeRule.onNodeWithTag("api_key_visibility_toggle").performClick()
        composeRule.onNodeWithText("Ẩn").assertIsDisplayed()
        composeRule.onNodeWithTag("api_key_visibility_toggle").performClick()
        composeRule.onNodeWithText("Hiện").assertIsDisplayed()

        composeRule.onNodeWithTag("provider_save").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Đã lưu khóa an toàn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Cấu hình hợp lệ cục bộ; chưa kiểm tra mạng").assertExists()
        assertTrue(composeRule.onAllNodesWithText(testApiKey).fetchSemanticsNodes().isEmpty())
        val preferencesFile = File(
            composeRule.activity.filesDir,
            "datastore/ai_provider_settings.preferences_pb"
        )
        assertTrue(preferencesFile.exists())
        val storedBytes = String(preferencesFile.readBytes(), StandardCharsets.ISO_8859_1)
        assertFalse(storedBytes.contains(testApiKey))

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Đã lưu khóa an toàn").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("api_key_delete_experiential-labs").performScrollTo().performClick()
        composeRule.onNodeWithTag("api_key_confirm_delete").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("API key đã bị xóa.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Chưa có API key").assertIsDisplayed()
    }
}
