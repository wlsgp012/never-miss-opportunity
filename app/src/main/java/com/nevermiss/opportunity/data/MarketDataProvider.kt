package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote

fun interface MarketDataProvider {
    suspend fun fetch(indicator: Indicator): ProviderResult
}

sealed interface ProviderResult {
    data class Success(val quote: MarketQuote) : ProviderResult
    data class Failure(val indicator: Indicator, val reason: String, val retryable: Boolean) : ProviderResult
}
