package com.hoangtu.aistorystudio.feature.roleplay

import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.model.ConversationHeader
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.repository.CharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.ConversationRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ConversationManagementUseCases(
    private val conversationRepository: ConversationRepository,
    private val characterRepository: CharacterRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    fun observeConversations(query: String): Flow<List<ConversationHeader>> =
        conversationRepository.observeConversationHeaders(query)

    fun observeHeader(id: String): Flow<ConversationHeader?> =
        conversationRepository.observeConversationHeader(id)

    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>> =
        conversationRepository.observeMessages(conversationId)

    fun observeCharacters() = characterRepository.observeProfiles()

    fun observePersonas() = characterRepository.observePersonas()

    suspend fun createConversation(
        title: String,
        characterId: String?,
        personaId: String?
    ): ConversationEntity {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Vui lòng nhập tiêu đề cuộc hội thoại." }
        require(!characterId.isNullOrBlank()) { "Vui lòng chọn một nhân vật." }
        require(characterRepository.getProfile(characterId) != null) {
            "Nhân vật đã chọn không còn tồn tại. Hãy tải lại danh sách."
        }
        if (personaId != null) {
            require(characterRepository.getPersona(personaId) != null) {
                "Persona đã chọn không còn tồn tại. Hãy tải lại danh sách."
            }
        }

        val now = clock()
        val conversation = ConversationEntity(
            id = newId(),
            title = normalizedTitle,
            characterId = characterId,
            personaId = personaId,
            createdAt = now,
            updatedAt = now
        )
        conversationRepository.createConversation(conversation)
        return conversation
    }

    fun observeDetail(id: String): Flow<ConversationDetail?> = combine(
        observeHeader(id),
        observeMessages(id)
    ) { header, messages ->
        header?.let { ConversationDetail(it, messages) }
    }

    suspend fun deleteConversation(id: String): Boolean =
        conversationRepository.deleteConversation(id)
}

data class ConversationDetail(
    val header: ConversationHeader,
    val messages: List<ChatMessageEntity>
)
