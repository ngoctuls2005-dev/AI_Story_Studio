package com.hoangtu.aistorystudio.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "memory_facts",
    foreignKeys = [
        ForeignKey(
            entity = CharacterProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["character_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NovelProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["character_id", "created_at"]),
        Index(value = ["conversation_id", "created_at"]),
        Index(value = ["project_id", "created_at"]),
        Index(value = ["status", "pinned"])
    ]
)
data class MemoryFactEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "owner_type")
    val ownerType: String,
    @ColumnInfo(name = "character_id")
    val characterId: String? = null,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String? = null,
    @ColumnInfo(name = "project_id")
    val projectId: String? = null,
    val content: String,
    val source: String,
    val status: String = STATUS_INFERRED,
    val pinned: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
) {
    companion object {
        const val OWNER_CHARACTER = "character"
        const val OWNER_CONVERSATION = "conversation"
        const val OWNER_NOVEL_PROJECT = "novel_project"

        const val STATUS_CANON = "canon"
        const val STATUS_INFERRED = "inferred"
        const val STATUS_PENDING = "pending"
    }
}
