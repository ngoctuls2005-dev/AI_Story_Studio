package com.hoangtu.aistorystudio.feature.characters

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.repository.CharacterDeleteResult
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class C01CharacterRepositoryInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun personaDefaultSelectionIsAtomicAndReferencedRowsCannotBeDeleted() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, StudioDatabase::class.java).build()
        try {
            val dao = database.characterDao()
            val repository = RoomCharacterRepository(dao)
            val character = CharacterProfileEntity(
                id = "c01-character",
                name = "Nhân vật C01",
                age = 24,
                createdAt = 1L,
                updatedAt = 1L
            )
            val first = persona("c01-persona-a", "Persona A")
            val second = persona("c01-persona-b", "Persona B")
            repository.saveProfile(character)
            repository.savePersona(first)
            repository.savePersona(second)

            assertTrue(repository.setDefaultPersona(first.id))
            assertTrue(repository.setDefaultPersona(second.id))
            assertFalse(repository.setDefaultPersona("missing-persona"))
            assertEquals(
                listOf(second.id),
                repository.observePersonas().first().filter(UserPersonaEntity::isDefault).map(UserPersonaEntity::id)
            )

            database.conversationDao().upsertConversation(
                ConversationEntity(
                    id = "c01-conversation",
                    title = "Dữ liệu kiểm thử",
                    characterId = character.id,
                    personaId = second.id,
                    createdAt = 2L,
                    updatedAt = 2L
                )
            )
            assertEquals(CharacterDeleteResult.REFERENCED, repository.deletePersona(second.id))
            assertEquals(CharacterDeleteResult.REFERENCED, repository.deleteProfile(character.id))
            assertEquals("Persona B", repository.getPersona(second.id)?.name)
            assertEquals("Nhân vật C01", repository.getProfile(character.id)?.name)
        } finally {
            database.close()
        }
    }

    @Test
    fun profileFieldsAndDefaultPersonaSurviveDatabaseReopen() = runBlocking {
        val databaseName = "c01_reopen_${UUID.randomUUID()}.db"
        var firstOpen: StudioDatabase? = null
        var secondOpen: StudioDatabase? = null
        try {
            firstOpen = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
            val firstRepository = RoomCharacterRepository(firstOpen.characterDao())
            val profile = CharacterProfileEntity(
                id = "c01-persistent-profile",
                name = "Nguyễn Ánh",
                description = "Người kể chuyện",
                age = 28,
                appearance = "Tóc đen, mắt nâu",
                background = "Lớn lên ở Huế",
                personality = "Điềm đạm",
                relationship = "Bạn đồng hành",
                speakingStyle = "Mình - bạn",
                roleplayRules = "Không điều khiển nhân vật người dùng",
                createdAt = 10L,
                updatedAt = 11L
            )
            val persona = persona("c01-persistent-persona", "Người kể chuyện")
            firstRepository.saveProfile(profile)
            firstRepository.savePersona(persona)
            assertTrue(firstRepository.setDefaultPersona(persona.id))
            firstOpen.close()
            firstOpen = null

            secondOpen = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
            val secondRepository = RoomCharacterRepository(secondOpen.characterDao())
            assertEquals(profile, secondRepository.getProfile(profile.id))
            assertTrue(secondRepository.getPersona(persona.id)?.isDefault == true)
            assertEquals(
                listOf(profile),
                secondRepository.searchProfiles("Nguyễn")
            )
            assertTrue(secondRepository.searchProfiles("Lớn lên").isEmpty())
        } finally {
            firstOpen?.close()
            secondOpen?.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun deletingUnreferencedDataReturnsExplicitResult() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, StudioDatabase::class.java).build()
        try {
            val repository = RoomCharacterRepository(database.characterDao())
            val profile = CharacterProfileEntity(id = "delete-profile", name = "Xóa được", age = 18, createdAt = 1L, updatedAt = 1L)
            val persona = persona("delete-persona", "Persona xóa được")
            repository.saveProfile(profile)
            repository.savePersona(persona)

            assertEquals(CharacterDeleteResult.DELETED, repository.deleteProfile(profile.id))
            assertEquals(CharacterDeleteResult.DELETED, repository.deletePersona(persona.id))
            assertEquals(CharacterDeleteResult.NOT_FOUND, repository.deleteProfile(profile.id))
            assertNull(repository.getProfile(profile.id))
        } finally {
            database.close()
        }
    }

    private fun persona(id: String, name: String) = UserPersonaEntity(
        id = id,
        name = name,
        description = "Persona giả dùng trong kiểm thử.",
        firstPerson = "mình",
        secondPerson = "bạn",
        createdAt = 1L,
        updatedAt = 1L
    )
}
