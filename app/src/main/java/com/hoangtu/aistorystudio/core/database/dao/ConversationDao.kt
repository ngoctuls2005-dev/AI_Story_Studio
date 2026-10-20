package com.hoangtu.aistorystudio.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hoangtu.aistorystudio.core.database.entity.ChatMessageEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationEntity
import com.hoangtu.aistorystudio.core.database.entity.ConversationSummaryEntity
import com.hoangtu.aistorystudio.core.database.entity.MessageVariantEntity
import com.hoangtu.aistorystudio.core.database.model.ConversationHeader
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY updated_at DESC, id ASC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query(
        """
        SELECT c.*, character_profiles.name AS character_name, user_personas.name AS persona_name
        FROM conversations AS c
        INNER JOIN character_profiles ON character_profiles.id = c.character_id
        LEFT JOIN user_personas ON user_personas.id = c.persona_id
        WHERE (:query = '' OR c.title LIKE '%' || :query || '%' COLLATE NOCASE
            OR character_profiles.name LIKE '%' || :query || '%' COLLATE NOCASE
            OR user_personas.name LIKE '%' || :query || '%' COLLATE NOCASE)
        ORDER BY c.updated_at DESC, c.created_at DESC, c.id ASC
        """
    )
    fun observeConversationHeaders(query: String): Flow<List<ConversationHeader>>

    @Query(
        """
        SELECT c.*, character_profiles.name AS character_name, user_personas.name AS persona_name
        FROM conversations AS c
        INNER JOIN character_profiles ON character_profiles.id = c.character_id
        LEFT JOIN user_personas ON user_personas.id = c.persona_id
        WHERE c.id = :id
        """
    )
    fun observeConversationHeader(id: String): Flow<ConversationHeader?>

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Upsert
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE id = :id")
    suspend fun getMessage(id: String): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY sequence ASC, id ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY sequence ASC, id ASC")
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>

    @Query("SELECT COALESCE(MAX(sequence), 0) + 1 FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun nextMessageSequence(conversationId: String): Long

    @Query("UPDATE conversations SET updated_at = MAX(updated_at, :updatedAt) WHERE id = :conversationId")
    suspend fun updateConversationActivity(conversationId: String, updatedAt: Long)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: String): Int

    @Upsert
    suspend fun upsertVariant(variant: MessageVariantEntity)

    @Query("SELECT * FROM message_variants WHERE message_id = :messageId ORDER BY variant_order ASC, id ASC")
    fun observeVariants(messageId: String): Flow<List<MessageVariantEntity>>

    @Query("SELECT COALESCE(MAX(variant_order), 0) + 1 FROM message_variants WHERE message_id = :messageId")
    suspend fun nextVariantOrder(messageId: String): Long

    @Query("DELETE FROM message_variants WHERE id = :id")
    suspend fun deleteVariant(id: String): Int

    @Upsert
    suspend fun upsertSummary(summary: ConversationSummaryEntity)

    @Query("SELECT * FROM conversation_summaries WHERE conversation_id = :conversationId ORDER BY covered_through_sequence DESC, id ASC")
    fun observeSummaries(conversationId: String): Flow<List<ConversationSummaryEntity>>

    @Query("DELETE FROM conversation_summaries WHERE id = :id")
    suspend fun deleteSummary(id: String): Int

    @Query("SELECT * FROM conversation_summaries WHERE id = :id")
    suspend fun getSummary(id: String): ConversationSummaryEntity?
}
