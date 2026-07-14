package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.MarketDataProvider
import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.data.mock.MockMarketDataProvider
import com.nevermiss.opportunity.domain.Indicator

class GoldSpotProvider(
    private val fallback: MarketDataProvider = MockMarketDataProvider(),
) : MarketDataProvider {
    override suspend fun fetch(indicator: Indicator): ProviderResult {
        if (indicator != Indicator.KRX_GOLD_1G) {
            return ProviderResult.Failure(indicator, "unsupported-indicator", retryable = false)
        }
        return fallback.fetch(indicator)
    }
}
