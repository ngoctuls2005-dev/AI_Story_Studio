package com.hoangtu.aistorystudio.core.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAiProviderTest {
    @Test
    fun successJoinsUnicodeChunksAndCompletesExactlyOnce() = runTest {
        val request = request("Xin chào")
        val chunks = listOf("Xin chào, ", "bạn khỏe không? ", "🌿")
        val provider = FakeAiProvider(
            defaultScenario = FakeAiScenario.Success(chunks = chunks)
        )

        val events = provider.stream(request).toList()
        val receivedText = events
            .filterIsInstance<AiStreamEvent.TextDelta>()
            .joinToString(separator = "") { it.text }

        assertEquals("Xin chào, bạn khỏe không? 🌿", receivedText)
        assertEquals(1, events.count { it == AiStreamEvent.Completed })
        assertEquals(0, events.count { it is AiStreamEvent.Failed })
    }

    @Test
    fun statusAndEmptyResponseScenariosEmitTypedFailures() = runTest {
        val scenarios = listOf(
            Triple("unauthorized", FakeAiScenario.Unauthorized, AiProviderError.Unauthorized),
            Triple("rate-limited", FakeAiScenario.RateLimited, AiProviderError.RateLimited),
            Triple("server-error", FakeAiScenario.ServerError, AiProviderError.ServerError),
            Triple("empty", FakeAiScenario.EmptyResponse, AiProviderError.EmptyResponse)
        )
        val requests = scenarios.associate { (key, _, _) -> key to request(key) }
        val provider = FakeAiProvider(
            scenariosByRequest = scenarios.associate { (key, scenario, _) ->
                requests.getValue(key) to scenario
            }
        )

        scenarios.forEach { (key, _, expectedError) ->
            val events = provider.stream(requests.getValue(key)).toList()
            assertEquals(listOf(AiStreamEvent.Failed(expectedError)), events)
        }
    }

    @Test
    fun emptySuccessChunksAreReportedAsEmptyResponse() = runTest {
        val provider = FakeAiProvider(
            defaultScenario = FakeAiScenario.Success(chunks = listOf("", ""))
        )

        assertEquals(
            listOf(AiStreamEvent.Failed(AiProviderError.EmptyResponse)),
            provider.stream(request()).toList()
        )
    }

    @Test
    fun timeoutUsesVirtualTimeAndTerminates() = runTest {
        val provider = FakeAiProvider(
            defaultScenario = FakeAiScenario.Timeout(delayMillis = 250L)
        )

        val events = provider.stream(request()).toList()

        assertEquals(250L, testScheduler.currentTime)
        assertEquals(listOf(AiStreamEvent.Failed(AiProviderError.Timeout)), events)
    }

    @Test
    fun interruptedStreamKeepsPartialTextAndDoesNotComplete() = runTest {
        val provider = FakeAiProvider(
            defaultScenario = FakeAiScenario.Interrupted(
                partialChunks = listOf("Đoạn đầu ", "còn dang dở")
            )
        )

        val events = provider.stream(request()).toList()
        val receivedText = events
            .filterIsInstance<AiStreamEvent.TextDelta>()
            .joinToString(separator = "") { it.text }

        assertEquals("Đoạn đầu còn dang dở", receivedText)
        assertEquals(1, events.count { it == AiStreamEvent.Failed(AiProviderError.Interrupted) })
        assertEquals(0, events.count { it == AiStreamEvent.Completed })
    }

    @Test
    fun chunkDelayIsControlledByTestScheduler() = runTest {
        val provider = FakeAiProvider(
            defaultScenario = FakeAiScenario.Success(
                chunks = listOf("một", "hai"),
                interChunkDelayMillis = 50L
            )
        )
        val events = mutableListOf<AiStreamEvent>()
        val collection = launch { provider.stream(request()).collect(events::add) }

        runCurrent()
        assertEquals(listOf(AiStreamEvent.TextDelta("một")), events)
        assertEquals(0L, testScheduler.currentTime)

        advanceTimeBy(49L)
        runCurrent()
        assertEquals(1, events.filterIsInstance<AiStreamEvent.TextDelta>().size)

        advanceTimeBy(1L)
        runCurrent()
        collection.join()

        assertEquals(
            listOf(
                AiStreamEvent.TextDelta("một"),
                AiStreamEvent.TextDelta("hai"),
                AiStreamEvent.Completed
            ),
            events
        )
        assertEquals(50L, testScheduler.currentTime)
    }

    @Test
    fun cancellationStopsStreamingAndLeavesNoActiveCollection() = runTest {
        val provider = FakeAiProvider(
            defaultScenario = FakeAiScenario.Success(
                chunks = listOf("đã nhận", "không được nhận"),
                interChunkDelayMillis = 100L
            )
        )
        val events = mutableListOf<AiStreamEvent>()
        var completionCause: Throwable? = null
        val collection = launch {
            provider.stream(request())
                .onCompletion { completionCause = it }
                .collect(events::add)
        }

        runCurrent()
        assertEquals(listOf(AiStreamEvent.TextDelta("đã nhận")), events)

        collection.cancelAndJoin()
        advanceUntilIdle()

        assertFalse(collection.isActive)
        assertTrue(collection.isCancelled)
        assertTrue(completionCause is CancellationException)
        assertEquals(listOf(AiStreamEvent.TextDelta("đã nhận")), events)
    }

    @Test
    fun concurrentRequestsUseTheirOwnDeterministicScenarios() = runTest {
        val firstRequest = request("yêu cầu thứ nhất")
        val secondRequest = request("yêu cầu thứ hai")
        val provider = FakeAiProvider(
            scenariosByRequest = mapOf(
                firstRequest to FakeAiScenario.Success(
                    chunks = listOf("A1", "A2"),
                    interChunkDelayMillis = 20L
                ),
                secondRequest to FakeAiScenario.Success(
                    chunks = listOf("B1", "B2"),
                    interChunkDelayMillis = 10L
                )
            )
        )
        val firstResult = async { collectText(provider, firstRequest) }
        val secondResult = async { collectText(provider, secondRequest) }

        advanceUntilIdle()

        assertEquals("A1A2", firstResult.await())
        assertEquals("B1B2", secondResult.await())
    }

    private suspend fun collectText(provider: AiProvider, request: AiRequest): String =
        provider.stream(request)
            .filterIsInstance<AiStreamEvent.TextDelta>()
            .toList()
            .joinToString(separator = "") { it.text }

    private fun request(userContent: String = "Xin chào"): AiRequest = AiRequest(
        messages = listOf(
            AiMessage(AiMessageRole.SYSTEM, "Hãy hỗ trợ sáng tác."),
            AiMessage(AiMessageRole.USER, userContent)
        )
    )
}
