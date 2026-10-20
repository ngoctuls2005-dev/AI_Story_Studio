package com.hoangtu.aistorystudio.feature.roleplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.model.ConversationHeader
import com.hoangtu.aistorystudio.core.ui.StudioEmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collect

@Composable
fun RoleplayScreen(
    viewModel: RoleplayViewModel,
    onOpenCharacters: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RoleplayEvent.OpenConversation -> onOpenConversation(event.id)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("screen_roleplay"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ROLEPLAY MODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Nhập vai", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Mỗi cuộc hội thoại giữ riêng nhân vật, Persona và lịch sử đã lưu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier.fillMaxWidth().testTag("conversation_search"),
            singleLine = true,
            label = { Text("Tìm cuộc hội thoại") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    TextButton(onClick = { viewModel.updateSearchQuery("") }) { Text("Xóa") }
                }
            }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = viewModel::beginCreateConversation,
                enabled = !state.isLoading && state.characters.isNotEmpty(),
                modifier = Modifier.testTag("create_conversation")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tạo hội thoại")
            }
        }

        when {
            state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = viewModel::retryLoading) { Text("Thử lại") }
            }
            state.conversations.isEmpty() && state.searchQuery.isBlank() -> StudioEmptyState(
                icon = Icons.Filled.Favorite,
                title = if (state.characters.isEmpty()) "Chưa có nhân vật" else "Chưa có cuộc trò chuyện",
                description = if (state.characters.isEmpty()) {
                    "Tạo hồ sơ nhân vật trước khi bắt đầu một cuộc trò chuyện."
                } else {
                    "Các cuộc hội thoại mới sẽ được lưu riêng theo nhân vật và Persona."
                },
                actionLabel = if (state.characters.isEmpty()) "Mở nhân vật" else null,
                onAction = if (state.characters.isEmpty()) onOpenCharacters else null
            )
            state.conversations.isEmpty() -> StudioEmptyState(
                icon = Icons.Filled.Search,
                title = "Không tìm thấy kết quả",
                description = "Thử tìm bằng tiêu đề, tên nhân vật hoặc Persona."
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("conversation_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.conversations, key = { it.conversation.id }) { header ->
                    ConversationListCard(
                        header = header,
                        onOpen = { onOpenConversation(header.conversation.id) },
                        onDelete = { viewModel.requestDelete(header) }
                    )
                }
            }
        }

        state.message?.let {
            Text(
                text = it,
                color = if (state.isMessageError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("conversation_message")
            )
        }
    }

    state.createDraft?.let { draft ->
        CreateConversationDialog(
            state = state,
            draft = draft,
            onTitleChange = viewModel::updateCreateTitle,
            onCharacterSelected = viewModel::selectCreateCharacter,
            onPersonaSelected = viewModel::selectCreatePersona,
            onDismiss = viewModel::cancelCreateConversation,
            onCreate = viewModel::createConversation
        )
    }

    state.pendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Xóa cuộc hội thoại?") },
            text = {
                Text("Lịch sử tin nhắn của ‘${pending.conversation.title}’ cũng sẽ bị xóa. Thao tác này không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    modifier = Modifier.testTag("confirm_delete_conversation")
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Hủy") } }
        )
    }
}

@Composable
private fun ConversationListCard(
    header: ConversationHeader,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("conversation_row_${header.conversation.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(header.conversation.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = buildString {
                        append(header.characterName)
                        header.personaName?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatActivityTime(header.conversation.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_conversation_${header.conversation.id}")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Xóa ${header.conversation.title}")
            }
        }
    }
}

