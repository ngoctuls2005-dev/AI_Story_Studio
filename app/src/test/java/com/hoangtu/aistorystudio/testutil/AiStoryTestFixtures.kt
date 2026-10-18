package com.hoangtu.aistorystudio.testutil

import com.hoangtu.aistorystudio.core.ai.AiMessage
import com.hoangtu.aistorystudio.core.ai.AiMessageRole
import com.hoangtu.aistorystudio.core.ai.AiRequest
import com.hoangtu.aistorystudio.core.ai.AiStreamEvent
import com.hoangtu.aistorystudio.core.ai.settings.AiGenerationParameters
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderConfiguration
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.entity.MemoryFactEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelChapterEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelProjectEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSceneEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity

object AiStoryTestFixtures {
    const val FIXED_TIMESTAMP = 1_750_000_000_000L
    const val CHARACTER_ID = "fixture-character-001"
    const val PERSONA_ID = "fixture-persona-001"
    const val CONVERSATION_ID = "fixture-conversation-001"
    const val NOVEL_PROJECT_ID = "fixture-novel-project-001"
    const val NOVEL_CHAPTER_ID = "fixture-novel-chapter-001"

    fun characterProfile(
        id: String = CHARACTER_ID,
        name: String = "Linh",
    ) = CharacterProfileEntity(
        id = id,
        name = name,
        description = "Nhân vật mẫu dùng trong kiểm thử.",
        personality = "Điềm tĩnh và quan sát kỹ.",
        relationship = "Người bạn đồng hành trong dữ liệu giả.",
        speakingStyle = "Nói ngắn gọn, thân thiện.",
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun userPersona(
        id: String = PERSONA_ID,
        name: String = "Người kể mẫu",
    ) = UserPersonaEntity(
        id = id,
        name = name,
        description = "Persona giả cho kiểm thử tự động.",
        firstPerson = "mình",
        secondPerson = "bạn",
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun conversation(
        id: String = CONVERSATION_ID,
        characterId: String = CHARACTER_ID,
        personaId: String? = PERSONA_ID,
    ) = ConversationEntity(
        id = id,
        title = "Cuộc trò chuyện kiểm thử",
        characterId = characterId,
        personaId = personaId,
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun chatMessage(
        id: String = "fixture-message-001",
        conversationId: String = CONVERSATION_ID,
        role: String = ChatMessageEntity.ROLE_USER,
        content: String = "Xin chào, đây là dữ liệu kiểm thử.",
        sequence: Long = 1L,
    ) = ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        status = ChatMessageEntity.STATUS_COMPLETED,
        sequence = sequence,
        createdAt = FIXED_TIMESTAMP
    )

    fun novelProject(
        id: String = NOVEL_PROJECT_ID,
        title: String = "Dự án truyện mẫu",
    ) = NovelProjectEntity(
        id = id,
        title = title,
        synopsis = "Tóm tắt giả dùng riêng cho kiểm thử.",
        status = NovelProjectEntity.STATUS_DRAFT,
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun novelChapter(
        id: String = NOVEL_CHAPTER_ID,
        projectId: String = NOVEL_PROJECT_ID,
        chapterOrder: Long = 1L,
    ) = NovelChapterEntity(
        id = id,
        projectId = projectId,
        title = "Chương mẫu",
        chapterOrder = chapterOrder,
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun novelScene(
        id: String = "fixture-novel-scene-001",
        chapterId: String = NOVEL_CHAPTER_ID,
        sceneOrder: Long = 1L,
    ) = NovelSceneEntity(
        id = id,
        chapterId = chapterId,
        title = "Cảnh mẫu",
        content = "Một đoạn văn giả để kiểm tra lưu trữ.",
        sceneOrder = sceneOrder,
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun characterMemoryFact(
        id: String = "fixture-memory-character-001",
        characterId: String = CHARACTER_ID,
    ) = MemoryFactEntity(
        id = id,
        ownerType = MemoryFactEntity.OWNER_CHARACTER,
        characterId = characterId,
        content = "Nhân vật thích quan sát bầu trời.",
        source = "fixture",
        status = MemoryFactEntity.STATUS_CANON,
        pinned = true,
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun conversationMemoryFact(
        id: String = "fixture-memory-conversation-001",
        conversationId: String = CONVERSATION_ID,
    ) = MemoryFactEntity(
        id = id,
        ownerType = MemoryFactEntity.OWNER_CONVERSATION,
        conversationId = conversationId,
        content = "Cuộc trò chuyện đang dùng dữ liệu giả.",
        source = "fixture",
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun novelMemoryFact(
        id: String = "fixture-memory-novel-001",
        projectId: String = NOVEL_PROJECT_ID,
    ) = MemoryFactEntity(
        id = id,
        ownerType = MemoryFactEntity.OWNER_NOVEL_PROJECT,
        projectId = projectId,
        content = "Quy tắc cốt truyện mẫu không dùng dữ liệu cá nhân.",
        source = "fixture",
        createdAt = FIXED_TIMESTAMP,
        updatedAt = FIXED_TIMESTAMP
    )

    fun aiRequest() = AiRequest(
        messages = listOf(
            AiMessage(AiMessageRole.SYSTEM, "Bạn là trợ lý sáng tác trong bộ test."),
            AiMessage(AiMessageRole.USER, "Viết một câu tiếng Việt ngắn.")
        )
    )

    fun aiResponseEvents(): List<AiStreamEvent> = listOf(
        AiStreamEvent.TextDelta("Bầu trời hôm nay trong xanh."),
        AiStreamEvent.Completed
    )

    fun providerConfiguration() = AiProviderConfiguration(
        providerId = "fixture-provider",
        displayName = "Provider mẫu",
        baseUrl = "https://mock.example.test/v1",
        modelId = "fixture-model",
        parameters = AiGenerationParameters(
            temperature = 0.4f,
            topP = 0.9f,
            maxOutputTokens = 128
        )
    )
}
