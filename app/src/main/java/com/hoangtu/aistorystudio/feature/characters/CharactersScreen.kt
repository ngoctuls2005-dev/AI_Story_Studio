package com.hoangtu.aistorystudio.feature.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity

@Composable
fun CharactersScreen(viewModel: CharacterManagementViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp)
                .testTag("screen_characters"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HỒ SƠ DÙNG CHUNG",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Nhân vật & Persona",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    modifier = Modifier.testTag("add_character_item"),
                    onClick = {
                        if (state.selectedTab == CharacterManagementTab.CHARACTERS) {
                            viewModel.addProfile()
                        } else {
                            viewModel.addPersona()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (state.selectedTab == CharacterManagementTab.CHARACTERS) "Tạo mới" else "Tạo Persona")
                }
            }
            Text(
                text = "Quản lý hồ sơ nhân vật AI và danh tính bạn muốn dùng trong câu chuyện.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                Tab(
                    modifier = Modifier.testTag("tab_characters"),
                    selected = state.selectedTab == CharacterManagementTab.CHARACTERS,
                    onClick = { viewModel.selectTab(CharacterManagementTab.CHARACTERS) },
                    text = { Text("Nhân vật") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) }
                )
                Tab(
                    modifier = Modifier.testTag("tab_personas"),
                    selected = state.selectedTab == CharacterManagementTab.PERSONAS,
                    onClick = { viewModel.selectTab(CharacterManagementTab.PERSONAS) },
                    text = { Text("Persona") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) }
                )
            }

            if (state.selectedTab == CharacterManagementTab.CHARACTERS) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_search"),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Tìm theo tên nhân vật") },
                    singleLine = true
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 36.dp)
                            .testTag("characters_loading")
                    )
                    state.loadError != null -> LoadErrorState(
                        message = state.loadError,
                        onRetry = viewModel::retryLoad
                    )
                    state.selectedTab == CharacterManagementTab.CHARACTERS -> ProfileList(
                        profiles = state.visibleProfiles,
                        hasSearch = state.searchQuery.isNotBlank(),
                        onOpen = viewModel::showProfile,
                        onEdit = viewModel::editProfile,
                        onDelete = { viewModel.requestDelete(CharacterDeleteTarget.Profile(it.id, it.name)) }
                    )
                    else -> PersonaList(
                        personas = state.personas,
                        onEdit = viewModel::editPersona,
                        onDelete = { viewModel.requestDelete(CharacterDeleteTarget.Persona(it.id, it.name)) },
                        onSelect = viewModel::setDefaultPersona
                    )
                }
            }
        }
    }

    state.profileEditor?.let { draft ->
        ProfileEditorDialog(
            draft = draft,
            validationError = state.validationError,
            isSaving = state.isSaving,
            onDraftChange = viewModel::updateProfileDraft,
            onDismiss = viewModel::closeProfileEditor,
            onSave = viewModel::saveProfile
        )
    }
    state.personaEditor?.let { draft ->
        PersonaEditorDialog(
            draft = draft,
            validationError = state.validationError,
            isSaving = state.isSaving,
            onDraftChange = viewModel::updatePersonaDraft,
            onDismiss = viewModel::closePersonaEditor,
            onSave = viewModel::savePersona
        )
    }
    state.selectedProfile?.let { profile ->
        ProfileDetailsDialog(profile = profile, onDismiss = viewModel::closeProfileDetails)
    }
    state.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(if (target is CharacterDeleteTarget.Profile) "Xóa nhân vật?" else "Xóa Persona?") },
            text = { Text("Bạn có chắc muốn xóa \"${target.name}\"? Dữ liệu đang được tham chiếu sẽ được giữ lại.") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("confirm_delete"),
                    onClick = viewModel::confirmDelete
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun ProfileList(
    profiles: List<CharacterProfileEntity>,
    hasSearch: Boolean,
    onOpen: (CharacterProfileEntity) -> Unit,
    onEdit: (CharacterProfileEntity) -> Unit,
    onDelete: (CharacterProfileEntity) -> Unit
) {
    if (profiles.isEmpty()) {
        EmptyManagementState(
            title = if (hasSearch) "Không tìm thấy nhân vật" else "Chưa có hồ sơ nhân vật",
            description = if (hasSearch) "Thử một tên khác để tìm trong danh sách." else "Tạo hồ sơ để giữ tính cách, mối quan hệ và quy tắc nhập vai nhất quán."
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("character_list"),
        contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(profiles, key = CharacterProfileEntity::id) { profile ->
            ProfileCard(profile, onOpen, onEdit, onDelete)
        }
    }
}

@Composable
private fun ProfileCard(
    profile: CharacterProfileEntity,
    onOpen: (CharacterProfileEntity) -> Unit,
    onEdit: (CharacterProfileEntity) -> Unit,
    onDelete: (CharacterProfileEntity) -> Unit
) {
    Card(
        onClick = { onOpen(profile) },
        modifier = Modifier.fillMaxWidth().testTag("profile_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = profile.age?.let { "$it tuổi" } ?: "Chưa xác định tuổi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    modifier = Modifier.testTag("edit_profile"),
                    onClick = { onEdit(profile) }
                ) { Icon(Icons.Filled.Edit, contentDescription = "Sửa ${profile.name}") }
                IconButton(
                    modifier = Modifier.testTag("delete_profile"),
                    onClick = { onDelete(profile) }
                ) { Icon(Icons.Filled.Delete, contentDescription = "Xóa ${profile.name}") }
            }
            if (profile.description.isNotBlank() || profile.personality.isNotBlank()) {
                Text(
                    text = listOf(profile.description, profile.personality).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun PersonaList(
    personas: List<UserPersonaEntity>,
    onEdit: (UserPersonaEntity) -> Unit,
    onDelete: (UserPersonaEntity) -> Unit,
    onSelect: (UserPersonaEntity) -> Unit
) {
    if (personas.isEmpty()) {
        EmptyManagementState(
            title = "Chưa có Persona",
            description = "Tạo Persona để chọn cách xưng hô và danh tính của bạn trong câu chuyện."
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("persona_list"),
        contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(personas, key = UserPersonaEntity::id) { persona ->
            PersonaCard(persona, onEdit, onDelete, onSelect)
        }
    }
}

@Composable
private fun PersonaCard(
    persona: UserPersonaEntity,
    onEdit: (UserPersonaEntity) -> Unit,
    onDelete: (UserPersonaEntity) -> Unit,
    onSelect: (UserPersonaEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("persona_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(persona.name, style = MaterialTheme.typography.titleLarge)
                    if (persona.isDefault) {
                        Text("Mặc định", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(
                    modifier = Modifier.testTag("edit_persona"),
                    onClick = { onEdit(persona) }
                ) { Icon(Icons.Filled.Edit, contentDescription = "Sửa ${persona.name}") }
                IconButton(
                    modifier = Modifier.testTag("delete_persona"),
                    onClick = { onDelete(persona) }
                ) { Icon(Icons.Filled.Delete, contentDescription = "Xóa ${persona.name}") }
            }
            Text(
                text = persona.description.ifBlank { "Chưa có mô tả" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            if (!persona.isDefault) {
                OutlinedButton(
                    modifier = Modifier.testTag("select_default_persona"),
                    onClick = { onSelect(persona) }
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Chọn làm mặc định")
                }
            }
        }
    }
}

@Composable
private fun EmptyManagementState(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadErrorState(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message.orEmpty(), color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Thử tải lại") }
    }
}

@Composable
private fun ProfileEditorDialog(
    draft: CharacterProfileDraft,
    validationError: String?,
    isSaving: Boolean,
    onDraftChange: ((CharacterProfileDraft) -> CharacterProfileDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    EditorDialogFrame(
        title = if (draft.id == null) "Tạo nhân vật" else "Chỉnh sửa nhân vật",
        validationError = validationError,
        isSaving = isSaving,
        onDismiss = onDismiss,
        onSave = onSave,
        saveTag = "save_profile"
    ) {
        EditorField("Tên nhân vật *", draft.name, "profile_name", { onDraftChange { old -> old.copy(name = it) } })
        OutlinedTextField(
            value = draft.age,
            onValueChange = { value ->
                if (value.length <= 3 && value.all(Char::isDigit)) onDraftChange { old -> old.copy(age = value) }
            },
            modifier = Modifier.fillMaxWidth().testTag("profile_age"),
            label = { Text("Tuổi (từ 18 đến 120) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        EditorField("Mô tả", draft.description, "profile_description", { onDraftChange { old -> old.copy(description = it) } }, true)
        EditorField("Ngoại hình", draft.appearance, "profile_appearance", { onDraftChange { old -> old.copy(appearance = it) } }, true)
        EditorField("Bối cảnh", draft.background, "profile_background", { onDraftChange { old -> old.copy(background = it) } }, true)
        EditorField("Tính cách", draft.personality, "profile_personality", { onDraftChange { old -> old.copy(personality = it) } }, true)
        EditorField("Mối quan hệ", draft.relationship, "profile_relationship", { onDraftChange { old -> old.copy(relationship = it) } }, true)
        EditorField("Cách xưng hô và giọng nói", draft.speakingStyle, "profile_speaking_style", { onDraftChange { old -> old.copy(speakingStyle = it) } }, true)
        EditorField("Quy tắc nhập vai", draft.roleplayRules, "profile_roleplay_rules", { onDraftChange { old -> old.copy(roleplayRules = it) } }, true)
    }
}

@Composable
private fun PersonaEditorDialog(
    draft: UserPersonaDraft,
    validationError: String?,
    isSaving: Boolean,
    onDraftChange: ((UserPersonaDraft) -> UserPersonaDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    EditorDialogFrame(
        title = if (draft.id == null) "Tạo Persona" else "Chỉnh sửa Persona",
        validationError = validationError,
        isSaving = isSaving,
        onDismiss = onDismiss,
        onSave = onSave,
        saveTag = "save_persona"
    ) {
        EditorField("Tên Persona *", draft.name, "persona_name", { onDraftChange { old -> old.copy(name = it) } })
        EditorField("Mô tả", draft.description, "persona_description", { onDraftChange { old -> old.copy(description = it) } }, true)
        EditorField("Đại từ ngôi thứ nhất", draft.firstPerson, "persona_first_person", { onDraftChange { old -> old.copy(firstPerson = it) } })
        EditorField("Cách gọi người đối thoại", draft.secondPerson, "persona_second_person", { onDraftChange { old -> old.copy(secondPerson = it) } })
    }
}

@Composable
private fun EditorDialogFrame(
    title: String,
    validationError: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveTag: String,
    fields: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp)
                .imePadding(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Thông tin được lưu trên thiết bị.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = fields
                )
                if (validationError != null) {
                    Text(
                        text = validationError,
                        modifier = Modifier.testTag("editor_validation_error"),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") }
                    Button(
                        modifier = Modifier.testTag(saveTag),
                        onClick = onSave,
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Lưu")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    tag: String,
    onValueChange: (String) -> Unit,
    multiline: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) },
        minLines = if (multiline) 2 else 1,
        maxLines = if (multiline) 4 else 1
    )
}

@Composable
private fun ProfileDetailsDialog(profile: CharacterProfileEntity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text(profile.name, style = MaterialTheme.typography.headlineSmall)
                Text(profile.age?.let { "$it tuổi" } ?: "Chưa xác định tuổi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileDetail("Mô tả", profile.description)
                    ProfileDetail("Ngoại hình", profile.appearance)
                    ProfileDetail("Bối cảnh", profile.background)
                    ProfileDetail("Tính cách", profile.personality)
                    ProfileDetail("Mối quan hệ", profile.relationship)
                    ProfileDetail("Cách xưng hô và giọng nói", profile.speakingStyle)
                    ProfileDetail("Quy tắc nhập vai", profile.roleplayRules)
                }
                TextButton(modifier = Modifier.align(Alignment.End), onClick = onDismiss) { Text("Đóng") }
            }
        }
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value.ifBlank { "Chưa có thông tin" }, style = MaterialTheme.typography.bodyMedium)
    }
}