@Composable
private fun CreateConversationDialog(
    state: RoleplayUiState,
    draft: NewConversationDraft,
    onTitleChange: (String) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onPersonaSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cuộc hội thoại mới") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth().testTag("new_conversation_title"),
                    singleLine = true,
                    label = { Text("Tiêu đề") }
                )
                SelectionField(
                    label = "Nhân vật",
                    emptyLabel = "Chọn nhân vật",
                    selectedLabel = state.characters.firstOrNull { it.id == draft.characterId }?.name,
                    options = state.characters.map { it.id to it.name },
                    testTag = "new_conversation_character",
                    onSelected = { onCharacterSelected(it) }
                )
                SelectionField(
                    label = "Persona",
                    emptyLabel = "Không dùng Persona",
                    selectedLabel = state.personas.firstOrNull { it.id == draft.personaId }?.name,
                    options = listOf("" to "Không dùng Persona") + state.personas.map { it.id to it.name },
                    testTag = "new_conversation_persona",
                    onSelected = { onPersonaSelected(it.ifEmpty { null }) }
                )
                Text(
                    "Persona chỉ được gắn vào cuộc hội thoại mới; lựa chọn của hội thoại cũ không thay đổi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.createError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("create_conversation_error"))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = !state.isCreating,
                modifier = Modifier.testTag("confirm_create_conversation")
            ) {
                if (state.isCreating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Tạo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isCreating) { Text("Hủy") } }
    )
}

@Composable
private fun SelectionField(
    label: String,
    emptyLabel: String,
    selectedLabel: String?,
    options: List<Pair<String, String>>,
    testTag: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selectedLabel ?: emptyLabel,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.testTag(testTag),
            label = { Text(label) }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                    modifier = Modifier.testTag("${testTag}_option_${id.ifEmpty { "none" }}")
                )
            }
        }
    }
}

@Composable
fun ConversationHistoryScreen(
    viewModel: RoleplayViewModel,
    conversationId: String,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    DisposableEffect(viewModel, conversationId) {
        viewModel.openConversation(conversationId)
        onDispose { viewModel.closeConversation(conversationId) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("conversation_history_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại danh sách")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Lịch sử hội thoại", style = MaterialTheme.typography.titleLarge)
                state.activeDetail?.header?.let { header ->
                    Text(
                        "${header.characterName} · ${header.personaName ?: "Không dùng Persona"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        state.activeDetail?.let { detail ->
            Text(
                detail.header.conversation.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("conversation_history_title")
            )
        }
        when {
            state.isDetailLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.detailError != null -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(state.detailError.orEmpty(), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { viewModel.openConversation(conversationId) }) { Text("Tải lại") }
            }
            state.activeDetail?.messages.isNullOrEmpty() -> StudioEmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Chưa có tin nhắn",
                description = "Lịch sử sẽ xuất hiện tại đây sau khi có tin nhắn được lưu."
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("conversation_history_messages"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.activeDetail?.messages.orEmpty(), key = { it.id }) { message ->
                    HistoryMessageCard(message)
                }
            }
        }
    }
}

@Composable
private fun HistoryMessageCard(message: ChatMessageEntity) {
    val alignment = if (message.role == ChatMessageEntity.ROLE_USER) Alignment.End else Alignment.Start
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).testTag("history_message_${message.id}"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.role == ChatMessageEntity.ROLE_USER) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message.role.asVietnameseRole(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(message.content, style = MaterialTheme.typography.bodyLarge)
                if (message.status != ChatMessageEntity.STATUS_COMPLETED) {
                    Text(
                        message.status.asVietnameseStatus(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.asVietnameseRole(): String = when (this) {
    ChatMessageEntity.ROLE_USER -> "Bạn"
    ChatMessageEntity.ROLE_ASSISTANT -> "Trợ lý"
    ChatMessageEntity.ROLE_SYSTEM -> "Hệ thống"
    else -> "Tin nhắn"
}

private fun String.asVietnameseStatus(): String = when (this) {
    ChatMessageEntity.STATUS_PENDING -> "Đang chờ"
    ChatMessageEntity.STATUS_STREAMING -> "Đang tạo dở"
    ChatMessageEntity.STATUS_INTERRUPTED -> "Đã gián đoạn"
    ChatMessageEntity.STATUS_FAILED -> "Gửi thất bại"
    else -> "Trạng thái: $this"
}

private fun formatActivityTime(timestamp: Long): String {
    val age = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    return when {
        age < 60_000L -> "Hoạt động vừa xong"
        age < 3_600_000L -> "Hoạt động ${age / 60_000L} phút trước"
        age < 86_400_000L -> "Hoạt động ${age / 3_600_000L} giờ trước"
        else -> "Hoạt động ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(timestamp))}"
    }
}

private fun formatMessageTime(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(timestamp))
