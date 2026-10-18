package com.hoangtu.aistorystudio.testutil

import com.hoangtu.aistorystudio.core.ai.AiStreamEvent
import com.hoangtu.aistorystudio.core.database.entity.MemoryFactEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AiStoryTestFixturesTest {
    @Test
    fun reusableFixturesHaveStableIdsAndCoherentScopes() {
        assertEquals(
            AiStoryTestFixtures.CHARACTER_ID,
            AiStoryTestFixtures.characterProfile().id
        )
        assertEquals(
            AiStoryTestFixtures.CHARACTER_ID,
            AiStoryTestFixtures.conversation().characterId
        )
        assertEquals(
            AiStoryTestFixtures.CONVERSATION_ID,
            AiStoryTestFixtures.conversationMemoryFact().conversationId
        )
        assertEquals(
            AiStoryTestFixtures.NOVEL_PROJECT_ID,
            AiStoryTestFixtures.novelChapter().projectId
        )
        assertEquals(
            AiStoryTestFixtures.NOVEL_CHAPTER_ID,
            AiStoryTestFixtures.novelScene().chapterId
        )
        assertEquals(
            MemoryFactEntity.OWNER_NOVEL_PROJECT,
            AiStoryTestFixtures.novelMemoryFact().ownerType
        )
    }

    @Test
    fun aiFixturesContainUnicodeRequestAndTerminalResponse() {
        assertNotNull(AiStoryTestFixtures.aiRequest().messages)
        assertEquals("fixture-provider", AiStoryTestFixtures.providerConfiguration().providerId)
        assertEquals("Bầu trời hôm nay trong xanh.", (AiStoryTestFixtures.aiResponseEvents().first() as AiStreamEvent.TextDelta).text)
        assertEquals(AiStreamEvent.Completed, AiStoryTestFixtures.aiResponseEvents().last())
    }
}
