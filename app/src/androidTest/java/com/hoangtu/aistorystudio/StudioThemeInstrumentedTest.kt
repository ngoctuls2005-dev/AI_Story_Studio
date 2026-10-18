package com.hoangtu.aistorystudio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoangtu.aistorystudio.ui.theme.AIStoryStudioTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioThemeInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightAndDarkModesUseDifferentBackgroundColors() {
        val darkMode = mutableStateOf(false)
        val backgroundColor = AtomicReference(Color.Unspecified)

        composeRule.setContent {
            AIStoryStudioTheme(
                darkTheme = darkMode.value,
                dynamicColor = false
            ) {
                val currentBackground = MaterialTheme.colorScheme.background
                SideEffect {
                    backgroundColor.set(currentBackground)
                }
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        val lightBackground = composeRule.runOnIdle { backgroundColor.get() }
        composeRule.runOnIdle {
            darkMode.value = true
        }
        composeRule.waitForIdle()
        val darkBackground = composeRule.runOnIdle { backgroundColor.get() }

        assertNotEquals(lightBackground, darkBackground)
    }
}
