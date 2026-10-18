package com.hoangtu.aistorystudio.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.MemoryFactEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelProjectEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.TimelineEventEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomConversationRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomMemoryFactRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomNovelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class A03RoomInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun newInMemoryDatabase(): StudioDatabase =
        Room.inMemoryDatabaseBuilder(context, StudioDatabase::class.java).build()

    private suspend fun withInMemoryDatabase(testBody: suspend (StudioDatabase) -> Unit) {
        val database = newInMemoryDatabase()
        try {
            testBody(database)
        } finally {
            database.close()
        }
    }

    @Test
    fun profileAndPersonaSupportCreateReadUpdateDeleteAndVietnameseText() = runBlocking {
        withInMemoryDatabase { database ->
            val repository = RoomCharacterRepository(database.characterDao())
            val profile = CharacterProfileEntity(
                id = "character-vn",
                name = "Nguyễn Ánh",
                description = "Nhân vật ở phố cổ Hà Nội.",
                personality = "Điềm đạm, giàu lòng trắc ẩn.",
                relationship = "Bạn đồng hành",
                speakingStyle = "Tự nhiên, có dấu tiếng Việt.",
                createdAt = 10L,
                updatedAt = 10L
            )
            repository.saveProfile(profile)
            assertEquals(profile, repository.getProfile(profile.id))
            assertEquals(profile.name, repository.searchProfiles("Nguyễn").single().name)

            val updated = profile.copy(description = "Đã cập nhật: thành phố Huế.", updatedAt = 20L)
            repository.saveProfile(updated)
            assertEquals(updated, repository.getProfile(profile.id))
            assertEquals(updated, repository.observeProfiles().first().single())
            assertTrue(repository.deleteProfile(profile.id))
            assertNull(repository.getProfile(profile.id))

            val persona = UserPersonaEntity(
                id = "persona-vn",
                name = "Tôi",
                description = "Người kể chuyện tiếng Việt.",
                firstPerson = "mình",
                secondPerson = "bạn",
                createdAt = 1L,
                updatedAt = 1L
            )
            repository.savePersona(persona)
            assertEquals(persona, repository.getPersona(persona.id))
            repository.savePersona(persona.copy(name = "Người kể", updatedAt = 2L))
            assertEquals("Người kể", repository.observePersonas().first().single().name)
            assertTrue(repository.deletePersona(persona.id))
            assertFalse(repository.deletePersona(persona.id))
        }
    }

    @Test
    fun dataSurvivesDatabaseCloseAndReopen() = runBlocking {
        val databaseName = "a03_reopen_${UUID.randomUUID()}.db"
        var firstOpen: StudioDatabase? = null
        var secondOpen: StudioDatabase? = null
        try {
            firstOpen = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
            val stored = CharacterProfileEntity(
                id = "persisted-character",
                name = "Lưu bền vững",
                createdAt = 1L,
                updatedAt = 1L
            )
            firstOpen.characterDao().upsertProfile(stored)
            firstOpen.close()
            firstOpen = null

            secondOpen = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
            assertEquals(stored, secondOpen.characterDao().getProfile(stored.id))
        } finally {
            firstOpen?.close()
            secondOpen?.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun messagesAndChaptersRemainScopedToTheirParentsAndOrdered() = runBlocking {
        withInMemoryDatabase { database ->
            val characterRepository = RoomCharacterRepository(database.characterDao())
            val conversationRepository = RoomConversationRepository(database, database.conversationDao())
            val novelRepository = RoomNovelRepository(database, database.novelDao())
            characterRepository.saveProfile(
                CharacterProfileEntity(id = "scope-character", name = "Nhân vật", createdAt = 1L, updatedAt = 1L)
            )

            val conversationA = conversation("conversation-a", "scope-character")
            val conversationB = conversation("conversation-b", "scope-character")
            conversationRepository.saveConversation(conversationA)
            conversationRepository.saveConversation(conversationB)
            conversationRepository.appendMessage(message("a-2", conversationA.id, "Thứ hai"))
            conversationRepository.appendMessage(message("b-1", conversationB.id, "Hội thoại B"))
            conversationRepository.appendMessage(message("a-1", conversationA.id, "Thứ nhất"))
            assertEquals(
                listOf("Thứ hai", "Thứ nhất"),
                conversationRepository.getMessages(conversationA.id).map { it.content }
            )
            assertEquals(listOf("Hội thoại B"), conversationRepository.getMessages(conversationB.id).map { it.content })
            assertEquals(listOf(1L, 2L), conversationRepository.getMessages(conversationA.id).map { it.sequence })
            assertEquals(listOf("Thứ hai", "Thứ nhất"), conversationRepository.observeMessages(conversationA.id).first().map { it.content })
            val variant = conversationRepository.addVariant("a-1", "Phương án trả lời khác", 3L)
            assertEquals(listOf(variant), conversationRepository.observeVariants("a-1").first())
            val conversationSummary = ConversationSummaryEntity(
                id = "summary-conversation-a",
                conversationId = conversationA.id,
                coveredFromSequence = 1L,
                coveredThroughSequence = 2L,
                content = "Tóm tắt hội thoại A",
                createdAt = 4L
            )
            conversationRepository.saveSummary(conversationSummary)
            assertEquals(listOf(conversationSummary), conversationRepository.observeSummaries(conversationA.id).first())

            val projectA = NovelProjectEntity(id = "project-a", title = "Truyện A", createdAt = 1L, updatedAt = 1L)
            val projectB = NovelProjectEntity(id = "project-b", title = "Truyện B", createdAt = 1L, updatedAt = 1L)
            novelRepository.saveProject(projectA)
            novelRepository.saveProject(projectB)
            val chapterA = novelRepository.addChapter(projectA.id, "Chương A", 2L)
            val chapterB = novelRepository.addChapter(projectB.id, "Chương B", 2L)
            assertEquals(1L, chapterA.chapterOrder)
            assertEquals(1L, chapterB.chapterOrder)
            assertEquals(listOf(chapterA), novelRepository.observeChapters(projectA.id).first())
            assertEquals(listOf(chapterB), novelRepository.observeChapters(projectB.id).first())
            val scene = novelRepository.addScene(chapterA.id, "Cảnh mở đầu", 3L)
            val revision = novelRepository.addRevision(scene.id, "Bản thảo đầu tiên", "V1", 4L)
            assertEquals(listOf(scene), novelRepository.observeScenes(chapterA.id).first())
            assertEquals(listOf(revision), novelRepository.observeRevisions(scene.id).first())
            val novelSummary = NovelSummaryEntity(
                id = "summary-project-a",
                projectId = projectA.id,
                content = "Tóm tắt truyện A",
                createdAt = 5L
            )
            novelRepository.saveSummary(novelSummary)
            assertEquals(listOf(novelSummary), novelRepository.observeSummaries(projectA.id).first())
            assertTrue(novelRepository.observeSummaries(projectB.id).first().isEmpty())
            val event = TimelineEventEntity(
                id = "event-project-a",
                projectId = projectA.id,
                title = "Biến cố đầu tiên",
                eventOrder = 99L,
                occurredAt = 6L,
                createdAt = 6L
            )
            val storedEvent = novelRepository.addTimelineEvent(event)
            assertEquals(1L, storedEvent.eventOrder)
            assertEquals(listOf(storedEvent), novelRepository.observeTimeline(projectA.id).first())
            assertTrue(novelRepository.observeTimeline(projectB.id).first().isEmpty())
            novelRepository.linkCharacter(projectA.id, "scope-character", 7L)
            assertEquals(listOf("scope-character"), novelRepository.observeCharacterIds(projectA.id).first())
            assertTrue(novelRepository.observeCharacterIds(projectB.id).first().isEmpty())
        }
    }

    @Test
    fun memoryFactsRequireOneValidOwnerAndRemainWithinThatScope() = runBlocking {
        withInMemoryDatabase { database ->
            val characterRepository = RoomCharacterRepository(database.characterDao())
            val conversationRepository = RoomConversationRepository(database, database.conversationDao())
            val memoryRepository = RoomMemoryFactRepository(database, database.memoryFactDao())
            characterRepository.saveProfile(
                CharacterProfileEntity(id = "memory-character", name = "Nhân vật", createdAt = 1L, updatedAt = 1L)
            )
            val parentConversation = conversation("memory-conversation", "memory-character")
            conversationRepository.saveConversation(parentConversation)

            val invalid = MemoryFactEntity(
                id = "invalid-memory",
                ownerType = MemoryFactEntity.OWNER_CONVERSATION,
                characterId = "memory-character",
                conversationId = parentConversation.id,
                content = "Không hợp lệ",
                source = "test",
                createdAt = 1L,
                updatedAt = 1L
            )
            val validationError = runCatching { memoryRepository.save(invalid) }.exceptionOrNull()
            assertNotNull(validationError)
            assertTrue(validationError is IllegalArgumentException)

            val memory = MemoryFactEntity(
                id = "conversation-memory",
                ownerType = MemoryFactEntity.OWNER_CONVERSATION,
                conversationId = parentConversation.id,
                content = "Nhân vật đã hứa quay lại.",
                source = "user_confirmed",
                status = MemoryFactEntity.STATUS_CANON,
                createdAt = 2L,
                updatedAt = 2L
            )
            memoryRepository.save(memory)
            assertEquals(memory, memoryRepository.get(memory.id))
            val updatedMemory = memory.copy(content = "Lời hứa đã được xác nhận.", updatedAt = 3L)
            memoryRepository.save(updatedMemory)
            assertEquals(listOf(updatedMemory), memoryRepository.observeForConversation(parentConversation.id).first())
            assertTrue(memoryRepository.observeForCharacter("memory-character").first().isEmpty())
            assertTrue(memoryRepository.observeForProject("other-project").first().isEmpty())

            val movedMemory = updatedMemory.copy(
                ownerType = MemoryFactEntity.OWNER_NOVEL_PROJECT,
                conversationId = null,
                projectId = "other-project"
            )
            val moveError = runCatching { memoryRepository.save(movedMemory) }.exceptionOrNull()
            assertNotNull(moveError)
            assertTrue(moveError is IllegalArgumentException)
            assertTrue(memoryRepository.delete(memory.id, 4L))
            assertEquals(4L, memoryRepository.get(memory.id)?.deletedAt)
            assertTrue(memoryRepository.observeForConversation(parentConversation.id).first().isEmpty())
            val restoreError = runCatching { memoryRepository.save(updatedMemory) }.exceptionOrNull()
            assertNotNull(restoreError)
            assertTrue(restoreError is IllegalArgumentException)
        }
    }

    @Test
    fun foreignKeysAreEnforcedAndConversationChildrenCascadeOnDelete() = runBlocking {
        withInMemoryDatabase { database ->
            val dao = database.conversationDao()
            val characterRepository = RoomCharacterRepository(database.characterDao())
            val invalidConversation = conversation("orphan-conversation", "missing-character")
            val foreignKeyError = runCatching { dao.upsertConversation(invalidConversation) }.exceptionOrNull()
            assertNotNull(foreignKeyError)
            assertTrue(foreignKeyError is SQLiteConstraintException)

            characterRepository.saveProfile(
                CharacterProfileEntity(id = "parent-character", name = "Cha", createdAt = 1L, updatedAt = 1L)
            )
            val parent = conversation("parent-conversation", "parent-character")
            dao.upsertConversation(parent)
            val restrictedDelete = runCatching { characterRepository.deleteProfile("parent-character") }.exceptionOrNull()
            assertNotNull(restrictedDelete)
            assertTrue(restrictedDelete is SQLiteConstraintException)
            assertNotNull(characterRepository.getProfile("parent-character"))
            dao.upsertMessage(message("child-message", parent.id, "Nội dung"))
            assertEquals(1, dao.deleteConversation(parent.id))
            assertTrue(dao.getMessages(parent.id).isEmpty())
        }
    }

    @Test
    fun failedConversationCreationRollsBackAllRows() = runBlocking {
        withInMemoryDatabase { database ->
            val characterRepository = RoomCharacterRepository(database.characterDao())
            val conversationRepository = RoomConversationRepository(database, database.conversationDao())
            characterRepository.saveProfile(
                CharacterProfileEntity(id = "rollback-character", name = "Nhân vật", createdAt = 1L, updatedAt = 1L)
            )
            val originalConversation = conversation("rollback-original", "rollback-character")
            conversationRepository.saveConversation(originalConversation)
            val originalMessage = message("duplicate-message-id", originalConversation.id, "Bản gốc")
            conversationRepository.saveMessage(originalMessage)

            val attemptedConversation = conversation("rollback-attempt", "rollback-character")
            val attemptedMessage = message("duplicate-message-id", attemptedConversation.id, "Không được ghi")
            val failure = runCatching {
                conversationRepository.createConversationWithFirstMessage(attemptedConversation, attemptedMessage)
            }.exceptionOrNull()

            assertNotNull(failure)
            assertTrue(failure is SQLiteConstraintException)
            assertNull(conversationRepository.getConversation(attemptedConversation.id))
            assertEquals(originalMessage, database.conversationDao().getMessage(originalMessage.id))
        }
    }

    private fun conversation(id: String, characterId: String) = ConversationEntity(
        id = id,
        title = id,
        characterId = characterId,
        createdAt = 1L,
        updatedAt = 1L
    )

    private fun message(id: String, conversationId: String, content: String) = ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        role = ChatMessageEntity.ROLE_USER,
        content = content,
        sequence = 999L,
        createdAt = 1L
    )
}
