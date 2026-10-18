package com.hoangtu.aistorystudio.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class StudioDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Trang chủ", Icons.Filled.Home),
    ROLEPLAY("roleplay", "Nhập vai", Icons.Filled.Favorite),
    NOVEL("novel", "Tiểu thuyết", Icons.Filled.Edit),
    CHARACTERS("characters", "Nhân vật", Icons.Filled.Person),
    LIBRARY("library", "Thư viện", Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", "Cài đặt", Icons.Filled.Settings)
}
