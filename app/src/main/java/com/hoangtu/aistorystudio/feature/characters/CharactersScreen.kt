package com.hoangtu.aistorystudio.feature.characters

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import com.hoangtu.aistorystudio.core.ui.StudioEmptyState
import com.hoangtu.aistorystudio.core.ui.StudioPage

@Composable
fun CharactersScreen() {
    StudioPage(
        testTag = "screen_characters",
        eyebrow = "HỒ SƠ DÙNG CHUNG",
        title = "Nhân vật",
        description = "Tập hợp tính cách, mối quan hệ và những chi tiết bạn muốn giữ nhất quán."
    ) {
        StudioEmptyState(
            icon = Icons.Filled.Person,
            title = "Chưa có hồ sơ nhân vật",
            description = "Nhân vật có thể được dùng trong các câu chuyện và cuộc nhập vai."
        )
    }
}
