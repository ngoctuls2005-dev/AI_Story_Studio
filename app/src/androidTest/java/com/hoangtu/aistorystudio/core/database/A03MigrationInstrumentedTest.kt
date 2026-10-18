package com.hoangtu.aistorystudio.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
    fun exportedVersionOneSchemaCanBeCreatedAndValidated() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "a03_schema_v1_${UUID.randomUUID()}.db"
        try {
            migrationHelper.createDatabase(databaseName, StudioDatabase.VERSION).close()
            migrationHelper.runMigrationsAndValidate(
                databaseName,
                StudioDatabase.VERSION,
                true,
                *StudioDatabaseMigrations.ALL
            ).close()
            assertEquals(1, StudioDatabase.VERSION)
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun roomCanOpenDatabaseUsingCurrentSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "a03_open_v1_${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
        try {
            assertEquals(StudioDatabase.VERSION, database.openHelper.writableDatabase.version)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
