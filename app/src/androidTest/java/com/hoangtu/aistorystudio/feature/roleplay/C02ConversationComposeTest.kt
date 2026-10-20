package com.hoangtu.aistorystudio.feature.roleplay

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderConfiguration
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderProfile
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderSettingsRepository
import com.hoangtu.aistorystudio.core.ai.settings.ApiKeyReadResult
import com.hoangtu.aistorystudio.core.ai.settings.DeleteApiKeyResult
import com.hoangtu.aistorystudio.core.ai.settings.LocalConfigurationStatus
import com.hoangtu.aistorystudio.core.ai.settings.SaveProviderResult
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomConversationRepository
import com.hoangtu.aistorystudio.core.navigation.StudioApp
import com.hoangtu.aistorystudio.feature.characters.CharacterManagementUseCases
import com.hoangtu.aistorystudio.feature.characters.CharacterManagementViewModel
import com.hoangtu.aistorystudio.feature.settings.SettingsViewModel
import com.hoangtu.aistorystudio.ui.theme.AIStoryStudioTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class C02ConversationComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: StudioDatabase
    private lateinit var characterRepository: RoomCharacterRepository
    private lateinit var conversationRepository: RoomConversationRepository
    private lateinit var roleplayViewModel: RoleplayViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, StudioDatabase::class.java).build()
        characterRepository = RoomCharacterRepository(database.characterDao())
        conversationRepository = RoomConversationRepository(database, database.conversationDao())
        runBlocking { C02ConversationFixtures.seed(characterRepository, conversationRepository) }
        roleplayViewModel = RoleplayViewModel(
            ConversationManagementUseCases(
                conversationRepository = conversationRepository,
                characterRepository = characterRepository,
                clock = { 1_000L },
                newId = { "c02-ui-created-conversation" }
            )
        )
        val characterViewModel = CharacterManagementViewModel(CharacterManagementUseCases(characterRepository))
        val settingsViewModel = SettingsViewModel(EmptySettingsRepository())
        composeRule.setContent {
            AIStoryStudioTheme {
                StudioApp(settingsViewModel, characterViewModel, roleplayViewModel)
            }
        }
        composeRule.onNodeWithTag("navigation_roleplay").performClick()
        composeRule.waitUntil(5_000) {
            !roleplayViewModel.state.value.isLoading && roleplayViewModel.state.value.conversations.size == 3
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun searchOpenHistoryAndSystemBackKeepConversationScope() {
        composeRule.onNodeWithTag("conversation_search").performTextInput("khu vườn")
        composeRule.waitUntil(5_000) {
            roleplayViewModel.state.value.conversations.map { it.conversation.id } ==
                listOf(C02ConversationFixtures.CONVERSATION_GARDEN)
        }
        composeRule.onNodeWithText("Hội thoại khu vườn").assertIsDisplayed()
        assertEquals(listOf(C02ConversationFixtures.CONVERSATION_GARDEN), roleplayViewModel.state.value.conversations.map { it.conversation.id })
        composeRule.onNodeWithTag("conversation_row_${C02ConversationFixtures.CONVERSATION_GARDEN}").performClick()
        composeRule.onNodeWithTag("conversation_history_title").assertTextEquals("Hội thoại khu vườn")
        composeRule.onNodeWithText(
            "Lịch sử riêng của Hội thoại khu vườn: mình nhớ khu vườn có hoa sen."
        ).assertIsDisplayed()
        assertTrue(
            roleplayViewModel.state.value.activeDetail?.messages?.none {
                it.content.contains("Hội thoại thành phố")
            } == true
        )

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(5_000) {
            roleplayViewModel.state.value.activeConversationId == null
        }
        composeRule.onNodeWithTag("conversation_row_${C02ConversationFixtures.CONVERSATION_GARDEN}").assertIsDisplayed()
        assertNull(roleplayViewModel.state.value.activeConversationId)
    }

    @Test
    fun newConversationSelectsCharacterAndPersonaWithoutChangingOldHistory() {
        val oldMessages = runBlocking {
            conversationRepository.getMessages(C02ConversationFixtures.CONVERSATION_GARDEN)
        }
        composeRule.onNodeWithTag("create_conversation").performClick()
        composeRule.onNodeWithTag("new_conversation_character").performClick()
        composeRule.onNodeWithTag("new_conversation_character_option_${C02ConversationFixtures.CHARACTER_MINH}")
            .performClick()
        composeRule.onNodeWithTag("new_conversation_persona").performClick()
        composeRule.onNodeWithTag("new_conversation_persona_option_${C02ConversationFixtures.PERSONA_SECOND}")
            .performClick()
        assertEquals("Trò chuyện với Minh Trần", roleplayViewModel.state.value.createDraft?.title)
        composeRule.onNodeWithTag("confirm_create_conversation").performClick()

        composeRule.waitUntil(5_000) {
            roleplayViewModel.state.value.activeDetail?.header?.conversation?.id == "c02-ui-created-conversation"
        }
        composeRule.onNodeWithTag("conversation_history_title").assertTextEquals("Trò chuyện với Minh Trần")
        composeRule.onNodeWithText("Chưa có tin nhắn").assertIsDisplayed()
        runBlocking {
            assertNotNull(conversationRepository.getConversation("c02-ui-created-conversation"))
            assertEquals(C02ConversationFixtures.CHARACTER_MINH, conversationRepository.getConversation("c02-ui-created-conversation")?.characterId)
            assertEquals(C02ConversationFixtures.PERSONA_SECOND, conversationRepository.getConversation("c02-ui-created-conversation")?.personaId)
            assertEquals(oldMessages, conversationRepository.getMessages(C02ConversationFixtures.CONVERSATION_GARDEN))
            assertEquals(C02ConversationFixtures.PERSONA_FIRST, conversationRepository.getConversation(C02ConversationFixtures.CONVERSATION_GARDEN)?.personaId)
        }

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("conversation_row_c02-ui-created-conversation").assertIsDisplayed()
    }

    @Test
    fun deleteRequiresConfirmationAndOnlyDeletesSelectedHistory() {
        composeRule.onNodeWithTag("delete_conversation_${C02ConversationFixtures.CONVERSATION_CITY}").performClick()
        composeRule.onNodeWithText("Xóa cuộc hội thoại?").assertIsDisplayed()
        composeRule.onNodeWithText("Hủy").performClick()
        runBlocking { assertNotNull(conversationRepository.getConversation(C02ConversationFixtures.CONVERSATION_CITY)) }

        composeRule.onNodeWithTag("delete_conversation_${C02ConversationFixtures.CONVERSATION_CITY}").performClick()
        composeRule.onNodeWithTag("confirm_delete_conversation").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { conversationRepository.getConversation(C02ConversationFixtures.CONVERSATION_CITY) == null }
        }
        runBlocking {
            assertNull(conversationRepository.getConversation(C02ConversationFixtures.CONVERSATION_CITY))
            assertEquals(2, conversationRepository.getMessages(C02ConversationFixtures.CONVERSATION_GARDEN).size)
            assertEquals(2, conversationRepository.getMessages(C02ConversationFixtures.CONVERSATION_EVENING).size)
        }
    }
}

private class EmptySettingsRepository : AiProviderSettingsRepository {
    override fun observeProviders(): Flow<List<AiProviderProfile>> = flowOf(emptyList())
    override suspend fun ensureDefaultProvider() = Unit
    override suspend fun saveProvider(configuration: AiProviderConfiguration, apiKey: ByteArray?): SaveProviderResult =
        SaveProviderResult.StorageUnavailable
    override suspend fun selectProvider(providerId: String): Boolean = false
    override suspend fun deleteApiKey(providerId: String): DeleteApiKeyResult = DeleteApiKeyResult.StorageUnavailable
    override suspend fun readApiKey(providerId: String): ApiKeyReadResult = ApiKeyReadResult.Missing
    override suspend fun validateLocally(providerId: String): LocalConfigurationStatus = LocalConfigurationStatus.INVALID
}
