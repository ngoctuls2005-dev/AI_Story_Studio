package com.hoangtu.aistorystudio.testutil

import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class MockWebServerHarnessTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
        server.close()
    }

    @Test
    fun httpStatusAndEmptyBodyFixturesAreServedLocally() {
        val fixtures = listOf(
            MockWebServerFixtures.success() to 200,
            MockWebServerFixtures.badRequest() to 400,
            MockWebServerFixtures.unauthorized() to 401,
            MockWebServerFixtures.rateLimited() to 429,
            MockWebServerFixtures.serverError() to 500,
            MockWebServerFixtures.emptyResponse() to 200
        )

        fixtures.forEachIndexed { index, (fixture, expectedCode) ->
            server.enqueue(fixture)
            client.newCall(request("/fixture/$index")).execute().use { response ->
                assertEquals(expectedCode, response.code)
                if (index == fixtures.lastIndex) {
                    assertEquals("", response.body.string())
                }
            }
        }

        assertEquals(fixtures.size, server.requestCount)
    }

    @Test
    fun delayedResponseFixtureReturnsItsBody() {
        server.enqueue(MockWebServerFixtures.delayedResponse())

        client.newCall(request("/delayed")).execute().use { response ->
            assertEquals(200, response.code)
            assertTrue(response.body.string().contains("dữ liệu giả"))
        }

        assertEquals("/delayed", server.takeRequest().target)
    }

    @Test
    fun timeoutFixtureDoesNotLeaveTheCallRunning() {
        server.enqueue(MockWebServerFixtures.timeoutResponse())
        val timedClient = client.newBuilder().callTimeout(250, TimeUnit.MILLISECONDS).build()
        var timeoutError: IOException? = null
        val elapsed = measureTimeMillis {
            try {
                timedClient.newCall(request("/timeout")).execute().use { response ->
                    response.body.string()
                }
            } catch (error: IOException) {
                timeoutError = error
            }
        }

        assertTrue(timeoutError is InterruptedIOException)
        assertTrue(elapsed < 2_500L)
    }

    @Test
    fun connectionCanCloseDuringResponseBody() {
        server.enqueue(MockWebServerFixtures.disconnectDuringResponse())

        var disconnected = false
        try {
            client.newCall(request("/disconnect")).execute().use { response ->
                response.body.bytes()
            }
        } catch (_: IOException) {
            disconnected = true
        }

        assertTrue(disconnected)
    }

    @Test
    fun unicodeAndSseFixturesPreserveTheirRawText() {
        server.enqueue(MockWebServerFixtures.utf8Response())
        client.newCall(request("/unicode")).execute().use { response ->
            assertEquals(MockWebServerFixtures.UTF8_BODY, response.body.string())
        }

        server.enqueue(MockWebServerFixtures.sseResponse())
        client.newCall(request("/sse")).execute().use { response ->
            assertTrue(response.header("Content-Type").orEmpty().startsWith("text/event-stream"))
            assertEquals(MockWebServerFixtures.SSE_BODY, response.body.string())
        }
    }

    private fun request(path: String) = Request.Builder()
        .url(server.url(path))
        .get()
        .build()
}
