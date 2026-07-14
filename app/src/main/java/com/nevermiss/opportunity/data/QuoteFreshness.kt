package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState

object QuoteFreshness {
    private const val STALE_AFTER_MILLIS = 90 * 60 * 1000L

    fun stateFor(quote: MarketQuote?, nowEpochMillis: Long): QuoteState {
        if (quote == null) return QuoteState.UNAVAILABLE
        // A persisted stale state records a failed refresh; time-derived freshness must not erase it.
        if (quote.state == QuoteState.STALE) return QuoteState.STALE
        val ageMillis = nowEpochMillis - quote.observedAtEpochMillis
        return if (ageMillis > STALE_AFTER_MILLIS) QuoteState.STALE else QuoteState.FRESH
    }

    fun millisUntilStateChange(quote: MarketQuote, nowEpochMillis: Long): Long? {
        if (stateFor(quote, nowEpochMillis) != QuoteState.FRESH) return null
        return (quote.observedAtEpochMillis + STALE_AFTER_MILLIS + 1 - nowEpochMillis).coerceAtLeast(1)
    }
}
