package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.data.local.FetchLogEntity
import com.nevermiss.opportunity.data.local.MarketDao
import com.nevermiss.opportunity.data.local.QuoteEntity
import com.nevermiss.opportunity.data.local.toDomain
import com.nevermiss.opportunity.data.local.toEntity
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

class DefaultMarketRepository(
    private val dao: MarketDao,
    private val provider: MarketDataProvider,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : MarketRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLatestQuotes(): Flow<Map<Indicator, MarketQuote>> =
        dao.observeLatestQuotes().transformLatest { rows ->
            while (true) {
                emit(rows.toLatestQuoteMap())
                val nextStateChangeMillis = rows
                    .map { it.toDomain() }
                    .mapNotNull { QuoteFreshness.millisUntilStateChange(it, clockMillis()) }
                    .minOrNull()
                    ?: break
                delay(nextStateChangeMillis)
            }
        }

    override suspend fun latestQuotes(): Map<Indicator, MarketQuote> =
        dao.latestQuotes().toLatestQuoteMap()

    private fun List<QuoteEntity>.toLatestQuoteMap(): Map<Indicator, MarketQuote> =
        associate {
            val quote = it.toDomain()
            it.indicator to quote.copy(state = QuoteFreshness.stateFor(quote, clockMillis()))
        }

    override suspend fun refreshIndicators(indicators: List<Indicator>, slotId: String): RefreshResult {
        val startedAt = clockMillis()
        val successes = mutableListOf<MarketQuote>()
        val failures = mutableListOf<ProviderResult.Failure>()
        val quotesBeforeRefresh = latestQuotes()

        indicators.forEach { indicator ->
            when (val result = provider.fetch(indicator)) {
                is ProviderResult.Success -> {
                    val quote = result.quote.copy(
                        state = QuoteFreshness.stateFor(result.quote, clockMillis()),
                    )
                    dao.insertQuote(quote.toEntity())
                    if (quote.state == QuoteState.FRESH) successes += quote
                }
                is ProviderResult.Failure -> {
                    failures += result
                    quotesBeforeRefresh[indicator]
                        ?.copy(state = QuoteState.STALE)
                        ?.let { dao.insertQuote(it.toEntity()) }
                }
            }
        }

        dao.insertFetchLog(
            FetchLogEntity(
                slotId = slotId,
                startedAtEpochMillis = startedAt,
                completedAtEpochMillis = clockMillis(),
                successCount = successes.size,
                failureCount = failures.size,
                errorSummary = failures.joinToString { "${it.indicator.name}:${it.reason}" },
            ),
        )

        return RefreshResult(slotId, successes, failures)
    }
}
