package com.hoangtu.aistorystudio.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "novel_characters",
    foreignKeys = [
        ForeignKey(
            entity = NovelProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CharacterProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["character_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id", "character_id"], unique = true),
        Index(value = ["character_id"])
    ]
)
data class NovelCharacterCrossRefEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "character_id")
    val characterId: String,
    @ColumnInfo(name = "linked_at")
    val linkedAt: Long
)
