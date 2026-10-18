package com.hoangtu.aistorystudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.hoangtu.aistorystudio.feature.settings.SettingsViewModel
import com.hoangtu.aistorystudio.core.navigation.StudioApp
import com.hoangtu.aistorystudio.ui.theme.AIStoryStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsViewModel = ViewModelProvider(
            this,
            (application as StudioApplication).settingsViewModelFactory
        )[SettingsViewModel::class.java]
        setContent {
            AIStoryStudioTheme {
                StudioApp(settingsViewModel)
            }
        }
    }
}
