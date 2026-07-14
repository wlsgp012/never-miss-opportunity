package com.nevermiss.opportunity.worker

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.data.RefreshResult
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketFetchWorkerTest {
    @Test
    fun `notification should post only when refresh has successes`() {
        val quote = MarketQuote(Indicator.USD_KRW, 1382.4, 2.1, 0.15, 1_725_000_000_000, "Test", QuoteState.FRESH)
        val success = RefreshResult("0900", listOf(quote), emptyList())
        val failure = RefreshResult("0900", emptyList(), listOf(ProviderResult.Failure(Indicator.KOSPI, "network", true)))

        assertTrue(MarketFetchWorker.shouldPostNotification(success))
        assertFalse(MarketFetchWorker.shouldPostNotification(failure))
    }
}
