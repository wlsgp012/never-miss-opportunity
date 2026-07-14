package com.nevermiss.opportunity.domain

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
}
