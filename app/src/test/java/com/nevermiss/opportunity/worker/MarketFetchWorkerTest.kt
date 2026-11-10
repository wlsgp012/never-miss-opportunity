package com.nevermiss.opportunity.worker

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.data.RefreshResult
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketSchedule
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class MarketFetchWorkerTest {
    @Test
    fun `notification should post only when refresh has successes`() {
        val quote = MarketQuote(Indicator.USD_KRW, 1382.4, 2.1, 0.15, 1_725_000_000_000, "Test", QuoteState.FRESH)
        val success = RefreshResult("0900", listOf(quote), emptyList())
        val failure = RefreshResult("0900", emptyList(), listOf(ProviderResult.Failure(Indicator.KOSPI, "network", true)))

        assertTrue(MarketFetchWorker.shouldPostNotification(success))
        assertFalse(MarketFetchWorker.shouldPostNotification(failure))
    }

    @Test
    fun `notification skips source-stale successes`() {
        val stale = quote(QuoteState.STALE)
        val result = RefreshResult("0900", listOf(stale), emptyList())

        assertFalse(MarketFetchWorker.shouldPostNotification(result))
    }

    @Test
    fun `notification skips weekends`() {
        val result = RefreshResult("0900", listOf(quote(QuoteState.FRESH)), emptyList())
        val saturday = ZonedDateTime.of(2026, 7, 18, 9, 0, 0, 0, MarketSchedule.zoneId)

        assertFalse(MarketFetchWorker.shouldPostNotification(result, saturday))
    }

    @Test
    fun `all retryable failures request retry`() {
        val result = RefreshResult(
            "0900",
            emptyList(),
            listOf(
                ProviderResult.Failure(Indicator.USD_KRW, "network", retryable = true),
                ProviderResult.Failure(Indicator.KOSPI, "http-503", retryable = true),
            ),
        )

        assertTrue(MarketFetchWorker.shouldRetry(result))
    }

    @Test
    fun `partial success does not request retry`() {
        val result = RefreshResult(
            "0900",
            listOf(quote(QuoteState.FRESH)),
            listOf(ProviderResult.Failure(Indicator.KOSPI, "network", retryable = true)),
        )

        assertFalse(MarketFetchWorker.shouldRetry(result))
    }

    @Test
    fun `all non-retryable failures do not request retry`() {
        val result = RefreshResult(
            "0900",
            emptyList(),
            listOf(ProviderResult.Failure(Indicator.KOSPI, "empty-chart", retryable = false)),
        )

        assertFalse(MarketFetchWorker.shouldRetry(result))
    }

    @Test
    fun `oil notification skips weekends and stale prices`() {
        val oil = Indicator.entries.single { it.name == "WTI_CRUDE" }
        val result = RefreshResult("oil", listOf(quote(QuoteState.FRESH).copy(indicator = oil)), emptyList())
        val saturday = ZonedDateTime.parse("2026-11-14T09:00:00+09:00[Asia/Seoul]")
        assertFalse(MarketFetchWorker.shouldPostNotification(result, saturday))
        assertFalse(MarketFetchWorker.shouldPostNotification(result, saturday.plusDays(1)))
        assertFalse(MarketFetchWorker.shouldPostNotification(result.copy(successes = result.successes.map { it.copy(state = QuoteState.STALE) }), saturday.plusDays(2)))
    }

    private fun quote(state: QuoteState) =
        MarketQuote(Indicator.USD_KRW, 1382.4, 2.1, 0.15, 1_725_000_000_000, "Test", state)
}
