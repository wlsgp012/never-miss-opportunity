package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMarketRepositoryTest {
    @Test
    fun `partial provider failure preserves successes`() = runTest {
        val provider = MarketDataProvider { indicator ->
            if (indicator == Indicator.USD_KRW) {
                ProviderResult.Success(
                    MarketQuote(indicator, 1382.4, 2.1, 0.15, 1_725_000_000_000, "Test", QuoteState.FRESH),
                )
            } else {
                ProviderResult.Failure(indicator, "network", retryable = true)
            }
        }
        val repository = InMemoryMarketRepository(provider)

        val result = repository.refreshIndicators(listOf(Indicator.USD_KRW, Indicator.KOSPI), "0900")

        assertEquals(1, result.successes.size)
        assertEquals(1, result.failures.size)
        assertTrue(repository.latestQuotes().containsKey(Indicator.USD_KRW))
    }
}

private class InMemoryMarketRepository(
    private val provider: MarketDataProvider,
) : MarketRepository {
    private val quotes = linkedMapOf<Indicator, MarketQuote>()

    override fun observeLatestQuotes() = kotlinx.coroutines.flow.flowOf(quotes)

    override suspend fun latestQuotes(): Map<Indicator, MarketQuote> = quotes

    override suspend fun refreshIndicators(indicators: List<Indicator>, slotId: String): RefreshResult {
        val successes = mutableListOf<MarketQuote>()
        val failures = mutableListOf<ProviderResult.Failure>()
        for (indicator in indicators) {
            when (val result = provider.fetch(indicator)) {
                is ProviderResult.Success -> {
                    quotes[indicator] = result.quote
                    successes += result.quote
                }
                is ProviderResult.Failure -> failures += result
            }
        }
        return RefreshResult(slotId, successes, failures)
    }
}
