package com.hoangtu.aistorystudio.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hoangtu.aistorystudio.core.database.dao.CharacterDao
import com.hoangtu.aistorystudio.core.database.dao.ConversationDao
import com.hoangtu.aistorystudio.core.database.dao.MemoryFactDao
import com.hoangtu.aistorystudio.core.database.dao.NovelDao
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.MemoryFactEntity
import com.hoangtu.aistorystudio.core.database.entity.MessageVariantEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelChapterEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelCharacterCrossRefEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelProjectEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSceneEntity
import com.hoangtu.aistorystudio.core.database.entity.NovelSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.SceneRevisionEntity
import com.hoangtu.aistorystudio.core.database.entity.TimelineEventEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity

@Database(
    entities = [
        CharacterProfileEntity::class,
        UserPersonaEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
        MessageVariantEntity::class,
        ConversationSummaryEntity::class,
        NovelProjectEntity::class,
        NovelChapterEntity::class,
        NovelSceneEntity::class,
        SceneRevisionEntity::class,
        NovelCharacterCrossRefEntity::class,
        MemoryFactEntity::class,
        NovelSummaryEntity::class,
        TimelineEventEntity::class
    ],
    version = StudioDatabase.VERSION,
    exportSchema = true
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun conversationDao(): ConversationDao
    abstract fun novelDao(): NovelDao
    abstract fun memoryFactDao(): MemoryFactDao

    companion object {
        const val VERSION = 2
        const val DATABASE_NAME = "ai_story_studio.db"

        @Volatile
        private var instance: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                StudioDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*StudioDatabaseMigrations.ALL)
                .build()
                .also { instance = it }
        }
    }
}
