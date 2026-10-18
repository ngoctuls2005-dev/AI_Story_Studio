package com.hoangtu.aistorystudio.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "conversation_summaries",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversation_id", "covered_through_sequence"])]
)
data class ConversationSummaryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "covered_from_sequence")
    val coveredFromSequence: Long,
    @ColumnInfo(name = "covered_through_sequence")
    val coveredThroughSequence: Long,
    val content: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
