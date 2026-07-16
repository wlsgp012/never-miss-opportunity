package com.nevermiss.opportunity.data.local

import android.content.Context
import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.DefaultAlertScheduleRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface AlertScheduleStore {
    fun observeRules(): Flow<List<AlertScheduleRule>>
    fun rules(): List<AlertScheduleRule>
    fun rule(ruleId: String): AlertScheduleRule?
    fun addRule(rule: AlertScheduleRule)
    fun updateRule(rule: AlertScheduleRule)
    fun deleteRule(ruleId: String)
}

class SharedPreferencesAlertScheduleStore(context: Context) : AlertScheduleStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val rulesState = MutableStateFlow(loadRules())

    init {
        if (!preferences.contains(KEY_RULES)) {
            saveRules(DefaultAlertScheduleRules.rules)
        }
    }

    override fun observeRules(): Flow<List<AlertScheduleRule>> = rulesState

    override fun rules(): List<AlertScheduleRule> = rulesState.value

    override fun rule(ruleId: String): AlertScheduleRule? =
        rules().firstOrNull { it.id == ruleId }

    override fun addRule(rule: AlertScheduleRule) {
        saveRules(rules().filterNot { it.id == rule.id } + rule)
    }

    override fun updateRule(rule: AlertScheduleRule) {
        saveRules(rules().map { if (it.id == rule.id) rule else it })
    }

    override fun deleteRule(ruleId: String) {
        saveRules(rules().filterNot { it.id == ruleId })
    }

    private fun loadRules(): List<AlertScheduleRule> {
        val raw = preferences.getString(KEY_RULES, null) ?: return DefaultAlertScheduleRules.rules
        return runCatching { json.decodeFromString<AlertScheduleRulesSnapshot>(raw).rules }
            .getOrDefault(DefaultAlertScheduleRules.rules)
    }

    private fun saveRules(rules: List<AlertScheduleRule>) {
        preferences.edit()
            .putString(KEY_RULES, json.encodeToString(AlertScheduleRulesSnapshot(rules)))
            .apply()
        rulesState.value = rules
    }

    private companion object {
        const val PREFERENCES_NAME = "alert-schedule-rules"
        const val KEY_RULES = "rules"
    }
}

@Serializable
private data class AlertScheduleRulesSnapshot(
    val rules: List<AlertScheduleRule>,
)
