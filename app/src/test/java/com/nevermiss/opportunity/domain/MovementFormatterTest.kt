package com.nevermiss.opportunity.domain

import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class MovementFormatterTest {
    @Test
    fun `formats positive movement`() {
        val quote = MarketQuote(
            indicator = Indicator.USD_KRW,
            value = 1382.4,
            change = 2.1,
            percentChange = 0.15,
            observedAtEpochMillis = 1_725_000_000_000,
            source = "Test",
            state = QuoteState.FRESH,
        )

        assertEquals("1,382.40", MovementFormatter.value(quote))
        assertEquals("+2.10 (+0.15%)", MovementFormatter.movement(quote))
    }

    @Test
    fun `formats unavailable percent change`() {
        val quote = MarketQuote(
            indicator = Indicator.CNN_FEAR_GREED,
            value = 44.0,
            change = -3.0,
            percentChange = null,
            observedAtEpochMillis = 1_725_000_000_000,
            source = "Test",
            state = QuoteState.FRESH,
        )

        assertEquals("-3.00", MovementFormatter.movement(quote))
    }

    @Test
    fun `formats consistently when called concurrently`() {
        val workers = 16
        val iterations = 500
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(workers)
        val futures = (0 until workers).map { worker ->
            executor.submit {
                start.await()
                val quote = MarketQuote(
                    indicator = Indicator.USD_KRW,
                    value = 1000.0 + worker + 0.25,
                    change = 2.0 + worker + 0.5,
                    percentChange = 0.1 + worker + 0.25,
                    observedAtEpochMillis = 1_725_000_000_000,
                    source = "Test",
                    state = QuoteState.FRESH,
                )

                repeat(iterations) {
                    assertEquals(
                        String.format(Locale.US, "%,.2f", quote.value),
                        MovementFormatter.value(quote),
                    )
                    assertEquals(
                        String.format(Locale.US, "%+,.2f (+%,.2f%%)", quote.change, quote.percentChange),
                        MovementFormatter.movement(quote),
                    )
                }
            }
        }

        try {
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }
}
