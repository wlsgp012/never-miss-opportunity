package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CnnFearGreedProviderTest {
    @Test
    fun `parses cnn fear greed score`() = runTest {
        val server = MockWebServer()
        server.enqueue(
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
        server.start()

        val provider = CnnFearGreedProvider(baseUrl = server.url("/").toString())
        val result = provider.fetch(Indicator.CNN_FEAR_GREED)

        assertTrue(result is ProviderResult.Success)
        val quote = (result as ProviderResult.Success).quote
        assertEquals(44.0, quote.value, 0.001)
        assertEquals(-3.0, quote.change, 0.001)
        server.shutdown()
    }
}
