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

class YahooFinanceProviderTest {
    @Test
    fun `parses yahoo chart response into quote`() = runTest {
        val result = fetch(
            Indicator.USD_KRW,
            MockResponse().setBody(
                """
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "regularMarketPrice": 1382.4,
                        "previousClose": 1380.3,
                        "regularMarketTime": 1725000000
                      }
                    }],
                    "error": null
                  }
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(1382.4, quote.value, 0.001)
        assertEquals(2.1, quote.change, 0.001)
    }

    @Test
    fun `malformed payload is a non-retryable failure`() = runTest {
        val result = fetch(Indicator.USD_KRW, MockResponse().setBody("{not-json"))

        assertFailure(result, retryable = false)
    }

    @Test
    fun `empty response body is a non-retryable failure`() = runTest {
        val result = fetch(Indicator.USD_KRW, MockResponse().setBody(""))

        assertFailure(result, retryable = false)
    }

    @Test
    fun `empty chart is a non-retryable failure`() = runTest {
        val result = fetch(Indicator.USD_KRW, MockResponse().setBody("""{"chart":{"result":[],"error":null}}"""))

        assertFailure(result, retryable = false, reason = "empty-chart")
    }

    @Test
    fun `rate limit response is retryable`() = runTest {
        val result = fetch(Indicator.USD_KRW, MockResponse().setResponseCode(429))

        assertFailure(result, retryable = true, reason = "http-429")
    }

    @Test
    fun `permanent HTTP response is not retryable`() = runTest {
        val result = fetch(Indicator.USD_KRW, MockResponse().setResponseCode(404))

        assertFailure(result, retryable = false, reason = "http-404")
    }

    @Test
    fun `network IO failure is retryable`() = runTest {
        val server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString()
        server.shutdown()

        val result = YahooFinanceProvider(baseUrl = baseUrl).fetch(Indicator.USD_KRW)

        assertFailure(result, retryable = true)
    }

    @Test
    fun `supports vix and bitcoin yahoo symbols`() = runTest {
        val btcResult = fetch(
            Indicator.BTC_USD,
            MockResponse().setBody(
                """
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "regularMarketPrice": 67890.0,
                        "previousClose": 67000.0,
                        "regularMarketTime": 1725000000
                      }
                    }],
                    "error": null
                  }
                }
                """.trimIndent(),
            ),
            MockResponse().setBody(rsiChartBody(closeValues = (1..15).map { it.toDouble() })),
        )

        assertTrue(btcResult is ProviderResult.Success)
        assertEquals(67890.0, (btcResult as ProviderResult.Success).quote.value, 0.001)
    }

    @Test
    fun `calculates rsi for supported yahoo indicators`() = runTest {
        val result = fetch(
            Indicator.SP500,
            MockResponse().setBody(
                """
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "regularMarketPrice": 5500.0,
                        "previousClose": 5450.0,
                        "regularMarketTime": 1725000000
                      }
                    }],
                    "error": null
                  }
                }
                """.trimIndent(),
            ),
            MockResponse().setBody(rsiChartBody(closeValues = (1..15).map { it.toDouble() })),
        )

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(100.0, quote.rsi ?: -1.0, 0.001)
    }

    @Test
    fun `omits rsi for unsupported yahoo indicators`() = runTest {
        val result = fetch(
            Indicator.USD_KRW,
            MockResponse().setBody(
                """
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "regularMarketPrice": 1382.4,
                        "previousClose": 1380.3,
                        "regularMarketTime": 1725000000
                      }
                    }],
                    "error": null
                  }
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Success)
        assertEquals(null, (result as ProviderResult.Success).quote.rsi)
    }

    private fun rsiChartBody(closeValues: List<Double>): String =
        """
        {
          "chart": {
            "result": [{
              "meta": {
                "regularMarketPrice": ${closeValues.last()},
                "previousClose": ${closeValues.dropLast(1).last()},
                "regularMarketTime": 1725000000
              },
              "timestamp": [${closeValues.indices.joinToString { (1723000000 + it * 86400).toString() }}],
              "indicators": {
                "quote": [{
                  "close": [${closeValues.joinToString()}]
                }]
              }
            }],
            "error": null
          }
        }
        """.trimIndent()

    private suspend fun fetch(indicator: Indicator, vararg responses: MockResponse): ProviderResult {
        val server = MockWebServer()
        responses.forEach { server.enqueue(it) }
        server.start()
        return try {
            YahooFinanceProvider(baseUrl = server.url("/").toString()).fetch(indicator)
        } finally {
            server.shutdown()
        }
    }

    private suspend fun fetch(indicator: Indicator, response: MockResponse): ProviderResult {
        return fetch(indicator, *arrayOf(response))
    }

    private fun assertFailure(result: ProviderResult, retryable: Boolean, reason: String? = null) {
        assertTrue(result is ProviderResult.Failure)
        result as ProviderResult.Failure
        assertEquals(retryable, result.retryable)
        reason?.let { assertEquals(it, result.reason) }
        if (!retryable) assertFalse(result.retryable)
    }
}
