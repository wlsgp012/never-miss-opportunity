package com.nevermiss.opportunity.domain

enum class QuoteState {
    LOADING,
    FRESH,
    STALE,
    UNAVAILABLE,
}

data class MarketQuote(
    val indicator: Indicator,
    val value: Double,
    val change: Double,
    val percentChange: Double?,
    val observedAtEpochMillis: Long,
    val source: String,
    val state: QuoteState,
)
