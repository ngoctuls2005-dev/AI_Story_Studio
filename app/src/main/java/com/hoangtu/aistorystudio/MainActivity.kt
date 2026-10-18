package com.hoangtu.aistorystudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hoangtu.aistorystudio.core.navigation.StudioApp
import com.hoangtu.aistorystudio.ui.theme.AIStoryStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIStoryStudioTheme {
                StudioApp()
            }
        }
    }
}
