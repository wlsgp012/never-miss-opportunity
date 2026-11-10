package com.nevermiss.opportunity.data

import com.nevermiss.opportunity.data.local.FetchLogEntity
import com.nevermiss.opportunity.data.local.MarketDao
import com.nevermiss.opportunity.data.local.QuoteEntity
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMarketRepositoryTest {
    @Test
    fun `successful oil fetch excludes stale last traded quote from notifications`() = runTest {
        val oil = Indicator.entries.single { it.name == "WTI_CRUDE" }
        val lastTrade = quote(oil, observedAtEpochMillis = 1000)
        val repository = DefaultMarketRepository(FakeMarketDao(), providerReturning(ProviderResult.Success(lastTrade)), clockMillis = { 1000 + 3 * 24 * 60 * MINUTE })
        val result = repository.refreshIndicators(listOf(oil), "oil")
        assertTrue(result.successes.isEmpty())
        assertEquals(QuoteState.STALE, repository.latestQuotes().getValue(oil).state)
    }

    @Test
    fun `failed oil fetch does not notify using cached last trade`() = runTest {
        val oil = Indicator.entries.single { it.name == "WTI_CRUDE" }
        val dao = FakeMarketDao(listOf(quote(oil, observedAtEpochMillis = 1000).toEntity(id = 1)))
        val repository = DefaultMarketRepository(dao, providerReturning(ProviderResult.Failure(oil, "network", true)), clockMillis = { 2000 })
        val result = repository.refreshIndicators(listOf(oil), "oil")
        assertTrue(result.successes.isEmpty())
        assertEquals(1, result.failures.size)
    }

    @Test
    fun `refresh persists successful provider quotes`() = runTest {
        val quote = quote(Indicator.USD_KRW).copy(signalLabel = "Fear", rsi = 54.2)
        val dao = FakeMarketDao()
        val repository = DefaultMarketRepository(
            dao,
            providerReturning(ProviderResult.Success(quote)),
            clockMillis = { quote.observedAtEpochMillis },
        )

        val result = repository.refreshIndicators(listOf(Indicator.USD_KRW), "0900")

        assertEquals(listOf(quote), result.successes)
        assertEquals(listOf(quote), dao.insertedQuotes.map { it.toMarketQuote() })
    }

    @Test
    fun `refresh persists source-stale quote but excludes it from successes`() = runTest {
        val now = 1_725_000_000_000
        val sourceStaleQuote = quote(
            Indicator.USD_KRW,
            observedAtEpochMillis = now - 91 * MINUTE,
        )
        val dao = FakeMarketDao()
        val repository = DefaultMarketRepository(
            dao,
            providerReturning(ProviderResult.Success(sourceStaleQuote)),
            clockMillis = { now },
        )

        val result = repository.refreshIndicators(listOf(Indicator.USD_KRW), "0900")

        assertTrue(result.successes.isEmpty())
        assertEquals(QuoteState.STALE, dao.insertedQuotes.single().state)
        assertEquals(QuoteState.STALE, repository.latestQuotes().getValue(Indicator.USD_KRW).state)
    }

    @Test
    fun `refresh preserves previous quote as stale when provider fails`() = runTest {
        val existing = quote(Indicator.KOSPI, observedAtEpochMillis = 1_000)
        val dao = FakeMarketDao(listOf(existing.toEntity(id = 1)))
        val repository = DefaultMarketRepository(
            dao,
            providerReturning(ProviderResult.Failure(Indicator.KOSPI, "network", retryable = true)),
            clockMillis = { 2_000 },
        )

        repository.refreshIndicators(listOf(Indicator.KOSPI), "0900")

        assertEquals(QuoteState.STALE, dao.insertedQuotes.single().state)
        assertEquals(QuoteState.STALE, repository.latestQuotes().getValue(Indicator.KOSPI).state)
    }

    @Test
    fun `latest quotes map persisted rows with derived freshness`() = runTest {
        val dao = FakeMarketDao(listOf(quote(Indicator.USD_KRW, observedAtEpochMillis = 1_000).toEntity(id = 1)))
        val repository = DefaultMarketRepository(dao, providerReturning(), clockMillis = { 1_000 + 91 * MINUTE })

        val latest = repository.latestQuotes()

        assertEquals(1, latest.size)
        assertEquals(1382.4, latest.getValue(Indicator.USD_KRW).value, 0.001)
        assertEquals(QuoteState.STALE, latest.getValue(Indicator.USD_KRW).state)
    }

    @Test
    fun `refresh inserts fetch log with outcome counts`() = runTest {
        val dao = FakeMarketDao()
        val repository = DefaultMarketRepository(
            dao,
            MarketDataProvider { indicator ->
                if (indicator == Indicator.USD_KRW) ProviderResult.Success(quote(indicator))
                else ProviderResult.Failure(indicator, "network", retryable = true)
            },
            clockMillis = { 10_000 },
        )

        repository.refreshIndicators(listOf(Indicator.USD_KRW, Indicator.KOSPI), "0900")

        assertEquals(
            FetchLogEntity(
                slotId = "0900",
                startedAtEpochMillis = 10_000,
                completedAtEpochMillis = 10_000,
                successCount = 1,
                failureCount = 1,
                errorSummary = "KOSPI:network",
            ),
            dao.insertedFetchLogs.single().copy(id = 0),
        )
    }

    @Test
    fun `observed fresh quote becomes stale without another database write`() = runTest {
        val dao = FakeMarketDao(listOf(quote(Indicator.USD_KRW, observedAtEpochMillis = 0).toEntity(id = 1)))
        val repository = DefaultMarketRepository(
            dao,
            providerReturning(),
            clockMillis = { testScheduler.currentTime + 89 * MINUTE },
        )
        val states = mutableListOf<QuoteState>()
        val collection = launch {
            repository.observeLatestQuotes().take(2).collect {
                states += it.getValue(Indicator.USD_KRW).state
            }
        }

        runCurrent()
        assertEquals(listOf(QuoteState.FRESH), states)

        advanceTimeBy(MINUTE + 1)
        runCurrent()

        assertEquals(listOf(QuoteState.FRESH, QuoteState.STALE), states)
        assertTrue(collection.isCompleted)
    }

    @Test
    fun `observed quotes become stale at each staggered expiry without database writes`() = runTest {
        val dao = FakeMarketDao(
            listOf(
                quote(Indicator.USD_KRW, observedAtEpochMillis = 0).toEntity(id = 1),
                quote(Indicator.KOSPI, observedAtEpochMillis = MINUTE).toEntity(id = 2),
            ),
        )
        val repository = DefaultMarketRepository(
            dao,
            providerReturning(),
            clockMillis = { testScheduler.currentTime + 89 * MINUTE },
        )
        val states = mutableListOf<Map<Indicator, QuoteState>>()
        val collection = launch {
            repository.observeLatestQuotes().take(3).collect { quotes ->
                states += quotes.mapValues { it.value.state }
            }
        }

        runCurrent()
        assertEquals(
            listOf(mapOf(Indicator.USD_KRW to QuoteState.FRESH, Indicator.KOSPI to QuoteState.FRESH)),
            states,
        )

        advanceTimeBy(MINUTE + 1)
        runCurrent()
        assertEquals(
            listOf(
                mapOf(Indicator.USD_KRW to QuoteState.FRESH, Indicator.KOSPI to QuoteState.FRESH),
                mapOf(Indicator.USD_KRW to QuoteState.STALE, Indicator.KOSPI to QuoteState.FRESH),
            ),
            states,
        )

        advanceTimeBy(MINUTE)
        runCurrent()

        assertEquals(
            listOf(
                mapOf(Indicator.USD_KRW to QuoteState.FRESH, Indicator.KOSPI to QuoteState.FRESH),
                mapOf(Indicator.USD_KRW to QuoteState.STALE, Indicator.KOSPI to QuoteState.FRESH),
                mapOf(Indicator.USD_KRW to QuoteState.STALE, Indicator.KOSPI to QuoteState.STALE),
            ),
            states,
        )
        assertTrue(collection.isCompleted)
    }

    private fun providerReturning(result: ProviderResult? = null) = MarketDataProvider { indicator ->
        result ?: ProviderResult.Failure(indicator, "unexpected", retryable = false)
    }

    private fun quote(
        indicator: Indicator,
        observedAtEpochMillis: Long = 1_725_000_000_000,
    ) = MarketQuote(indicator, 1382.4, 2.1, 0.15, observedAtEpochMillis, "Test", QuoteState.FRESH)

    private fun MarketQuote.toEntity(id: Long) = QuoteEntity(
        id = id,
        indicator = indicator,
        value = value,
        change = change,
        percentChange = percentChange,
        observedAtEpochMillis = observedAtEpochMillis,
        source = source,
        state = state,
        signalLabel = signalLabel,
        rsi = rsi,
    )

    private fun QuoteEntity.toMarketQuote() = MarketQuote(
        indicator = indicator,
        value = value,
        change = change,
        percentChange = percentChange,
        observedAtEpochMillis = observedAtEpochMillis,
        source = source,
        state = state,
        signalLabel = signalLabel,
        rsi = rsi,
    )

    private class FakeMarketDao(initialRows: List<QuoteEntity> = emptyList()) : MarketDao {
        private val latestRows = MutableStateFlow(initialRows)
        val insertedQuotes = mutableListOf<QuoteEntity>()
        val insertedFetchLogs = mutableListOf<FetchLogEntity>()

        override suspend fun insertQuote(entity: QuoteEntity) {
            val stored = entity.copy(id = if (entity.id == 0L) insertedQuotes.size + 1L else entity.id)
            insertedQuotes += stored
            latestRows.value = latestRows.value.filterNot { it.indicator == stored.indicator } + stored
        }

        override suspend fun insertFetchLog(entity: FetchLogEntity) {
            insertedFetchLogs += entity.copy(id = insertedFetchLogs.size + 1L)
        }

        override fun observeLatestQuotes(): Flow<List<QuoteEntity>> = latestRows

        override suspend fun latestQuotes(): List<QuoteEntity> = latestRows.value
    }

    private companion object {
        const val MINUTE = 60 * 1000L
    }
}
