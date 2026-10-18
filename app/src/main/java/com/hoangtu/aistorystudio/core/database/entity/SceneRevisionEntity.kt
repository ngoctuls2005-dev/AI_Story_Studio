package com.hoangtu.aistorystudio.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "scene_revisions",
    foreignKeys = [
        ForeignKey(
            entity = NovelSceneEntity::class,
            parentColumns = ["id"],
            childColumns = ["scene_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scene_id", "revision_number"], unique = true)]
)
data class SceneRevisionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "scene_id")
    val sceneId: String,
    @ColumnInfo(name = "revision_number")
    val revisionNumber: Long,
    @ColumnInfo(name = "content_snapshot")
    val contentSnapshot: String,
    val label: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
