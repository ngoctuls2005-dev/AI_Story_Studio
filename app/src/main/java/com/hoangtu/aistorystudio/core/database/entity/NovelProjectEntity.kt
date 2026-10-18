package com.hoangtu.aistorystudio.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "novel_projects",
    indices = [Index(value = ["updated_at"])]
)
data class NovelProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val synopsis: String = "",
    val status: String = STATUS_DRAFT,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    companion object {
        const val STATUS_DRAFT = "draft"
        const val STATUS_ACTIVE = "active"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_ARCHIVED = "archived"
    }
}
