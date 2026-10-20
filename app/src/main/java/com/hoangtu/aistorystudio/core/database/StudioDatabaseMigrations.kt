package com.hoangtu.aistorystudio.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object StudioDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE character_profiles ADD COLUMN age INTEGER")
            db.execSQL("ALTER TABLE character_profiles ADD COLUMN appearance TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE character_profiles ADD COLUMN background TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE character_profiles ADD COLUMN roleplay_rules TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE user_personas ADD COLUMN is_default INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
