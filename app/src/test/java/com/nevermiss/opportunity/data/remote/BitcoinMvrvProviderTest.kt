package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BitcoinMvrvProviderTest {
    @Test
    fun `parses latest public coin metrics mvrv value`() = runTest {
        val (result, request) = fetchWithRequest(
            MockResponse().setBody(
                """
                {
                  "data": [
                    {
                      "time": "2026-07-13T00:00:00.000000000Z",
                      "CapMVRVCur": "1.2028"
                    },
                    {
                      "time": "2026-07-14T00:00:00.000000000Z",
                      "CapMVRVCur": "1.1743"
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(Indicator.BTC_MVRV, quote.indicator)
        assertEquals(1.1743, quote.value, 0.001)
        assertEquals(-0.0285, quote.change, 0.001)
        assertEquals(-2.369, quote.percentChange ?: -1.0, 0.001)
        assertEquals("CapMVRVCur", request.requestUrl?.queryParameter("metrics"))
    }

    @Test
    fun `missing mvrv metric is a non retryable failure`() = runTest {
        val result = fetch(
            MockResponse().setBody(
                """
                {
                  "data": [
                    {
                      "time": "2026-07-14T00:00:00.000000000Z",
                      "CapMrktCurUSD": "2000000000000",
                      "CapRealUSD": "1000000000000"
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertTrue(result is ProviderResult.Failure)
        assertEquals(false, (result as ProviderResult.Failure).retryable)
    }

    private suspend fun fetch(response: MockResponse): ProviderResult {
        return fetchWithRequest(response).first
    }

    private suspend fun fetchWithRequest(response: MockResponse): Pair<ProviderResult, RecordedRequest> {
        val server = MockWebServer()
        server.enqueue(response)
        server.start()
        return try {
            val result = BitcoinMvrvProvider(baseUrl = server.url("/").toString()).fetch(Indicator.BTC_MVRV)
            result to server.takeRequest()
        } finally {
            server.shutdown()
        }
    }
}
