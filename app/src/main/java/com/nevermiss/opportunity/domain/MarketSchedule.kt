package com.nevermiss.opportunity.domain

import java.time.LocalTime
import java.time.ZoneId

data class ScheduleSlot(
    val id: String,
    val time: LocalTime,
    val indicators: List<Indicator>,
)

object MarketSchedule {
    val zoneId: ZoneId = ZoneId.of("Asia/Seoul")

    val dailySlots: List<ScheduleSlot> = buildList {
        for (hour in 9..21) {
            addSlot(LocalTime.of(hour, 0), Indicator.USD_KRW)
        }
        addSlot(LocalTime.of(9, 0), Indicator.CNN_FEAR_GREED)
        addSlot(LocalTime.of(23, 0), Indicator.CNN_FEAR_GREED)
        addSlot(LocalTime.of(8, 30), Indicator.SP500)
        addSlot(LocalTime.of(8, 30), Indicator.VIX)
        listOf(LocalTime.of(22, 40), LocalTime.of(23, 0), LocalTime.of(23, 40)).forEach { time ->
            addSlot(time, Indicator.SP500)
            addSlot(time, Indicator.NASDAQ100)
            addSlot(time, Indicator.VIX)
        }
        addSlot(LocalTime.of(12, 0), Indicator.KRX_GOLD_1G)
        addSlot(LocalTime.of(12, 0), Indicator.BTC_USD)
        addSlot(LocalTime.of(12, 0), Indicator.BTC_MVRV)
        for (hour in 9..15) {
            addSlot(LocalTime.of(hour, 0), Indicator.KOSPI)
        }
    }
        .groupBy { it.time }
        .map { (time, slots) ->
            ScheduleSlot(
                id = "%02d%02d".format(time.hour, time.minute),
                time = time,
                indicators = Indicator.displayOrder.filter { indicator ->
                    slots.any { indicator in it.indicators }
                },
            )
        }
        .sortedWith(compareBy<ScheduleSlot> { it.time.hour }.thenBy { it.time.minute })

    fun slotsFor(indicator: Indicator): List<ScheduleSlot> =
        dailySlots.filter { indicator in it.indicators }

    fun slotAt(time: LocalTime): ScheduleSlot =
        dailySlots.first { it.time == time }

    private fun MutableList<ScheduleSlot>.addSlot(time: LocalTime, indicator: Indicator) {
        add(ScheduleSlot(id = "%02d%02d-%s".format(time.hour, time.minute, indicator.name), time = time, indicators = listOf(indicator)))
    }
}
