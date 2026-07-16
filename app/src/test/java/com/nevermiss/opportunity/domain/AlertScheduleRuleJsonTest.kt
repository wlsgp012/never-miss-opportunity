package com.nevermiss.opportunity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertScheduleRuleJsonTest {
    @Test
    fun `exports and imports rules with versioned json`() {
        val rules = listOf(
            AlertScheduleRule.fixed("fixed-1", Indicator.KOSPI, secondOfDay(9, 0)),
            AlertScheduleRule.recurring("range-1", Indicator.BTC_USD, secondOfDay(12, 0), secondOfDay(13, 0), 300),
        )

        val json = AlertScheduleRuleJson.exportRules(rules)
        val imported = AlertScheduleRuleJson.importRules(json).getOrThrow()

        assertTrue(json.contains("\"version\": 1"))
        assertEquals(rules, imported)
    }

    @Test
    fun `rejects malformed json`() {
        val result = AlertScheduleRuleJson.importRules("{bad-json")

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects invalid rule values`() {
        val json = """
            {
              "version": 1,
              "rules": [
                {
                  "id": "bad",
                  "indicator": "KOSPI",
                  "type": "RECURRING_RANGE",
                  "enabled": true,
                  "startSecondOfDay": 54000,
                  "endSecondOfDay": 32400,
                  "intervalSeconds": 0
                }
              ]
            }
        """.trimIndent()

        val result = AlertScheduleRuleJson.importRules(json)

        assertTrue(result.isFailure)
    }

    private fun secondOfDay(hour: Int, minute: Int): Int = hour * 3600 + minute * 60
}
