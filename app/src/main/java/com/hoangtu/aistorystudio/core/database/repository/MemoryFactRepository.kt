package com.hoangtu.aistorystudio.core.database.repository

import androidx.room.withTransaction
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.dao.MemoryFactDao
import com.hoangtu.aistorystudio.core.database.entity.MemoryFactEntity
import kotlinx.coroutines.flow.Flow

interface MemoryFactRepository {
    suspend fun save(memory: MemoryFactEntity)
    suspend fun get(id: String): MemoryFactEntity?
    fun observeForCharacter(characterId: String): Flow<List<MemoryFactEntity>>
    fun observeForConversation(conversationId: String): Flow<List<MemoryFactEntity>>
    fun observeForProject(projectId: String): Flow<List<MemoryFactEntity>>
    suspend fun delete(id: String, deletedAt: Long): Boolean
}

class RoomMemoryFactRepository(
    private val database: StudioDatabase,
    private val dao: MemoryFactDao
) : MemoryFactRepository {
    override suspend fun save(memory: MemoryFactEntity) = database.withTransaction {
        val ownerCount = listOf(memory.characterId, memory.conversationId, memory.projectId)
            .count { it != null }
        val validOwner = when (memory.ownerType) {
            MemoryFactEntity.OWNER_CHARACTER -> memory.characterId != null &&
                memory.conversationId == null && memory.projectId == null
            MemoryFactEntity.OWNER_CONVERSATION -> memory.conversationId != null &&
                memory.characterId == null && memory.projectId == null
            MemoryFactEntity.OWNER_NOVEL_PROJECT -> memory.projectId != null &&
                memory.characterId == null && memory.conversationId == null
            else -> false
        }
        require(ownerCount == 1 && validOwner) { "Memory phai co dung mot chu so huu hop le" }
        val existing = dao.get(memory.id)
        require(
            existing == null ||
                (existing.ownerType == memory.ownerType &&
                    existing.characterId == memory.characterId &&
                    existing.conversationId == memory.conversationId &&
                    existing.projectId == memory.projectId)
        ) { "Khong duoc chuyen memory sang chu so huu khac" }
        require(existing?.deletedAt == null || memory.deletedAt != null) {
            "Khong duoc tu dong phuc hoi memory da danh dau xoa"
        }
        dao.upsert(memory)
    }

    override suspend fun get(id: String) = dao.get(id)
    override fun observeForCharacter(characterId: String) = dao.observeForCharacter(characterId)
    override fun observeForConversation(conversationId: String) = dao.observeForConversation(conversationId)
    override fun observeForProject(projectId: String) = dao.observeForProject(projectId)
    override suspend fun delete(id: String, deletedAt: Long) = dao.tombstone(id, deletedAt) > 0
}
