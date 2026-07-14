package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import kotlinx.coroutines.flow.Flow

data class RefreshResult(
    val slotId: String,
    val successes: List<MarketQuote>,
    val failures: List<ProviderResult.Failure>,
)

interface MarketRepository {
    fun observeLatestQuotes(): Flow<Map<Indicator, MarketQuote>>
    suspend fun latestQuotes(): Map<Indicator, MarketQuote>
    suspend fun refreshIndicators(indicators: List<Indicator>, slotId: String): RefreshResult
}
