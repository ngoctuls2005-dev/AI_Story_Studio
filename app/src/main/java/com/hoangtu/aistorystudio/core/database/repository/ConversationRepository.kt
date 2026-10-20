package com.hoangtu.aistorystudio.core.database.repository

import androidx.room.withTransaction
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.dao.ConversationDao
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.MessageVariantEntity
import com.hoangtu.aistorystudio.core.database.model.ConversationHeader
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun createConversation(conversation: ConversationEntity)
    suspend fun saveConversation(conversation: ConversationEntity)
    suspend fun getConversation(id: String): ConversationEntity?
    fun observeConversations(): Flow<List<ConversationEntity>>
    fun observeConversationHeaders(query: String): Flow<List<ConversationHeader>>
    fun observeConversationHeader(id: String): Flow<ConversationHeader?>
    suspend fun deleteConversation(id: String): Boolean
    suspend fun createConversationWithFirstMessage(
        conversation: ConversationEntity,
        firstMessage: ChatMessageEntity
    )
    suspend fun appendMessage(message: ChatMessageEntity): ChatMessageEntity
    suspend fun saveMessage(message: ChatMessageEntity)
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>
    suspend fun deleteMessage(id: String): Boolean
    suspend fun addVariant(messageId: String, content: String, createdAt: Long): MessageVariantEntity
    fun observeVariants(messageId: String): Flow<List<MessageVariantEntity>>
    suspend fun deleteVariant(id: String): Boolean
    suspend fun saveSummary(summary: ConversationSummaryEntity)
    fun observeSummaries(conversationId: String): Flow<List<ConversationSummaryEntity>>
    suspend fun deleteSummary(id: String): Boolean
}

class RoomConversationRepository(
    private val database: StudioDatabase,
    private val dao: ConversationDao
) : ConversationRepository {
    override suspend fun createConversation(conversation: ConversationEntity) {
        database.withTransaction {
            dao.insertConversation(conversation)
        }
    }

    override suspend fun saveConversation(conversation: ConversationEntity) {
        database.withTransaction {
            val existing = dao.getConversation(conversation.id)
            require(existing == null || existing.characterId == conversation.characterId) {
                "Khong duoc chuyen cuoc hoi thoai sang nhan vat khac"
            }
            require(existing == null || existing.personaId == conversation.personaId) {
                "Khong duoc doi Persona cua cuoc hoi thoai da tao"
            }
            dao.upsertConversation(conversation)
        }
    }

    override suspend fun getConversation(id: String) = dao.getConversation(id)
    override fun observeConversations() = dao.observeConversations()
    override fun observeConversationHeaders(query: String) = dao.observeConversationHeaders(query.trim())
    override fun observeConversationHeader(id: String) = dao.observeConversationHeader(id)
    override suspend fun deleteConversation(id: String) = dao.deleteConversation(id) > 0

    override suspend fun createConversationWithFirstMessage(
        conversation: ConversationEntity,
        firstMessage: ChatMessageEntity
    ) {
        require(firstMessage.conversationId == conversation.id) {
            "Tin nhan dau tien phai thuoc cuoc hoi thoai moi"
        }
        database.withTransaction {
            dao.insertConversation(conversation)
            dao.insertMessage(firstMessage.copy(sequence = 1L))
            dao.updateConversationActivity(conversation.id, firstMessage.createdAt)
        }
    }

    override suspend fun appendMessage(message: ChatMessageEntity): ChatMessageEntity =
        database.withTransaction {
            check(dao.getConversation(message.conversationId) != null) {
                "Cuoc hoi thoai khong ton tai"
            }
            val sequencedMessage = message.copy(sequence = dao.nextMessageSequence(message.conversationId))
            dao.insertMessage(sequencedMessage)
            dao.updateConversationActivity(message.conversationId, message.createdAt)
            sequencedMessage
        }

    override suspend fun saveMessage(message: ChatMessageEntity) {
        database.withTransaction {
            val existing = dao.getMessage(message.id)
            if (existing == null) {
                dao.insertMessage(message)
            } else {
                require(existing.conversationId == message.conversationId) {
                    "Khong duoc chuyen tin nhan sang cuoc hoi thoai khac"
                }
                require(existing.sequence == message.sequence) {
                    "Khong duoc thay doi thu tu cua tin nhan da luu"
                }
                dao.upsertMessage(message)
            }
            dao.updateConversationActivity(message.conversationId, message.createdAt)
        }
    }
    override fun observeMessages(conversationId: String) = dao.observeMessages(conversationId)
    override suspend fun getMessages(conversationId: String) = dao.getMessages(conversationId)
    override suspend fun deleteMessage(id: String) = dao.deleteMessage(id) > 0

    override suspend fun addVariant(
        messageId: String,
        content: String,
        createdAt: Long
    ): MessageVariantEntity = database.withTransaction {
        check(dao.getMessage(messageId) != null) { "Tin nhan khong ton tai" }
        val variant = MessageVariantEntity(
            messageId = messageId,
            content = content,
            variantOrder = dao.nextVariantOrder(messageId),
            createdAt = createdAt
        )
        dao.upsertVariant(variant)
        variant
    }

    override fun observeVariants(messageId: String) = dao.observeVariants(messageId)
    override suspend fun deleteVariant(id: String) = dao.deleteVariant(id) > 0

    override suspend fun saveSummary(summary: ConversationSummaryEntity) {
        require(summary.coveredFromSequence > 0L)
        require(summary.coveredThroughSequence >= summary.coveredFromSequence)
        database.withTransaction {
            val existing = dao.getSummary(summary.id)
            require(existing == null || existing.conversationId == summary.conversationId) {
                "Khong duoc chuyen summary sang cuoc hoi thoai khac"
            }
            dao.upsertSummary(summary)
        }
    }

    override fun observeSummaries(conversationId: String) = dao.observeSummaries(conversationId)
    override suspend fun deleteSummary(id: String) = dao.deleteSummary(id) > 0
}
