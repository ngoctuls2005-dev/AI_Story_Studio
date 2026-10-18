package com.hoangtu.aistorystudio.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "novel_summaries",
    foreignKeys = [
        ForeignKey(
            entity = NovelProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["project_id", "created_at"])]
)
data class NovelSummaryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "project_id")
    val projectId: String,
    val content: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
