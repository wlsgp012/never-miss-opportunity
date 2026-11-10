package com.nevermiss.opportunity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAlertScheduleRulesTest {
    @Test
    fun `default rules preserve the existing indicator coverage`() {
        val byIndicator = DefaultAlertScheduleRules.rules.groupBy { it.indicator }

        assertTrue(byIndicator.keys.containsAll(Indicator.displayOrder))
        assertEquals(1, byIndicator.getValue(Indicator.USD_KRW).size)
        assertEquals(2, byIndicator.getValue(Indicator.CNN_FEAR_GREED).size)
        assertEquals(4, byIndicator.getValue(Indicator.SP500).size)
        assertEquals(3, byIndicator.getValue(Indicator.NASDAQ100).size)
        assertEquals(4, byIndicator.getValue(Indicator.VIX).size)
        assertEquals(1, byIndicator.getValue(Indicator.KRX_GOLD_1G).size)
        assertEquals(1, byIndicator.getValue(Indicator.KOSPI).size)
        assertEquals(1, byIndicator.getValue(Indicator.BTC_USD).size)
        assertEquals(1, byIndicator.getValue(Indicator.BTC_MVRV).size)
    }

    @Test
    fun `default usd krw and kospi use hourly recurring ranges`() {
        val usdKrw = DefaultAlertScheduleRules.rules.single { it.indicator == Indicator.USD_KRW }
        val kospi = DefaultAlertScheduleRules.rules.single { it.indicator == Indicator.KOSPI }

        assertEquals(AlertScheduleRuleType.RECURRING_RANGE, usdKrw.type)
        assertEquals(secondOfDay(9, 0), usdKrw.startSecondOfDay)
        assertEquals(secondOfDay(21, 0), usdKrw.endSecondOfDay)
        assertEquals(3600, usdKrw.intervalSeconds)
        assertEquals(secondOfDay(9, 0), kospi.startSecondOfDay)
        assertEquals(secondOfDay(15, 0), kospi.endSecondOfDay)
        assertEquals(3600, kospi.intervalSeconds)
    }

    @Test
    fun `oil schedules weekdays at nine Seoul time and skips weekends`() {
        val rule = DefaultAlertScheduleRules.rules.single { it.indicator.name == "WTI_CRUDE" }
        val friday = java.time.ZonedDateTime.parse("2026-11-13T09:00:00+09:00[Asia/Seoul]")
        assertEquals(java.time.ZonedDateTime.parse("2026-11-16T09:00:00+09:00[Asia/Seoul]"), rule.nextOccurrence(friday))
        assertEquals(java.time.ZonedDateTime.parse("2026-11-16T09:00:00+09:00[Asia/Seoul]"), rule.nextOccurrence(friday.plusDays(1)))
    }

    private fun secondOfDay(hour: Int, minute: Int): Int = hour * 3600 + minute * 60
}
