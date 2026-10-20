package com.hoangtu.aistorystudio.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "user_personas",
    indices = [Index(value = ["name"])]
)
data class UserPersonaEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    @ColumnInfo(name = "first_person")
    val firstPerson: String = "",
    @ColumnInfo(name = "second_person")
    val secondPerson: String = "",
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
