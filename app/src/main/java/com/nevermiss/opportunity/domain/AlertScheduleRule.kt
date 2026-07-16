package com.nevermiss.opportunity.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

@Serializable
enum class AlertScheduleRuleType {
    FIXED_TIME,
    RECURRING_RANGE,
}

@Serializable
data class AlertScheduleRule(
    val id: String,
    val indicator: Indicator,
    val type: AlertScheduleRuleType,
    val enabled: Boolean,
    val fixedSecondOfDay: Int? = null,
    val startSecondOfDay: Int? = null,
    val endSecondOfDay: Int? = null,
    val intervalSeconds: Int? = null,
) {
    fun nextOccurrence(now: ZonedDateTime): ZonedDateTime? {
        if (!enabled) return null
        return when (type) {
            AlertScheduleRuleType.FIXED_TIME -> fixedSecondOfDay?.let { nextFixed(now, it) }
            AlertScheduleRuleType.RECURRING_RANGE -> nextRecurring(now)
        }
    }

    private fun nextRecurring(now: ZonedDateTime): ZonedDateTime? {
        val start = startSecondOfDay ?: return null
        val end = endSecondOfDay ?: return null
        val interval = intervalSeconds?.takeIf { it > 0 } ?: return null
        if (start > end) return null

        val todaySecond = now.toLocalTime().toSecondOfDay()
        val todayNextSecond = when {
            todaySecond < start -> start
            todaySecond >= end -> null
            else -> {
                val elapsed = todaySecond - start
                val nextStep = elapsed / interval + 1
                start + nextStep * interval
            }
        }

        return if (todayNextSecond != null && todayNextSecond <= end) {
            atSecondOfDay(now.toLocalDate(), todayNextSecond)
        } else {
            atSecondOfDay(now.toLocalDate().plusDays(1), start)
        }
    }

    companion object {
        fun fixed(
            id: String,
            indicator: Indicator,
            secondOfDay: Int,
            enabled: Boolean = true,
        ): AlertScheduleRule =
            AlertScheduleRule(
                id = id,
                indicator = indicator,
                type = AlertScheduleRuleType.FIXED_TIME,
                enabled = enabled,
                fixedSecondOfDay = secondOfDay,
            )

        fun recurring(
            id: String,
            indicator: Indicator,
            startSecondOfDay: Int,
            endSecondOfDay: Int,
            intervalSeconds: Int,
            enabled: Boolean = true,
        ): AlertScheduleRule =
            AlertScheduleRule(
                id = id,
                indicator = indicator,
                type = AlertScheduleRuleType.RECURRING_RANGE,
                enabled = enabled,
                startSecondOfDay = startSecondOfDay,
                endSecondOfDay = endSecondOfDay,
                intervalSeconds = intervalSeconds,
            )
    }
}

fun nextFixed(now: ZonedDateTime, secondOfDay: Int): ZonedDateTime {
    val today = atSecondOfDay(now.toLocalDate(), secondOfDay)
    return if (today.isAfter(now)) today else today.plusDays(1)
}

fun atSecondOfDay(date: LocalDate, secondOfDay: Int): ZonedDateTime =
    date.atTime(LocalTime.ofSecondOfDay(secondOfDay.toLong())).atZone(MarketSchedule.zoneId)
