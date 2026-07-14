package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteFreshnessTest {
    @Test
    fun `quote becomes stale after ninety minutes`() {
        val quote = MarketQuote(
            indicator = Indicator.KOSPI,
            value = 2860.0,
            change = 1.0,
            percentChange = 0.03,
            observedAtEpochMillis = 1_000,
            source = "Test",
            state = QuoteState.FRESH,
        )

        assertEquals(QuoteState.FRESH, QuoteFreshness.stateFor(quote, nowEpochMillis = 1_000 + 89 * 60 * 1000))
        assertEquals(QuoteState.STALE, QuoteFreshness.stateFor(quote, nowEpochMillis = 1_000 + 91 * 60 * 1000))
    }

    @Test
    fun `missing quote is unavailable`() {
        assertEquals(QuoteState.UNAVAILABLE, QuoteFreshness.stateFor(null, nowEpochMillis = 1_000))
    }
}
