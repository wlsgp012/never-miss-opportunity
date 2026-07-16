package com.nevermiss.opportunity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

class AlertScheduleRuleTest {
    @Test
    fun `fixed rule returns today when time is ahead`() {
        val rule = AlertScheduleRule.fixed("rule-1", Indicator.KOSPI, secondOfDay(9, 0))
        val now = at(2026, 7, 15, 8, 50, 0)

        assertEquals(at(2026, 7, 15, 9, 0, 0), rule.nextOccurrence(now))
    }

    @Test
    fun `fixed rule rolls to tomorrow when time already passed`() {
        val rule = AlertScheduleRule.fixed("rule-1", Indicator.KOSPI, secondOfDay(9, 0))
        val now = at(2026, 7, 15, 9, 0, 0)

        assertEquals(at(2026, 7, 16, 9, 0, 0), rule.nextOccurrence(now))
    }

    @Test
    fun `recurring rule returns range start when range is ahead`() {
        val rule = AlertScheduleRule.recurring(
            id = "rule-1",
            indicator = Indicator.USD_KRW,
            startSecondOfDay = secondOfDay(9, 0),
            endSecondOfDay = secondOfDay(21, 0),
            intervalSeconds = 3600,
        )
        val now = at(2026, 7, 15, 8, 30, 0)

        assertEquals(at(2026, 7, 15, 9, 0, 0), rule.nextOccurrence(now))
    }

    @Test
    fun `recurring rule returns next interval within range`() {
        val rule = AlertScheduleRule.recurring(
            id = "rule-1",
            indicator = Indicator.USD_KRW,
            startSecondOfDay = secondOfDay(9, 0),
            endSecondOfDay = secondOfDay(21, 0),
            intervalSeconds = 3600,
        )
        val now = at(2026, 7, 15, 10, 15, 0)

        assertEquals(at(2026, 7, 15, 11, 0, 0), rule.nextOccurrence(now))
    }

    @Test
    fun `recurring rule supports second intervals`() {
        val rule = AlertScheduleRule.recurring(
            id = "rule-1",
            indicator = Indicator.BTC_USD,
            startSecondOfDay = secondOfDay(12, 0),
            endSecondOfDay = secondOfDay(12, 1),
            intervalSeconds = 30,
        )
        val now = at(2026, 7, 15, 12, 0, 10)

        assertEquals(at(2026, 7, 15, 12, 0, 30), rule.nextOccurrence(now))
    }

    @Test
    fun `disabled rule has no next occurrence`() {
        val rule = AlertScheduleRule.fixed("rule-1", Indicator.KOSPI, secondOfDay(9, 0), enabled = false)

        assertNull(rule.nextOccurrence(at(2026, 7, 15, 8, 0, 0)))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): ZonedDateTime =
        ZonedDateTime.of(year, month, day, hour, minute, second, 0, MarketSchedule.zoneId)

    private fun secondOfDay(hour: Int, minute: Int, second: Int = 0): Int =
        hour * 3600 + minute * 60 + second
}
