package com.hoangtu.aistorystudio.feature.roleplay

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomConversationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class C02ConversationRepositoryInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun threeConversationFixtureKeepsHistoryScopedAndOrdered() = runBlocking {
        withFixture { database, conversationRepository ->
            val garden = conversationRepository.getMessages(C02ConversationFixtures.CONVERSATION_GARDEN)
            val city = conversationRepository.getMessages(C02ConversationFixtures.CONVERSATION_CITY)
            val evening = conversationRepository.getMessages(C02ConversationFixtures.CONVERSATION_EVENING)

            assertEquals(listOf(1L, 2L), garden.map { it.sequence })
            assertEquals(ChatMessageEntity.ROLE_USER, garden.first().role)
            assertEquals(ChatMessageEntity.STATUS_COMPLETED, garden.first().status)
            assertEquals(listOf(500L, 500L), garden.map { it.createdAt })
            assertEquals(
                listOf(
                    "Xin chào từ Hội thoại khu vườn.",
                    "Lịch sử riêng của Hội thoại khu vườn: mình nhớ khu vườn có hoa sen."
                ),
                garden.map { it.content }
            )
            assertEquals(listOf(1L, 2L), city.map { it.sequence })
            assertEquals(listOf(1L, 2L), evening.map { it.sequence })
            assertTrue(garden.none { it.id in city.map(ChatMessageEntity::id) || it.id in evening.map(ChatMessageEntity::id) })
            assertTrue(city.none { it.id in evening.map(ChatMessageEntity::id) })
            assertEquals(3, conversationRepository.observeConversationHeaders("").first().size)
            assertEquals(
                listOf(C02ConversationFixtures.CONVERSATION_CITY, C02ConversationFixtures.CONVERSATION_EVENING),
                conversationRepository.observeConversationHeaders("Người kể hai").first().map { it.conversation.id }
            )
            assertEquals(
                listOf(C02ConversationFixtures.CONVERSATION_CITY),
                conversationRepository.observeConversationHeaders("Minh Trần").first().map { it.conversation.id }
            )
            assertEquals(
                listOf(C02ConversationFixtures.CONVERSATION_GARDEN),
                conversationRepository.observeConversationHeaders("khu vườn").first().map { it.conversation.id }
            )

            val orderedIds = conversationRepository.observeConversationHeaders("").first().map { it.conversation.id }
            assertEquals(
                listOf(C02ConversationFixtures.CONVERSATION_CITY, C02ConversationFixtures.CONVERSATION_EVENING, C02ConversationFixtures.CONVERSATION_GARDEN),
                orderedIds
            )
            assertEquals(3, database.conversationDao().observeConversations().first().size)
        }
    }

    @Test
    fun newConversationAndActivityDoNotModifyExistingHistoryOrPersona() = runBlocking {
        withFixture { _, repository ->
            val oldConversation = repository.getConversation(C02ConversationFixtures.CONVERSATION_GARDEN)!!
            val oldMessages = repository.getMessages(oldConversation.id)
            repository.createConversation(
                ConversationEntity(
                    id = "c02-created-after-fixture",
                    title = "Cuộc trò chuyện mới",
                    characterId = C02ConversationFixtures.CHARACTER_MINH,
                    personaId = C02ConversationFixtures.PERSONA_FIRST,
                    createdAt = 700L,
                    updatedAt = 700L
                )
            )
            assertEquals(oldConversation, repository.getConversation(oldConversation.id))
            assertEquals(oldMessages, repository.getMessages(oldConversation.id))
            assertEquals(C02ConversationFixtures.PERSONA_FIRST, repository.getConversation(oldConversation.id)?.personaId)
            assertTrue(repository.getMessages("c02-created-after-fixture").isEmpty())

            repository.appendMessage(
                C02ConversationFixtures.message(
                    id = "c02-created-after-fixture-user",
                    conversationId = "c02-created-after-fixture",
                    role = ChatMessageEntity.ROLE_USER,
                    content = "Dấu tiếng Việt: ấ, ệ, ơ, ư.",
                    createdAt = 900L
                )
            )
            assertEquals(900L, repository.getConversation("c02-created-after-fixture")?.updatedAt)
            assertEquals("Dấu tiếng Việt: ấ, ệ, ơ, ư.", repository.getMessages("c02-created-after-fixture").single().content)
            assertEquals(oldMessages, repository.getMessages(oldConversation.id))
        }
    }

    @Test
    fun conversationOwnerAndPersonaCannotBeChangedByGenericSave() = runBlocking {
        withFixture { _, repository ->
            val original = repository.getConversation(C02ConversationFixtures.CONVERSATION_GARDEN)!!
            val personaFailure = runCatching {
                repository.saveConversation(original.copy(personaId = C02ConversationFixtures.PERSONA_SECOND))
            }.exceptionOrNull()
            assertTrue(personaFailure is IllegalArgumentException)

            val characterFailure = runCatching {
                repository.saveConversation(original.copy(characterId = C02ConversationFixtures.CHARACTER_MINH))
            }.exceptionOrNull()
            assertTrue(characterFailure is IllegalArgumentException)
            assertEquals(original, repository.getConversation(original.id))
        }
    }

    @Test
    fun foreignKeysRejectInvalidOwnerAndFailedCreateLeavesNoPartialRow() = runBlocking {
        withFixture { _, repository ->
            val invalid = ConversationEntity(
                id = "c02-invalid-owner",
                title = "Không được lưu",
                characterId = "c02-missing-character",
                personaId = C02ConversationFixtures.PERSONA_FIRST,
                createdAt = 1L,
                updatedAt = 1L
            )
            val failure = runCatching { repository.createConversation(invalid) }.exceptionOrNull()
            assertTrue(failure is SQLiteConstraintException)
            assertNull(repository.getConversation(invalid.id))
            assertEquals(3, repository.observeConversationHeaders("").first().size)

            val duplicateId = repository.getConversation(C02ConversationFixtures.CONVERSATION_GARDEN)!!
            val duplicateFailure = runCatching {
                repository.createConversation(duplicateId.copy(title = "Không được ghi đè"))
            }.exceptionOrNull()
            assertNotNull(duplicateFailure)
            assertEquals("Hội thoại khu vườn", repository.getConversation(duplicateId.id)?.title)
        }
    }

    @Test
    fun confirmedRepositoryDeleteCascadesOnlySelectedHistory() = runBlocking {
        withFixture { _, repository ->
            assertTrue(repository.deleteConversation(C02ConversationFixtures.CONVERSATION_CITY))
            assertNull(repository.getConversation(C02ConversationFixtures.CONVERSATION_CITY))
            assertTrue(repository.getMessages(C02ConversationFixtures.CONVERSATION_CITY).isEmpty())
            assertEquals(2, repository.observeConversationHeaders("").first().size)
            assertEquals(2, repository.getMessages(C02ConversationFixtures.CONVERSATION_GARDEN).size)
            assertEquals(2, repository.getMessages(C02ConversationFixtures.CONVERSATION_EVENING).size)
        }
    }

    private suspend fun withFixture(testBody: suspend (StudioDatabase, RoomConversationRepository) -> Unit) {
        val database = Room.inMemoryDatabaseBuilder(context, StudioDatabase::class.java).build()
        try {
            val characters = RoomCharacterRepository(database.characterDao())
            val conversations = RoomConversationRepository(database, database.conversationDao())
            C02ConversationFixtures.seed(characters, conversations)
            testBody(database, conversations)
        } finally {
            database.close()
        }
    }
}
