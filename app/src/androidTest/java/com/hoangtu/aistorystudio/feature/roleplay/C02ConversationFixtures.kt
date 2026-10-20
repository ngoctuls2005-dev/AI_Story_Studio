package com.hoangtu.aistorystudio.feature.roleplay

import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomConversationRepository

internal object C02ConversationFixtures {
    const val CHARACTER_LINH = "c02-character-linh"
    const val CHARACTER_MINH = "c02-character-minh"
    const val PERSONA_FIRST = "c02-persona-first"
    const val PERSONA_SECOND = "c02-persona-second"
    const val CONVERSATION_GARDEN = "c02-conversation-garden"
    const val CONVERSATION_CITY = "c02-conversation-city"
    const val CONVERSATION_EVENING = "c02-conversation-evening"

    suspend fun seed(
        characterRepository: RoomCharacterRepository,
        conversationRepository: RoomConversationRepository
    ) {
        characterRepository.saveProfile(
            CharacterProfileEntity(
                id = CHARACTER_LINH,
                name = "Linh Nguyễn",
                description = "Nhân vật fixture.",
                createdAt = 10L,
                updatedAt = 30L
            )
        )
        characterRepository.saveProfile(
            CharacterProfileEntity(
                id = CHARACTER_MINH,
                name = "Minh Trần",
                description = "Nhân vật fixture thứ hai.",
                createdAt = 11L,
                updatedAt = 20L
            )
        )
        characterRepository.savePersona(
            UserPersonaEntity(
                id = PERSONA_FIRST,
                name = "Người kể một",
                firstPerson = "mình",
                secondPerson = "bạn",
                isDefault = true,
                createdAt = 12L,
                updatedAt = 12L
            )
        )
        characterRepository.savePersona(
            UserPersonaEntity(
                id = PERSONA_SECOND,
                name = "Người kể hai",
                firstPerson = "tôi",
                secondPerson = "cậu",
                createdAt = 13L,
                updatedAt = 13L
            )
        )

        val conversations = listOf(
            ConversationEntity(
                id = CONVERSATION_GARDEN,
                title = "Hội thoại khu vườn",
                characterId = CHARACTER_LINH,
                personaId = PERSONA_FIRST,
                createdAt = 100L,
                updatedAt = 200L
            ),
            ConversationEntity(
                id = CONVERSATION_CITY,
                title = "Hội thoại thành phố",
                characterId = CHARACTER_MINH,
                personaId = PERSONA_SECOND,
                createdAt = 100L,
                updatedAt = 300L
            ),
            ConversationEntity(
                id = CONVERSATION_EVENING,
                title = "Buổi chiều yên tĩnh",
                characterId = CHARACTER_LINH,
                personaId = PERSONA_SECOND,
                createdAt = 100L,
                updatedAt = 300L
            )
        )
        conversations.forEach { conversation ->
            conversationRepository.createConversation(conversation)
            conversationRepository.appendMessage(
                message(
                    id = "${conversation.id}-user",
                    conversationId = conversation.id,
                    role = ChatMessageEntity.ROLE_USER,
                    content = "Xin chào từ ${conversation.title}.",
                    createdAt = 500L
                )
            )
            conversationRepository.appendMessage(
                message(
                    id = "${conversation.id}-assistant",
                    conversationId = conversation.id,
                    role = ChatMessageEntity.ROLE_ASSISTANT,
                    content = "Lịch sử riêng của ${conversation.title}: mình nhớ khu vườn có hoa sen.",
                    createdAt = 500L
                )
            )
        }
    }

    fun message(
        id: String,
        conversationId: String,
        role: String,
        content: String,
        createdAt: Long
    ) = ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        status = ChatMessageEntity.STATUS_COMPLETED,
        sequence = 99L,
        createdAt = createdAt
    )
}
