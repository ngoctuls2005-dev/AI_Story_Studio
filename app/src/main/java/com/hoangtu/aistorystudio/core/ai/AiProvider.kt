package com.hoangtu.aistorystudio.core.ai

import kotlinx.coroutines.flow.Flow

data class AiMessage(
    val role: AiMessageRole,
    val content: String
)

enum class AiMessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

data class AiRequest(
    val messages: List<AiMessage>
)

sealed interface AiStreamEvent {
    data class TextDelta(val text: String) : AiStreamEvent
    data object Completed : AiStreamEvent
    data class Failed(val error: AiProviderError) : AiStreamEvent
}

sealed interface AiProviderError {
    data object Unauthorized : AiProviderError
    data object RateLimited : AiProviderError
    data object ServerError : AiProviderError
    data object Timeout : AiProviderError
    data object Interrupted : AiProviderError
    data object EmptyResponse : AiProviderError
}

/** Phat mot su kien terminal; cancellation duoc giu nguyen, khong chuyen thanh loi. */
interface AiProvider {
    fun stream(request: AiRequest): Flow<AiStreamEvent>
}
