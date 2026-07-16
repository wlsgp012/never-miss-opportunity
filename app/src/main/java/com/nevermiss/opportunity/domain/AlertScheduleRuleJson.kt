package com.nevermiss.opportunity.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AlertScheduleRuleJson {
    private const val CURRENT_VERSION = 1
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun exportRules(rules: List<AlertScheduleRule>): String =
        json.encodeToString(AlertScheduleRulesSnapshot(version = CURRENT_VERSION, rules = rules))

    fun importRules(rawJson: String): Result<List<AlertScheduleRule>> =
        runCatching {
            val snapshot = json.decodeFromString<AlertScheduleRulesSnapshot>(rawJson)
            require(snapshot.version == CURRENT_VERSION) { "unsupported-version" }
            require(snapshot.rules.all { it.isValid() }) { "invalid-rule" }
            snapshot.rules
        }
}

@Serializable
data class AlertScheduleRulesSnapshot(
    val version: Int = 1,
    val rules: List<AlertScheduleRule>,
)

fun AlertScheduleRule.isValid(): Boolean =
    id.isNotBlank() &&
        when (type) {
            AlertScheduleRuleType.FIXED_TIME ->
                fixedSecondOfDay?.isSecondOfDay() == true
            AlertScheduleRuleType.RECURRING_RANGE -> {
                val start = startSecondOfDay
                val end = endSecondOfDay
                val interval = intervalSeconds
                start?.isSecondOfDay() == true &&
                    end?.isSecondOfDay() == true &&
                    start <= end &&
                    interval != null &&
                    interval > 0
            }
        }

private fun Int.isSecondOfDay(): Boolean = this in 0..86_399
