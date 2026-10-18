package com.hoangtu.aistorystudio.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hoangtu.aistorystudio.core.database.entity.MemoryFactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryFactDao {
    @Upsert
    suspend fun upsert(memory: MemoryFactEntity)

    @Query("SELECT * FROM memory_facts WHERE id = :id")
    suspend fun get(id: String): MemoryFactEntity?

    @Query("SELECT * FROM memory_facts WHERE character_id = :characterId AND deleted_at IS NULL ORDER BY pinned DESC, updated_at DESC, id ASC")
    fun observeForCharacter(characterId: String): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts WHERE conversation_id = :conversationId AND deleted_at IS NULL ORDER BY pinned DESC, updated_at DESC, id ASC")
    fun observeForConversation(conversationId: String): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY pinned DESC, updated_at DESC, id ASC")
    fun observeForProject(projectId: String): Flow<List<MemoryFactEntity>>

    @Query("UPDATE memory_facts SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id AND deleted_at IS NULL")
    suspend fun tombstone(id: String, deletedAt: Long): Int
}
