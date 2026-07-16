package com.nevermiss.opportunity.domain

object DefaultAlertScheduleRules {
    val rules: List<AlertScheduleRule> = buildList {
        add(AlertScheduleRule.recurring("default-usd-krw-hourly", Indicator.USD_KRW, secondOfDay(9), secondOfDay(21), 3600))
        addFixed(Indicator.CNN_FEAR_GREED, 9, 0)
        addFixed(Indicator.CNN_FEAR_GREED, 23, 0)
        addFixed(Indicator.SP500, 8, 30)
        listOf(Triple(22, 40, Indicator.SP500), Triple(23, 0, Indicator.SP500), Triple(23, 40, Indicator.SP500)).forEach {
            addFixed(it.third, it.first, it.second)
        }
        listOf(22 to 40, 23 to 0, 23 to 40).forEach { (hour, minute) ->
            addFixed(Indicator.NASDAQ100, hour, minute)
            addFixed(Indicator.VIX, hour, minute)
        }
        addFixed(Indicator.VIX, 8, 30)
        addFixed(Indicator.KRX_GOLD_1G, 12, 0)
        add(AlertScheduleRule.recurring("default-kospi-hourly", Indicator.KOSPI, secondOfDay(9), secondOfDay(15), 3600))
        addFixed(Indicator.BTC_USD, 12, 0)
        addFixed(Indicator.BTC_MVRV, 12, 0)
    }

    private fun MutableList<AlertScheduleRule>.addFixed(indicator: Indicator, hour: Int, minute: Int) {
        add(
            AlertScheduleRule.fixed(
                id = "default-${indicator.name.lowercase()}-%02d%02d".format(hour, minute),
                indicator = indicator,
                secondOfDay = secondOfDay(hour, minute),
            ),
        )
    }

    private fun secondOfDay(hour: Int, minute: Int = 0): Int = hour * 3600 + minute * 60
}
