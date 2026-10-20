package com.hoangtu.aistorystudio.feature.characters

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import com.hoangtu.aistorystudio.ui.theme.AIStoryStudioTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class C01CharacterManagementComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: StudioDatabase
    private lateinit var repository: RoomCharacterRepository
    private lateinit var viewModel: CharacterManagementViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, StudioDatabase::class.java).build()
        repository = RoomCharacterRepository(database.characterDao())
        viewModel = CharacterManagementViewModel(CharacterManagementUseCases(repository))
        composeRule.setContent {
            AIStoryStudioTheme {
                CharactersScreen(viewModel)
            }
        }
        composeRule.waitUntil(5_000) { !viewModel.state.value.isLoading }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun characterCanBeCreatedViewedEditedAndDeletedWithUnicode() {
        composeRule.onNodeWithTag("add_character_item").performClick()
        composeRule.onNodeWithTag("profile_name").performTextInput("Nguyễn Ánh")
        composeRule.onNodeWithTag("profile_age").performTextInput("28")
        composeRule.onNodeWithTag("profile_appearance").performTextInput("Tóc đen, mắt nâu")
        composeRule.onNodeWithTag("profile_roleplay_rules").performTextInput("Không điều khiển nhân vật người dùng")
        composeRule.onNodeWithTag("save_profile").performClick()
        awaitCondition { runBlocking { repository.searchProfiles("Nguyễn").size == 1 } }

        val saved = runBlocking { repository.searchProfiles("Nguyễn").single() }
        assertEquals("Nguyễn Ánh", saved.name)
        assertEquals(28, saved.age)
        assertEquals("Tóc đen, mắt nâu", saved.appearance)
        assertEquals("Không điều khiển nhân vật người dùng", saved.roleplayRules)

        composeRule.onNodeWithText("Nguyễn Ánh").performClick()
        composeRule.onNodeWithText("Quy tắc nhập vai").assertIsDisplayed()
        composeRule.onNodeWithText("Không điều khiển nhân vật người dùng").assertIsDisplayed()
        composeRule.onNodeWithText("Đóng").performClick()

        composeRule.onNodeWithTag("edit_profile").performClick()
        composeRule.onNodeWithTag("profile_name").performTextClearance()
        composeRule.onNodeWithTag("profile_name").performTextInput("Nguyễn Ánh mới")
        composeRule.onNodeWithTag("save_profile").performClick()
        awaitCondition { runBlocking { repository.searchProfiles("Nguyễn Ánh mới").size == 1 } }
        assertEquals(saved.id, runBlocking { repository.searchProfiles("Nguyễn Ánh mới").single().id })

        composeRule.onNodeWithTag("delete_profile").performClick()
        composeRule.onNodeWithTag("confirm_delete").performClick()
        awaitCondition { runBlocking { repository.getProfile(saved.id) } == null }
        assertNull(runBlocking { repository.getProfile(saved.id) })
    }

    @Test
    fun personaCanBeCreatedEditedSelectedAndDeleted() {
        composeRule.onNodeWithTag("tab_personas").performClick()
        composeRule.onNodeWithTag("add_character_item").performClick()
        composeRule.onNodeWithTag("persona_name").performTextInput("Người kể thứ nhất")
        composeRule.onNodeWithTag("persona_first_person").performTextInput("mình")
        composeRule.onNodeWithTag("persona_second_person").performTextInput("bạn")
        composeRule.onNodeWithTag("save_persona").performClick()
        awaitCondition { viewModel.state.value.personas.size == 1 }
        val first = viewModel.state.value.personas.single()

        composeRule.onNodeWithTag("select_default_persona").performClick()
        awaitCondition { runBlocking { repository.getPersona(first.id)?.isDefault == true } }
        assertEquals(true, runBlocking { repository.getPersona(first.id)?.isDefault })

        composeRule.onNodeWithTag("add_character_item").performClick()
        composeRule.onNodeWithTag("persona_name").performTextInput("Người kể thứ hai")
        composeRule.onNodeWithTag("save_persona").performClick()
        awaitCondition { viewModel.state.value.personas.size == 2 }
        val second = viewModel.state.value.personas.single { it.name == "Người kể thứ hai" }

        composeRule.onNodeWithTag("select_default_persona").performClick()
        awaitCondition { runBlocking { repository.getPersona(second.id)?.isDefault == true } }
        assertEquals(false, runBlocking { repository.getPersona(first.id)?.isDefault })

        composeRule.onNodeWithContentDescription("Sửa Người kể thứ hai").performClick()
        composeRule.onNodeWithTag("persona_name").performTextClearance()
        composeRule.onNodeWithTag("persona_name").performTextInput("Người kể đã sửa")
        composeRule.onNodeWithTag("save_persona").performClick()
        awaitCondition { viewModel.state.value.personas.any { it.name == "Người kể đã sửa" } }
        assertEquals(true, runBlocking { repository.getPersona(second.id)?.isDefault })

        composeRule.onNodeWithContentDescription("Xóa Người kể đã sửa").performClick()
        composeRule.onNodeWithTag("confirm_delete").performClick()
        awaitCondition { runBlocking { repository.getPersona(second.id) } == null }
        assertNull(runBlocking { repository.getPersona(second.id) })
    }

    @Test
    fun invalidCharacterAndPersonaAreNotPersisted() {
        composeRule.onNodeWithTag("add_character_item").performClick()
        composeRule.onNodeWithTag("save_profile").performClick()
        composeRule.onNodeWithTag("editor_validation_error").assertTextEquals("Vui lòng nhập tên nhân vật.")
        assertEquals(0, runBlocking { repository.observeProfiles().first().size })

        composeRule.onNodeWithText("Hủy").performClick()
        composeRule.onNodeWithTag("tab_personas").performClick()
        composeRule.onNodeWithTag("add_character_item").performClick()
        composeRule.onNodeWithTag("save_persona").performClick()
        composeRule.onNodeWithTag("editor_validation_error").assertTextEquals("Vui lòng nhập tên Persona.")
        assertEquals(0, runBlocking { repository.observePersonas().first().size })
    }

    private fun awaitCondition(condition: () -> Boolean) {
        composeRule.waitUntil(5_000, condition)
    }
}
