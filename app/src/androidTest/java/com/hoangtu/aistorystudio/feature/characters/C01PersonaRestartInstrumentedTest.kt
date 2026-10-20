package com.hoangtu.aistorystudio.feature.characters

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class C01PersonaRestartInstrumentedTest {
    @Test
    fun personaAndProfileAreRestoredAfterApplicationProcessRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val phase = InstrumentationRegistry.getArguments().getString("phase")
        val databaseName = InstrumentationRegistry.getArguments().getString("databaseName")
            ?: "c01_restart_${UUID.randomUUID().toString().replace("-", "")}.db"
        assertTrue(databaseName.matches(Regex("c01_restart_[0-9a-f]{32}\\.db")))

        when (phase) {
            "seed" -> {
                assertFalse(context.getDatabasePath(databaseName).exists())
                seedDatabase(context, databaseName)
            }
            "verify" -> verifyDatabase(context, databaseName)
            "cleanup" -> assertTrue(context.deleteDatabase(databaseName))
            null, "" -> {
                try {
                    assertFalse(context.getDatabasePath(databaseName).exists())
                    seedDatabase(context, databaseName)
                    verifyDatabase(context, databaseName)
                } finally {
                    assertTrue(context.deleteDatabase(databaseName))
                }
            }
            else -> throw AssertionError("Unsupported test phase.")
        }
    }

    private suspend fun seedDatabase(context: android.content.Context, databaseName: String) {
        val database = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
        try {
            val repository = RoomCharacterRepository(database.characterDao())
            val profile = CharacterProfileEntity(
                id = "c01-restart-character",
                name = "Nhân vật thử khởi động lại",
                age = 31,
                personality = "Kiên nhẫn",
                createdAt = 1L,
                updatedAt = 2L
            )
            val persona = UserPersonaEntity(
                id = "c01-restart-persona",
                name = "Persona thử khởi động lại",
                firstPerson = "mình",
                secondPerson = "bạn",
                createdAt = 3L,
                updatedAt = 4L
            )
            repository.saveProfile(profile)
            repository.savePersona(persona)
            assertTrue(repository.setDefaultPersona(persona.id))
        } finally {
            database.close()
        }
    }

    private suspend fun verifyDatabase(context: android.content.Context, databaseName: String) {
        val database = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
        try {
            val repository = RoomCharacterRepository(database.characterDao())
            assertEquals("Nhân vật thử khởi động lại", repository.getProfile("c01-restart-character")?.name)
            assertEquals(31, repository.getProfile("c01-restart-character")?.age)
            assertEquals("Persona thử khởi động lại", repository.getPersona("c01-restart-persona")?.name)
            assertTrue(repository.getPersona("c01-restart-persona")?.isDefault == true)
        } finally {
            database.close()
        }
    }
}
