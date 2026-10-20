package com.hoangtu.aistorystudio.feature.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.repository.CharacterDeleteResult
import com.hoangtu.aistorystudio.core.database.repository.CharacterRepository
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CharacterManagementTab {
    CHARACTERS,
    PERSONAS
}

data class CharacterProfileDraft(
    val id: String? = null,
    val name: String = "",
    val age: String = "",
    val description: String = "",
    val appearance: String = "",
    val background: String = "",
    val personality: String = "",
    val relationship: String = "",
    val speakingStyle: String = "",
    val roleplayRules: String = ""
)

data class UserPersonaDraft(
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val firstPerson: String = "",
    val secondPerson: String = ""
)

sealed interface CharacterDeleteTarget {
    val id: String
    val name: String

    data class Profile(override val id: String, override val name: String) : CharacterDeleteTarget
    data class Persona(override val id: String, override val name: String) : CharacterDeleteTarget
}

data class CharacterManagementUiState(
    val selectedTab: CharacterManagementTab = CharacterManagementTab.CHARACTERS,
    val searchQuery: String = "",
    val profiles: List<CharacterProfileEntity> = emptyList(),
    val personas: List<UserPersonaEntity> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val profileEditor: CharacterProfileDraft? = null,
    val personaEditor: UserPersonaDraft? = null,
    val selectedProfile: CharacterProfileEntity? = null,
    val pendingDelete: CharacterDeleteTarget? = null,
    val validationError: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val isMessageError: Boolean = false
) {
    val visibleProfiles: List<CharacterProfileEntity>
        get() {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            return if (query.isEmpty()) profiles else profiles.filter {
                it.name.lowercase(Locale.ROOT).contains(query)
            }
        }
}

