package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CnnFearGreedProviderTest {
    @Test
    fun `parses cnn fear greed score`() = runTest {
        val result = fetch(
            MockResponse().setBody(
                """
                {
                  "fear_and_greed": {
                    "score": 44.0,
                    "previous_close": 47.0,
                    "timestamp": "2026-07-14T13:00:00Z"
                  }
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(44.0, quote.value, 0.001)
        assertEquals(-3.0, quote.change, 0.001)
    }

    @Test
    fun `empty payload is a non-retryable failure`() = runTest {
        val result = fetch(MockResponse().setBody("{}"))

        assertFailure(result, retryable = false)
    }

    @Test
    fun `empty response body is a non-retryable failure`() = runTest {
        val result = fetch(MockResponse().setBody(""))

        assertFailure(result, retryable = false)
    }

    @Test
    fun `server error is retryable`() = runTest {
        val result = fetch(MockResponse().setResponseCode(503))

        assertFailure(result, retryable = true, reason = "http-503")
    }

    @Test
    fun `permanent HTTP response is not retryable`() = runTest {
        val result = fetch(MockResponse().setResponseCode(404))

        assertFailure(result, retryable = false, reason = "http-404")
    }

    private suspend fun fetch(response: MockResponse): ProviderResult {
        val server = MockWebServer()
        server.enqueue(response)
        server.start()
        return try {
            CnnFearGreedProvider(baseUrl = server.url("/").toString()).fetch(Indicator.CNN_FEAR_GREED)
        } finally {
            server.shutdown()
        }
    }

    private fun assertFailure(result: ProviderResult, retryable: Boolean, reason: String? = null) {
        assertTrue(result is ProviderResult.Failure)
        result as ProviderResult.Failure
        assertEquals(retryable, result.retryable)
        reason?.let { assertEquals(it, result.reason) }
        if (!retryable) assertFalse(result.retryable)
    }
}
