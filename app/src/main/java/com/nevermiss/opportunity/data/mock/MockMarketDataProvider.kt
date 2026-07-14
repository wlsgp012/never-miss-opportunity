package com.nevermiss.opportunity.data.mock

import com.nevermiss.opportunity.data.MarketDataProvider
import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState

class MockMarketDataProvider(
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : MarketDataProvider {
    override suspend fun fetch(indicator: Indicator): ProviderResult.Success {
        val base = when (indicator) {
            Indicator.USD_KRW -> 1382.4
            Indicator.CNN_FEAR_GREED -> 44.0
            Indicator.SP500 -> 5630.0
            Indicator.NASDAQ100 -> 19880.0
            Indicator.VIX -> 15.5
            Indicator.KRX_GOLD_1G -> 103500.0
            Indicator.KOSPI -> 2860.0
            Indicator.BTC_USD -> 67890.0
            Indicator.BTC_MVRV -> 2.1
        }
        val change = when (indicator.ordinal % 3) {
            0 -> 2.1
            1 -> -3.0
            else -> 0.0
        }
        return ProviderResult.Success(
            MarketQuote(
                indicator = indicator,
                value = base,
                change = change,
                percentChange = if (base == 0.0) null else change / base * 100.0,
                observedAtEpochMillis = clockMillis(),
                source = "Mock fallback",
                state = QuoteState.FRESH,
            ),
        )
    }
}
