package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldSpotProviderTest {
    @Test
    fun `parses Naver KRX gold product detail response into quote`() = runTest {
        val result = fetch(
            MockResponse().setBody(
                """
                {
                  "isSuccess": true,
                  "result": {
                    "stockExchangeType": {"code": "KRX", "nameKor": "한국거래소"},
                    "name": "국내 금",
                    "localTradedAt": "2026-07-21T13:49:51+09:00",
                    "closePrice": "191,110",
                    "fluctuations": "1,110",
                    "fluctuationsRatio": "0.58",
                    "fluctuationsType": {"code": "2", "text": "상승", "name": "RISING"},
                    "unit": "원/g"
                  }
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(191110.0, quote.value, 0.001)
        assertEquals(1110.0, quote.change, 0.001)
        assertEquals(0.58, quote.percentChange ?: -1.0, 0.001)
        assertEquals("Naver Finance (KRX)", quote.source)
    }

    @Test
    fun `parses KRX gold response into quote`() = runTest {
        val result = fetch(
            MockResponse().setBody(
                """
                {
                  "TDD_CLSPRC": "197,490",
                  "CMPPREVDD_PRC": "1,230",
                  "FLUC_RT": "0.63",
                  "FLUC_TP_CD": "2"
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(Indicator.KRX_GOLD_1G, quote.indicator)
        assertEquals(197490.0, quote.value, 0.001)
        assertEquals(1230.0, quote.change, 0.001)
        assertEquals(0.63, quote.percentChange ?: -1.0, 0.001)
    }

    @Test
    fun `parses KRX downward change as negative`() = runTest {
        val result = fetch(
            MockResponse().setBody(
                """
                {
                  "TDD_CLSPRC": "196,260",
                  "CMPPREVDD_PRC": "1,230",
                  "FLUC_RT": "-0.62",
                  "FLUC_TP_CD": "4"
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(-1230.0, quote.change, 0.001)
        assertEquals(-0.62, quote.percentChange ?: 1.0, 0.001)
    }

    @Test
    fun `LOGOUT response is retryable failure`() = runTest {
        val result = fetch(MockResponse().setBody("LOGOUT"))

        assertTrue(result is ProviderResult.Failure)
        result as ProviderResult.Failure
        assertEquals("krx-logout", result.reason)
        assertEquals(true, result.retryable)
    }

    @Test
    fun `rate limit response is retryable failure`() = runTest {
        val result = fetch(MockResponse().setResponseCode(429))

        assertTrue(result is ProviderResult.Failure)
        result as ProviderResult.Failure
        assertEquals("http-429", result.reason)
        assertEquals(true, result.retryable)
    }

    @Test
    fun `unsupported indicator fails without remote call`() = runTest {
        val server = MockWebServer()
        server.start()
        val provider = GoldSpotProvider(baseUrl = server.url("/").toString())

        val result = provider.fetch(Indicator.KOSPI)

        assertTrue(result is ProviderResult.Failure)
        assertEquals(0, server.requestCount)
        server.shutdown()
    }

    private suspend fun fetch(response: MockResponse): ProviderResult {
        val server = MockWebServer()
        server.enqueue(response)
        server.start()
        return try {
            GoldSpotProvider(
                baseUrl = server.url("/").toString(),
                clockMillis = { 1_725_000_000_000 },
            ).fetch(Indicator.KRX_GOLD_1G)
        } finally {
            server.shutdown()
        }
    }
}
