package com.hoangtu.aistorystudio.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoangtu.aistorystudio.core.navigation.StudioDestination
import com.hoangtu.aistorystudio.core.ui.SectionHeader
import com.hoangtu.aistorystudio.core.ui.StudioEmptyState
import com.hoangtu.aistorystudio.core.ui.StudioFeatureCard
import com.hoangtu.aistorystudio.core.ui.StudioPage

@Composable
fun HomeScreen(
    onNavigate: (StudioDestination) -> Unit
) {
    StudioPage(
        testTag = "screen_home",
        eyebrow = "AI STORY STUDIO",
        title = "Chào mừng bạn",
        description = "Một không gian riêng để trò chuyện, sáng tác và lưu giữ những câu chuyện của bạn."
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Câu chuyện tiếp theo bắt đầu từ đây",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Chọn một không gian sáng tác để bắt đầu.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        SectionHeader(
            title = "Bắt đầu sáng tác",
            description = "Hai không gian riêng, cùng một nơi lưu trữ."
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 600.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StudioFeatureCard(
                        icon = Icons.Filled.Favorite,
                        title = "Nhập vai",
                        description = "Khám phá những cuộc trò chuyện cùng nhân vật.",
                        onClick = { onNavigate(StudioDestination.ROLEPLAY) },
                        modifier = Modifier.weight(1f)
                    )
                    StudioFeatureCard(
                        icon = Icons.Filled.Edit,
                        title = "Tiểu thuyết",
                        description = "Phát triển ý tưởng thành câu chuyện nhiều chương.",
                        onClick = { onNavigate(StudioDestination.NOVEL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudioFeatureCard(
                        icon = Icons.Filled.Favorite,
                        title = "Nhập vai",
                        description = "Khám phá những cuộc trò chuyện cùng nhân vật.",
                        onClick = { onNavigate(StudioDestination.ROLEPLAY) }
                    )
                    StudioFeatureCard(
                        icon = Icons.Filled.Edit,
                        title = "Tiểu thuyết",
                        description = "Phát triển ý tưởng thành câu chuyện nhiều chương.",
                        onClick = { onNavigate(StudioDestination.NOVEL) }
                    )
                }
            }
        }

        SectionHeader(title = "Hoạt động gần đây")
        StudioEmptyState(
            icon = Icons.Filled.Edit,
            title = "Chưa có nội dung gần đây",
            description = "Các cuộc trò chuyện và bản thảo của bạn sẽ xuất hiện tại đây."
        )
    }
}
