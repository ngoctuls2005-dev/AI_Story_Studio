package com.hoangtu.aistorystudio.testutil

import mockwebserver3.MockResponse
import mockwebserver3.SocketEffect
import java.util.concurrent.TimeUnit

object MockWebServerFixtures {
    const val SUCCESS_BODY = """{"id":"fixture-response-001","content":"Xin chào, dữ liệu giả."}"""
    const val UTF8_BODY = "Tiếng Việt có dấu: ấ, ắ, ệ, ớ, ữ."
    const val SSE_BODY = """data: {"choices":[{"delta":{"content":"Xin "}}]}""" +
        "\n\n" +
        """data: {"choices":[{"delta":{"content":"chào bạn."}}]}""" +
        "\n\n" +
        "data: [DONE]\n\n"

    fun success() = jsonResponse(200, SUCCESS_BODY)

    fun badRequest() = jsonResponse(400, """{"error":"fixture_bad_request"}""")

    fun unauthorized() = jsonResponse(401, """{"error":"fixture_unauthorized"}""")

    fun rateLimited() = jsonResponse(429, """{"error":"fixture_rate_limited"}""")

    fun serverError() = jsonResponse(500, """{"error":"fixture_server_error"}""")

    fun emptyResponse() = jsonResponse(200, "")

    fun delayedResponse(delayMillis: Long = 200L) = MockResponse.Builder()
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .bodyDelay(delayMillis, TimeUnit.MILLISECONDS)
        .body(SUCCESS_BODY)
        .build()

    fun timeoutResponse() = MockResponse.Builder()
        .onResponseStart(SocketEffect.Stall)
        .build()

    fun disconnectDuringResponse() = MockResponse.Builder()
        .addHeader("Content-Type", "application/octet-stream")
        .body("x".repeat(16_384))
        .onResponseBody(SocketEffect.CloseSocket())
        .build()

    fun utf8Response() = MockResponse.Builder()
        .addHeader("Content-Type", "text/plain; charset=utf-8")
        .body(UTF8_BODY)
        .build()

    fun sseResponse() = MockResponse.Builder()
        .addHeader("Content-Type", "text/event-stream; charset=utf-8")
        .chunkedBody(SSE_BODY, 7)
        .build()

    private fun jsonResponse(code: Int, body: String) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .body(body)
        .build()
}
