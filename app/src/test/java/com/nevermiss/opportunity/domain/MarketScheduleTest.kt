package com.nevermiss.opportunity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class MarketScheduleTest {
    @Test
    fun `schedule contains fixed indicator order`() {
        assertEquals(
            listOf(
                Indicator.USD_KRW,
                Indicator.CNN_FEAR_GREED,
                Indicator.SP500,
                Indicator.NASDAQ100,
                Indicator.VIX,
                Indicator.KRX_GOLD_1G,
                Indicator.KOSPI,
                Indicator.BTC_USD,
                Indicator.BTC_MVRV,
                Indicator.WTI_CRUDE,
            ),
            Indicator.displayOrder,
        )
    }

    @Test
    fun `usd krw runs hourly from nine through twenty one`() {
        val times = MarketSchedule.slotsFor(Indicator.USD_KRW).map { it.time }
        assertEquals((9..21).map { LocalTime.of(it, 0) }, times)
    }

    @Test
    fun `us index slots are grouped at requested night times`() {
        val times = listOf(LocalTime.of(8, 30), LocalTime.of(22, 40), LocalTime.of(23, 0), LocalTime.of(23, 40))

        times.forEach { time ->
            val slot = MarketSchedule.slotAt(time)
            assertTrue(slot.indicators.contains(Indicator.SP500))
            if (time != LocalTime.of(8, 30)) {
                assertTrue(slot.indicators.contains(Indicator.NASDAQ100))
            }
            assertTrue(slot.indicators.contains(Indicator.VIX))
        }
    }

    @Test
    fun `bitcoin indicators run at noon`() {
        assertEquals(listOf(LocalTime.of(12, 0)), MarketSchedule.slotsFor(Indicator.BTC_USD).map { it.time })
        assertEquals(listOf(LocalTime.of(12, 0)), MarketSchedule.slotsFor(Indicator.BTC_MVRV).map { it.time })
    }

    @Test
    fun `kospi and gold schedules match requested times`() {
        assertEquals((9..15).map { LocalTime.of(it, 0) }, MarketSchedule.slotsFor(Indicator.KOSPI).map { it.time })
        assertEquals(listOf(LocalTime.of(12, 0)), MarketSchedule.slotsFor(Indicator.KRX_GOLD_1G).map { it.time })
    }

    @Test
    fun `slots do not contain duplicate indicators`() {
        MarketSchedule.dailySlots.forEach { slot ->
            assertEquals(slot.indicators.distinct(), slot.indicators)
        }
    }
}
