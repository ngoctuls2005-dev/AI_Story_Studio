package com.hoangtu.aistorystudio.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.database.Cursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class A03MigrationInstrumentedTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudioDatabase::class.java
    )

    @Test
    fun legacyVersionOneDataIsPreservedByMigrationToCurrentSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "a03_schema_v1_${UUID.randomUUID()}.db"
        try {
            val legacyDatabase = migrationHelper.createDatabase(databaseName, 1)
            legacyDatabase.execSQL(
                "INSERT INTO character_profiles (id, name, description, personality, relationship, speaking_style, created_at, updated_at) " +
                    "VALUES ('legacy-character', 'Nhân vật cũ', 'Mô tả cũ', 'Điềm tĩnh', 'Bạn bè', 'Mình - bạn', 10, 20)"
            )
            legacyDatabase.execSQL(
                "INSERT INTO user_personas (id, name, description, first_person, second_person, created_at, updated_at) " +
                    "VALUES ('legacy-persona', 'Persona cũ', 'Mô tả persona', 'mình', 'bạn', 11, 21)"
            )
            legacyDatabase.execSQL(
                "INSERT INTO conversations (id, title, character_id, persona_id, created_at, updated_at) " +
                    "VALUES ('legacy-conversation', 'Hội thoại cũ', 'legacy-character', 'legacy-persona', 12, 22)"
            )
            legacyDatabase.close()

            val migratedDatabase = migrationHelper.runMigrationsAndValidate(
                databaseName,
                StudioDatabase.VERSION,
                true,
                *StudioDatabaseMigrations.ALL
            )
            migratedDatabase.query(
                "SELECT name, description, personality, relationship, speaking_style, age, appearance, background, roleplay_rules " +
                    "FROM character_profiles WHERE id = 'legacy-character'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Nhân vật cũ", cursor.getString(0))
                assertEquals("Mô tả cũ", cursor.getString(1))
                assertEquals("Điềm tĩnh", cursor.getString(2))
                assertEquals("Bạn bè", cursor.getString(3))
                assertEquals("Mình - bạn", cursor.getString(4))
                assertNull(cursor.getString(5))
                assertEquals("", cursor.getString(6))
                assertEquals("", cursor.getString(7))
                assertEquals("", cursor.getString(8))
            }
            migratedDatabase.query(
                "SELECT name, first_person, second_person, is_default FROM user_personas WHERE id = 'legacy-persona'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Persona cũ", cursor.getString(0))
                assertEquals("mình", cursor.getString(1))
                assertEquals("bạn", cursor.getString(2))
                assertEquals(0, cursor.getInt(3))
            }
            migratedDatabase.query(
                "SELECT title, character_id, persona_id FROM conversations WHERE id = 'legacy-conversation'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Hội thoại cũ", cursor.getString(0))
                assertEquals("legacy-character", cursor.getString(1))
                assertEquals("legacy-persona", cursor.getString(2))
            }
            migratedDatabase.close()
            assertEquals(2, StudioDatabase.VERSION)
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun roomCanOpenDatabaseUsingCurrentSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "a03_open_current_${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
        try {
            assertEquals(StudioDatabase.VERSION, database.openHelper.writableDatabase.version)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
