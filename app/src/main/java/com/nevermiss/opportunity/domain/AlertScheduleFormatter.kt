package com.nevermiss.opportunity.domain

object AlertScheduleFormatter {
    fun parseSecondOfDay(value: String): Int? {
        val parts = value.trim().split(":").map { it.toIntOrNull() ?: return null }
        if (parts.size !in 2..3) return null
        val hour = parts[0]
        val minute = parts[1]
        val second = parts.getOrElse(2) { 0 }
        if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null
        return hour * 3600 + minute * 60 + second
    }

    fun formatSecondOfDay(secondOfDay: Int): String {
        val hour = secondOfDay / 3600
        val minute = (secondOfDay % 3600) / 60
        val second = secondOfDay % 60
        return "%02d:%02d:%02d".format(hour, minute, second)
    }

    fun describe(rule: AlertScheduleRule): String =
        when (rule.type) {
            AlertScheduleRuleType.FIXED_TIME -> rule.fixedSecondOfDay
                ?.let { "At ${formatSecondOfDay(it)}" }
                ?: "Invalid fixed time"
            AlertScheduleRuleType.RECURRING_RANGE -> {
                val start = rule.startSecondOfDay?.let(::formatSecondOfDay)
                val end = rule.endSecondOfDay?.let(::formatSecondOfDay)
                val interval = rule.intervalSeconds?.let(::formatInterval)
                if (start != null && end != null && interval != null) "$start-$end every $interval" else "Invalid range"
            }
        }

    fun formatInterval(seconds: Int): String =
        when {
            seconds % 3600 == 0 -> "${seconds / 3600}h"
            seconds % 60 == 0 -> "${seconds / 60}m"
            else -> "${seconds}s"
        }
}
