package com.hoangtu.aistorystudio.feature.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import com.hoangtu.aistorystudio.core.ui.StudioEmptyState
import com.hoangtu.aistorystudio.core.ui.StudioPage

@Composable
fun LibraryScreen() {
    StudioPage(
        testTag = "screen_library",
        eyebrow = "KHÔNG GIAN CỦA BẠN",
        title = "Thư viện",
        description = "Truy cập nhanh nội dung đã lưu trong AI Story Studio."
    ) {
        StudioEmptyState(
            icon = Icons.AutoMirrored.Filled.List,
            title = "Thư viện đang trống",
            description = "Nội dung bạn tạo sẽ được nhóm tại đây để dễ tìm lại."
        )
    }
}