class CharacterManagementViewModel(
    private val useCases: CharacterManagementUseCases,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val mutableState = MutableStateFlow(CharacterManagementUiState())
    val state: StateFlow<CharacterManagementUiState> = mutableState.asStateFlow()

    private var profilesLoaded = false
    private var personasLoaded = false
    private var profilesJob: Job? = null
    private var personasJob: Job? = null

    init {
        observeData()
    }

    fun selectTab(tab: CharacterManagementTab) {
        mutableState.update { it.copy(selectedTab = tab, message = null) }
    }

    fun updateSearchQuery(query: String) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun addProfile() {
        mutableState.update {
            it.copy(profileEditor = CharacterProfileDraft(), validationError = null, message = null)
        }
    }

    fun editProfile(profile: CharacterProfileEntity) {
        mutableState.update {
            it.copy(
                profileEditor = CharacterProfileDraft(
                    id = profile.id,
                    name = profile.name,
                    age = profile.age?.toString().orEmpty(),
                    description = profile.description,
                    appearance = profile.appearance,
                    background = profile.background,
                    personality = profile.personality,
                    relationship = profile.relationship,
                    speakingStyle = profile.speakingStyle,
                    roleplayRules = profile.roleplayRules
                ),
                validationError = null,
                message = null
            )
        }
    }

    fun updateProfileDraft(transform: (CharacterProfileDraft) -> CharacterProfileDraft) {
        mutableState.update {
            it.copy(profileEditor = it.profileEditor?.let(transform), validationError = null)
        }
    }

    fun closeProfileEditor() {
        mutableState.update { it.copy(profileEditor = null, validationError = null) }
    }

    fun saveProfile() {
        val draft = mutableState.value.profileEditor ?: return
        val validationError = CharacterProfileValidator.validate(CharacterProfileInput(draft.name, draft.age))
        if (validationError != null) {
            mutableState.update { it.copy(validationError = validationError) }
            return
        }
        val age = draft.age.toIntOrNull() ?: return
        mutableState.update { it.copy(isSaving = true, validationError = null, message = null) }
        viewModelScope.launch {
            try {
                val existing = draft.id?.let { useCases.getProfile(it) }
                if (draft.id != null && existing == null) {
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            validationError = "Hồ sơ này không còn tồn tại. Hãy đóng và mở lại danh sách."
                        )
                    }
                    return@launch
                }
                val timestamp = clock()
                useCases.saveProfile(
                    CharacterProfileEntity(
                        id = existing?.id ?: draft.id ?: UUID.randomUUID().toString(),
                        name = draft.name.trim(),
                        description = draft.description.trim(),
                        age = age,
                        appearance = draft.appearance.trim(),
                        background = draft.background.trim(),
                        personality = draft.personality.trim(),
                        relationship = draft.relationship.trim(),
                        speakingStyle = draft.speakingStyle.trim(),
                        roleplayRules = draft.roleplayRules.trim(),
                        createdAt = existing?.createdAt ?: timestamp,
                        updatedAt = timestamp
                    )
                )
                mutableState.update {
                    it.copy(
                        profileEditor = null,
                        isSaving = false,
                        message = if (existing == null) "Đã tạo nhân vật." else "Đã cập nhật nhân vật.",
                        isMessageError = false
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(isSaving = false, message = STORAGE_ERROR, isMessageError = true)
                }
            }
        }
    }

    fun showProfile(profile: CharacterProfileEntity) {
        mutableState.update { it.copy(selectedProfile = profile) }
    }

    fun closeProfileDetails() {
        mutableState.update { it.copy(selectedProfile = null) }
    }

    fun addPersona() {
        mutableState.update {
            it.copy(personaEditor = UserPersonaDraft(), validationError = null, message = null)
        }
    }

    fun editPersona(persona: UserPersonaEntity) {
        mutableState.update {
            it.copy(
                personaEditor = UserPersonaDraft(
                    id = persona.id,
                    name = persona.name,
                    description = persona.description,
                    firstPerson = persona.firstPerson,
                    secondPerson = persona.secondPerson
                ),
                validationError = null,
                message = null
            )
        }
    }

    fun updatePersonaDraft(transform: (UserPersonaDraft) -> UserPersonaDraft) {
        mutableState.update {
            it.copy(personaEditor = it.personaEditor?.let(transform), validationError = null)
        }
    }

    fun closePersonaEditor() {
        mutableState.update { it.copy(personaEditor = null, validationError = null) }
    }

    fun savePersona() {
        val draft = mutableState.value.personaEditor ?: return
        val validationError = UserPersonaValidator.validate(draft.name)
        if (validationError != null) {
            mutableState.update { it.copy(validationError = validationError) }
            return
        }
        mutableState.update { it.copy(isSaving = true, validationError = null, message = null) }
        viewModelScope.launch {
            try {
                val existing = draft.id?.let { useCases.getPersona(it) }
                if (draft.id != null && existing == null) {
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            validationError = "Persona này không còn tồn tại. Hãy đóng và mở lại danh sách."
                        )
                    }
                    return@launch
                }
                val timestamp = clock()
                useCases.savePersona(
                    UserPersonaEntity(
                        id = existing?.id ?: draft.id ?: UUID.randomUUID().toString(),
                        name = draft.name.trim(),
                        description = draft.description.trim(),
                        firstPerson = draft.firstPerson.trim(),
                        secondPerson = draft.secondPerson.trim(),
                        isDefault = existing?.isDefault ?: false,
                        createdAt = existing?.createdAt ?: timestamp,
                        updatedAt = timestamp
                    )
                )
                mutableState.update {
                    it.copy(
                        personaEditor = null,
                        isSaving = false,
                        message = if (existing == null) "Đã tạo Persona." else "Đã cập nhật Persona.",
                        isMessageError = false
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(isSaving = false, message = STORAGE_ERROR, isMessageError = true)
                }
            }
        }
    }

    fun setDefaultPersona(persona: UserPersonaEntity) {
        viewModelScope.launch {
            try {
                val selected = useCases.setDefaultPersona(persona.id)
                mutableState.update {
                    it.copy(
                        message = if (selected) "Đã chọn Persona mặc định." else "Persona này không còn tồn tại.",
                        isMessageError = !selected
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(message = STORAGE_ERROR, isMessageError = true) }
            }
        }
    }

    fun requestDelete(target: CharacterDeleteTarget) {
        mutableState.update { it.copy(pendingDelete = target, message = null) }
    }

    fun dismissDelete() {
        mutableState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val target = mutableState.value.pendingDelete ?: return
        mutableState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            try {
                val result = when (target) {
                    is CharacterDeleteTarget.Profile -> useCases.deleteProfile(target.id)
                    is CharacterDeleteTarget.Persona -> useCases.deletePersona(target.id)
                }
                val message = when (result) {
                    CharacterDeleteResult.DELETED -> if (target is CharacterDeleteTarget.Profile) {
                        "Đã xóa nhân vật."
                    } else {
                        "Đã xóa Persona."
                    }
                    CharacterDeleteResult.NOT_FOUND -> "Mục này không còn tồn tại."
                    CharacterDeleteResult.REFERENCED -> if (target is CharacterDeleteTarget.Profile) {
                        "Không thể xóa vì nhân vật đang được hội thoại hoặc dự án truyện sử dụng."
                    } else {
                        "Không thể xóa vì Persona đang được hội thoại sử dụng."
                    }
                }
                mutableState.update {
                    it.copy(
                        selectedProfile = if (target is CharacterDeleteTarget.Profile && it.selectedProfile?.id == target.id) {
                            null
                        } else {
                            it.selectedProfile
                        },
                        message = message,
                        isMessageError = result != CharacterDeleteResult.DELETED
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(message = STORAGE_ERROR, isMessageError = true) }
            }
        }
    }

    fun retryLoad() {
        observeData()
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null, isMessageError = false) }
    }

    private fun observeData() {
        profilesJob?.cancel()
        personasJob?.cancel()
        profilesLoaded = false
        personasLoaded = false
        mutableState.update { it.copy(isLoading = true, loadError = null) }
        profilesJob = viewModelScope.launch {
            useCases.observeProfiles()
                .catch { handleLoadError(it) }
                .collect { profiles ->
                    profilesLoaded = true
                    mutableState.update {
                        it.copy(profiles = profiles, isLoading = !(profilesLoaded && personasLoaded))
                    }
                }
        }
        personasJob = viewModelScope.launch {
            useCases.observePersonas()
                .catch { handleLoadError(it) }
                .collect { personas ->
                    personasLoaded = true
                    mutableState.update {
                        it.copy(personas = personas, isLoading = !(profilesLoaded && personasLoaded))
                    }
                }
        }
    }

    private fun handleLoadError(error: Throwable) {
        if (error is CancellationException) throw error
        mutableState.update {
            it.copy(isLoading = false, loadError = STORAGE_ERROR)
        }
    }

    class Factory(
        private val repository: CharacterRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(CharacterManagementViewModel::class.java)) {
                throw IllegalArgumentException("Không hỗ trợ ViewModel này.")
            }
            return CharacterManagementViewModel(CharacterManagementUseCases(repository)) as T
        }
    }

    private companion object {
        const val STORAGE_ERROR = "Không thể truy cập dữ liệu lúc này. Vui lòng thử lại."
    }
}
