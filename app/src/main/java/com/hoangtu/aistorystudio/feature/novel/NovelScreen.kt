package com.hoangtu.aistorystudio.feature.novel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import com.hoangtu.aistorystudio.core.ui.StudioEmptyState
import com.hoangtu.aistorystudio.core.ui.StudioPage

@Composable
fun NovelScreen() {
    StudioPage(
        testTag = "screen_novel",
        eyebrow = "NOVEL MODE",
        title = "Tiểu thuyết",
        description = "Tổ chức dự án, chương và cảnh trong một không gian sáng tác riêng."
    ) {
        StudioEmptyState(
            icon = Icons.Filled.Edit,
            title = "Chưa có dự án nào",
            description = "Khi bắt đầu một dự án, dàn ý và bản thảo sẽ được sắp xếp tại đây."
        )
    }
}
