package com.hoangtu.aistorystudio.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity

data class ConversationHeader(
    @Embedded
    val conversation: ConversationEntity,
    @ColumnInfo(name = "character_name")
    val characterName: String,
    @ColumnInfo(name = "persona_name")
    val personaName: String?
)
