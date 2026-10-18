package com.hoangtu.aistorystudio.core.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.FlowCollector

sealed interface FakeAiScenario {
    data class Success(
        val chunks: List<String> = listOf(
            "Chào bạn! ",
            "Mình có thể giúp bạn phát triển câu chuyện."
        ),
        val interChunkDelayMillis: Long = 0L
    ) : FakeAiScenario {
        init {
            require(interChunkDelayMillis >= 0L)
        }
    }

    data object Unauthorized : FakeAiScenario
    data object RateLimited : FakeAiScenario
    data object ServerError : FakeAiScenario
    data class Timeout(val delayMillis: Long = 1_000L) : FakeAiScenario {
        init {
            require(delayMillis >= 0L)
        }
    }

    data class Interrupted(
        val partialChunks: List<String> = listOf("Phản hồi bị gián đoạn"),
        val interChunkDelayMillis: Long = 0L
    ) : FakeAiScenario {
        init {
            require(interChunkDelayMillis >= 0L)
        }
    }

    data object EmptyResponse : FakeAiScenario
}

class FakeAiProvider(
    private val defaultScenario: FakeAiScenario = FakeAiScenario.Success(),
    scenariosByRequest: Map<AiRequest, FakeAiScenario> = emptyMap()
) : AiProvider {
    private val requestScenarios = scenariosByRequest.toMap()

    override fun stream(request: AiRequest): Flow<AiStreamEvent> = flow {
        when (val scenario = requestScenarios[request] ?: defaultScenario) {
            is FakeAiScenario.Success -> {
                val chunks = scenario.chunks.filter(String::isNotEmpty)
                if (chunks.isEmpty()) {
                    emit(AiStreamEvent.Failed(AiProviderError.EmptyResponse))
                    return@flow
                }

                emitChunks(chunks, scenario.interChunkDelayMillis)
                emit(AiStreamEvent.Completed)
            }

            FakeAiScenario.Unauthorized -> emit(
                AiStreamEvent.Failed(AiProviderError.Unauthorized)
            )

            FakeAiScenario.RateLimited -> emit(
                AiStreamEvent.Failed(AiProviderError.RateLimited)
            )

            FakeAiScenario.ServerError -> emit(
                AiStreamEvent.Failed(AiProviderError.ServerError)
            )

            is FakeAiScenario.Timeout -> {
                delay(scenario.delayMillis)
                emit(AiStreamEvent.Failed(AiProviderError.Timeout))
            }

            is FakeAiScenario.Interrupted -> {
                emitChunks(scenario.partialChunks, scenario.interChunkDelayMillis)
                emit(AiStreamEvent.Failed(AiProviderError.Interrupted))
            }

            FakeAiScenario.EmptyResponse -> emit(
                AiStreamEvent.Failed(AiProviderError.EmptyResponse)
            )
        }
    }

    private suspend fun FlowCollector<AiStreamEvent>.emitChunks(
        chunks: List<String>,
        interChunkDelayMillis: Long
    ) {
        chunks.filter(String::isNotEmpty).forEachIndexed { index, chunk ->
            if (index > 0 && interChunkDelayMillis > 0L) {
                delay(interChunkDelayMillis)
            }
            emit(AiStreamEvent.TextDelta(chunk))
        }
    }
}
