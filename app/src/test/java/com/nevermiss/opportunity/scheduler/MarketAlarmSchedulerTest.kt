package com.nevermiss.opportunity.scheduler

import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZonedDateTime

class MarketAlarmSchedulerTest {
    @Test
    fun `schedule slot schedules only that slot for its next occurrence`() {
        val scheduled = mutableListOf<Pair<String, Long>>()
        val rule = AlertScheduleRule.fixed("rule-0900", Indicator.KOSPI, 9 * 3600)
        val scheduler = MarketAlarmScheduler(
            alarmSetter = { slotId, triggerAtEpochMillis -> scheduled += slotId to triggerAtEpochMillis },
            rulesProvider = { listOf(rule) },
        )
        val now = ZonedDateTime.of(2026, 7, 14, 9, 1, 0, 0, MarketSchedule.zoneId)

        scheduler.scheduleSlot("rule-0900", now)

        val expected = ZonedDateTime.of(2026, 7, 15, 9, 0, 0, 0, MarketSchedule.zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(listOf("rule-0900" to expected), scheduled)
    }

    @Test
    fun `next occurrence uses today when slot is still ahead`() {
        val now = ZonedDateTime.of(2026, 7, 14, 8, 30, 0, 0, MarketSchedule.zoneId)

        val next = MarketAlarmScheduler.nextOccurrence(now, LocalTime.of(9, 0))

        assertEquals(2026, next.year)
        assertEquals(14, next.dayOfMonth)
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }

    @Test
    fun `next occurrence rolls to tomorrow when slot already passed`() {
        val now = ZonedDateTime.of(2026, 7, 14, 23, 50, 0, 0, MarketSchedule.zoneId)

        val next = MarketAlarmScheduler.nextOccurrence(now, LocalTime.of(9, 0))

        assertEquals(2026, next.year)
        assertEquals(15, next.dayOfMonth)
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }

    @Test
    fun `next occurrence skips weekends`() {
        val now = ZonedDateTime.of(2026, 7, 17, 23, 50, 0, 0, MarketSchedule.zoneId)

        val next = MarketAlarmScheduler.nextOccurrence(now, LocalTime.of(9, 0))

        assertEquals(2026, next.year)
        assertEquals(20, next.dayOfMonth)
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }
}
