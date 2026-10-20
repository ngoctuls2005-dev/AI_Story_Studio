package com.hoangtu.aistorystudio.feature.roleplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.database.sqlite.SQLiteConstraintException
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.model.ConversationHeader
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.repository.CharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.ConversationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewConversationDraft(
    val title: String,
    val characterId: String?,
    val personaId: String?
)

data class RoleplayUiState(
    val searchQuery: String = "",
    val conversations: List<ConversationHeader> = emptyList(),
    val characters: List<CharacterProfileEntity> = emptyList(),
    val personas: List<UserPersonaEntity> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val createDraft: NewConversationDraft? = null,
    val createError: String? = null,
    val isCreating: Boolean = false,
    val pendingDelete: ConversationHeader? = null,
    val message: String? = null,
    val isMessageError: Boolean = false,
    val activeConversationId: String? = null,
    val activeDetail: ConversationDetail? = null,
    val isDetailLoading: Boolean = false,
    val detailError: String? = null
)

sealed interface RoleplayEvent {
    data class OpenConversation(val id: String) : RoleplayEvent
}

class RoleplayViewModel(
    private val useCases: ConversationManagementUseCases
) : ViewModel() {
    private val mutableState = MutableStateFlow(RoleplayUiState())
    val state: StateFlow<RoleplayUiState> = mutableState.asStateFlow()
    private val eventChannel = Channel<RoleplayEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var conversationJob: Job? = null
    private var detailJob: Job? = null
    private var charactersJob: Job? = null
    private var personasJob: Job? = null

    init {
        observeConversations()
        observeCharacters()
        observePersonas()
    }

    fun updateSearchQuery(query: String) {
        mutableState.update { it.copy(searchQuery = query, loadError = null) }
    }

    fun retryLoading() {
        observeConversations()
        observeCharacters()
        observePersonas()
    }

    fun beginCreateConversation() {
        val current = mutableState.value
        val character = current.characters.firstOrNull()
        val persona = current.personas.firstOrNull { it.isDefault }
        mutableState.update {
            it.copy(
                createDraft = NewConversationDraft(
                    title = character?.let { profile -> "Trò chuyện với ${profile.name}" }.orEmpty(),
                    characterId = character?.id,
                    personaId = persona?.id
                ),
                createError = null,
                message = null
            )
        }
    }

    fun updateCreateTitle(title: String) {
        mutableState.update { current ->
            current.copy(createDraft = current.createDraft?.copy(title = title), createError = null)
        }
    }

    fun selectCreateCharacter(characterId: String) {
        mutableState.update { current ->
            val draft = current.createDraft ?: return@update current
            val oldCharacter = current.characters.firstOrNull { it.id == draft.characterId }
            val newCharacter = current.characters.firstOrNull { it.id == characterId }
            val oldAutomaticTitle = oldCharacter?.let { "Trò chuyện với ${it.name}" }
            val updatedTitle = if (draft.title.isBlank() || draft.title == oldAutomaticTitle) {
                newCharacter?.let { "Trò chuyện với ${it.name}" }.orEmpty()
            } else {
                draft.title
            }
            current.copy(
                createDraft = draft.copy(title = updatedTitle, characterId = characterId),
                createError = null
            )
        }
    }

    fun selectCreatePersona(personaId: String?) {
        mutableState.update { current ->
            current.copy(
                createDraft = current.createDraft?.copy(personaId = personaId),
                createError = null
            )
        }
    }

    fun cancelCreateConversation() {
        mutableState.update { it.copy(createDraft = null, createError = null) }
    }

    fun createConversation() {
        val draft = mutableState.value.createDraft ?: return
        mutableState.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            try {
                val conversation = useCases.createConversation(
                    title = draft.title,
                    characterId = draft.characterId,
                    personaId = draft.personaId
                )
                mutableState.update {
                    it.copy(createDraft = null, isCreating = false, createError = null)
                }
                eventChannel.send(RoleplayEvent.OpenConversation(conversation.id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IllegalArgumentException) {
                mutableState.update {
                    it.copy(isCreating = false, createError = failure.message ?: CREATE_ERROR)
                }
            } catch (_: SQLiteConstraintException) {
                mutableState.update {
                    it.copy(isCreating = false, createError = "Không thể liên kết nhân vật hoặc Persona đã chọn.")
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(isCreating = false, createError = CREATE_ERROR) }
            }
        }
    }

    fun requestDelete(header: ConversationHeader) {
        mutableState.update { it.copy(pendingDelete = header, message = null) }
    }

    fun cancelDelete() {
        mutableState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val pending = mutableState.value.pendingDelete ?: return
        viewModelScope.launch {
            try {
                val deleted = useCases.deleteConversation(pending.conversation.id)
                mutableState.update {
                    it.copy(
                        pendingDelete = null,
                        message = if (deleted) "Đã xóa cuộc hội thoại và lịch sử tin nhắn." else "Cuộc hội thoại không còn tồn tại.",
                        isMessageError = !deleted
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        pendingDelete = null,
                        message = "Không thể xóa cuộc hội thoại lúc này. Dữ liệu vẫn được giữ lại.",
                        isMessageError = true
                    )
                }
            }
        }
    }

    fun openConversation(id: String) {
        if (mutableState.value.activeConversationId == id && detailJob?.isActive == true) return
        detailJob?.cancel()
        mutableState.update {
            it.copy(
                activeConversationId = id,
                activeDetail = null,
                isDetailLoading = true,
                detailError = null
            )
        }
        detailJob = viewModelScope.launch {
            useCases.observeDetail(id)
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    mutableState.update {
                        it.copy(isDetailLoading = false, detailError = DETAIL_ERROR)
                    }
                }
                .collect { detail ->
                    mutableState.update {
                        if (detail == null) {
                            it.copy(isDetailLoading = false, activeDetail = null, detailError = "Không tìm thấy cuộc hội thoại này.")
                        } else {
                            it.copy(isDetailLoading = false, activeDetail = detail, detailError = null)
                        }
                    }
                }
        }
    }

    fun closeConversation(id: String) {
        if (mutableState.value.activeConversationId != id) return
        detailJob?.cancel()
        detailJob = null
        mutableState.update {
            it.copy(
                activeConversationId = null,
                activeDetail = null,
                isDetailLoading = false,
                detailError = null
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeConversations() {
        conversationJob?.cancel()
        conversationJob = viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, loadError = null) }
            mutableState
                .map { it.searchQuery }
                .distinctUntilChanged()
                .flatMapLatest(useCases::observeConversations)
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    mutableState.update { it.copy(isLoading = false, loadError = LOAD_ERROR) }
                }
                .collect { conversations ->
                    mutableState.update {
                        it.copy(conversations = conversations, isLoading = false, loadError = null)
                    }
                }
        }
    }

    private fun observeCharacters() {
        charactersJob?.cancel()
        charactersJob = viewModelScope.launch {
            useCases.observeCharacters()
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    mutableState.update { it.copy(loadError = LOAD_ERROR) }
                }
                .collect { characters -> mutableState.update { it.copy(characters = characters) } }
        }
    }

    private fun observePersonas() {
        personasJob?.cancel()
        personasJob = viewModelScope.launch {
            useCases.observePersonas()
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    mutableState.update { it.copy(loadError = LOAD_ERROR) }
                }
                .collect { personas -> mutableState.update { it.copy(personas = personas) } }
        }
    }

    class Factory(
        private val conversationRepository: ConversationRepository,
        private val characterRepository: CharacterRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(RoleplayViewModel::class.java)) {
                throw IllegalArgumentException("Không hỗ trợ ViewModel này.")
            }
            return RoleplayViewModel(
                ConversationManagementUseCases(conversationRepository, characterRepository)
            ) as T
        }
    }

    private companion object {
        const val LOAD_ERROR = "Không thể tải dữ liệu hội thoại. Vui lòng thử lại."
        const val CREATE_ERROR = "Không thể tạo cuộc hội thoại. Dữ liệu hiện có vẫn được giữ nguyên."
        const val DETAIL_ERROR = "Không thể đọc lịch sử lúc này. Dữ liệu hiện có vẫn được giữ nguyên."
    }
}
