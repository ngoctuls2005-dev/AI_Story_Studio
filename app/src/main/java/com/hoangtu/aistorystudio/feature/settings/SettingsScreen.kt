package com.hoangtu.aistorystudio.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import com.hoangtu.aistorystudio.core.ui.StudioInfoCard
import com.hoangtu.aistorystudio.core.ui.StudioPage

@Composable
fun SettingsScreen() {
    StudioPage(
        testTag = "screen_settings",
        eyebrow = "TÙY CHỈNH",
        title = "Cài đặt",
        description = "Quản lý trải nghiệm ứng dụng của bạn."
    ) {
        StudioInfoCard(
            icon = Icons.Filled.Settings,
            title = "Giao diện",
            description = "Ứng dụng tự điều chỉnh sáng hoặc tối theo thiết bị."
        )
        StudioInfoCard(
            icon = Icons.Filled.Settings,
            title = "AI Story Studio",
            description = "Ứng dụng sáng tác cá nhân · phiên bản 1.0"
        )
    }
}
