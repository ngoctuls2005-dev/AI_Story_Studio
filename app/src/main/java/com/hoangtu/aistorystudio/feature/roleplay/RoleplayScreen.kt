package com.hoangtu.aistorystudio.feature.roleplay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import com.hoangtu.aistorystudio.core.ui.StudioEmptyState
import com.hoangtu.aistorystudio.core.ui.StudioPage

@Composable
fun RoleplayScreen(
    onOpenCharacters: () -> Unit
) {
    StudioPage(
        testTag = "screen_roleplay",
        eyebrow = "ROLEPLAY MODE",
        title = "Nhập vai",
        description = "Trò chuyện trong những thế giới và mối quan hệ do bạn tạo nên."
    ) {
        StudioEmptyState(
            icon = Icons.Filled.Favorite,
            title = "Chưa có cuộc trò chuyện",
            description = "Tạo hồ sơ nhân vật trước để chuẩn bị cho cuộc trò chuyện đầu tiên.",
            actionLabel = "Mở nhân vật",
            onAction = onOpenCharacters
        )
    }
}
