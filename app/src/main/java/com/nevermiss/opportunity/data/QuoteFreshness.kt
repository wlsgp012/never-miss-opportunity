package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState

object QuoteFreshness {
    private const val STALE_AFTER_MILLIS = 90 * 60 * 1000L

    fun stateFor(quote: MarketQuote?, nowEpochMillis: Long): QuoteState {
        if (quote == null) return QuoteState.UNAVAILABLE
        val ageMillis = nowEpochMillis - quote.observedAtEpochMillis
        return if (ageMillis > STALE_AFTER_MILLIS) QuoteState.STALE else QuoteState.FRESH
    }
}
