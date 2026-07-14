package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YahooFinanceProviderTest {
    @Test
    fun `parses yahoo chart response into quote`() = runTest {
        val server = MockWebServer()
        server.enqueue(
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
        server.start()

        val provider = YahooFinanceProvider(baseUrl = server.url("/").toString())
        val result = provider.fetch(Indicator.USD_KRW)

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(1382.4, quote.value, 0.001)
        assertEquals(2.1, quote.change, 0.001)
        server.shutdown()
    }
}
