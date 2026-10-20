package com.hoangtu.aistorystudio.feature.roleplay

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomConversationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class C02ConversationRestartInstrumentedTest {
    @Test
    fun conversationAndMessageHistorySurviveProcessRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val phase = InstrumentationRegistry.getArguments().getString("phase")
        val databaseName = InstrumentationRegistry.getArguments().getString("databaseName")
            ?: "c02_restart_${UUID.randomUUID().toString().replace("-", "")}.db"
        assertTrue(databaseName.matches(Regex("c02_restart_[0-9a-f]{32}\\.db")))

        when (phase) {
            "seed" -> {
                assertFalse(context.getDatabasePath(databaseName).exists())
                seed(context, databaseName)
            }
            "verify" -> verify(context, databaseName)
            "cleanup" -> assertTrue(context.deleteDatabase(databaseName))
            null, "" -> {
                try {
                    assertFalse(context.getDatabasePath(databaseName).exists())
                    seed(context, databaseName)
                    verify(context, databaseName)
                } finally {
                    assertTrue(context.deleteDatabase(databaseName))
                }
            }
            else -> throw AssertionError("Unsupported test phase.")
        }
    }

    private suspend fun seed(context: android.content.Context, databaseName: String) {
        val database = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
        try {
            C02ConversationFixtures.seed(
                RoomCharacterRepository(database.characterDao()),
                RoomConversationRepository(database, database.conversationDao())
            )
        } finally {
            database.close()
        }
    }

    private suspend fun verify(context: android.content.Context, databaseName: String) {
        val database = Room.databaseBuilder(context, StudioDatabase::class.java, databaseName).build()
        try {
            val repository = RoomConversationRepository(database, database.conversationDao())
            val headers = repository.observeConversationHeaders("").first()
            assertEquals(3, headers.size)
            assertEquals(
                "Linh Nguyễn",
                headers.first { it.conversation.id == C02ConversationFixtures.CONVERSATION_GARDEN }.characterName
            )
            assertEquals(
                "Người kể một",
                headers.first { it.conversation.id == C02ConversationFixtures.CONVERSATION_GARDEN }.personaName
            )
            val gardenMessages = repository.getMessages(C02ConversationFixtures.CONVERSATION_GARDEN)
            val cityMessages = repository.getMessages(C02ConversationFixtures.CONVERSATION_CITY)
            assertEquals(listOf(1L, 2L), gardenMessages.map { it.sequence })
            assertEquals("Xin chào từ Hội thoại khu vườn.", gardenMessages.first().content)
            assertEquals("Lịch sử riêng của Hội thoại khu vườn: mình nhớ khu vườn có hoa sen.", gardenMessages.last().content)
            assertTrue(gardenMessages.none { it.id in cityMessages.map { cityMessage -> cityMessage.id } })
            assertEquals(2, repository.observeConversationHeaders("Hội thoại").first().size)
        } finally {
            database.close()
        }
    }
}
