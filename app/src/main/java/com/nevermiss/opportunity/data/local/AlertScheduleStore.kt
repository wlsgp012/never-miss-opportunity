package com.nevermiss.opportunity.data.local

import android.content.Context
import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.AlertScheduleRuleJson
import com.nevermiss.opportunity.domain.DefaultAlertScheduleRules
import com.nevermiss.opportunity.domain.Indicator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AlertScheduleStore {
    fun observeRules(): Flow<List<AlertScheduleRule>>
    fun rules(): List<AlertScheduleRule>
    fun rule(ruleId: String): AlertScheduleRule?
    fun exportJson(): String
    fun importJson(rawJson: String): Boolean
    fun addRule(rule: AlertScheduleRule)
    fun updateRule(rule: AlertScheduleRule)
    fun deleteRule(ruleId: String)
}

class SharedPreferencesAlertScheduleStore(context: Context) : AlertScheduleStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val rulesState = MutableStateFlow(loadRules())

    init {
        if (!preferences.getBoolean(KEY_OIL_DEFAULT_ADDED, false)) {
            val oilRule = DefaultAlertScheduleRules.rules.single { it.indicator == Indicator.WTI_CRUDE }
            val migratedRules = if (rulesState.value.any { it.indicator == Indicator.WTI_CRUDE }) {
                rulesState.value
            } else {
                rulesState.value + oilRule
            }
            // Persist the migration marker with the rules so deletions and imports stay intentional.
            preferences.edit()
                .putString(KEY_RULES, AlertScheduleRuleJson.exportRules(migratedRules))
                .putBoolean(KEY_OIL_DEFAULT_ADDED, true)
                .apply()
            rulesState.value = migratedRules
        }
    }

    override fun observeRules(): Flow<List<AlertScheduleRule>> = rulesState

    override fun rules(): List<AlertScheduleRule> = rulesState.value

    override fun rule(ruleId: String): AlertScheduleRule? =
        rules().firstOrNull { it.id == ruleId }

    override fun exportJson(): String = AlertScheduleRuleJson.exportRules(rules())

    override fun importJson(rawJson: String): Boolean {
        val importedRules = AlertScheduleRuleJson.importRules(rawJson).getOrNull() ?: return false
        saveRules(importedRules)
        return true
    }

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
        return AlertScheduleRuleJson.importRules(raw)
            .getOrDefault(DefaultAlertScheduleRules.rules)
    }

    private fun saveRules(rules: List<AlertScheduleRule>) {
        preferences.edit()
            .putString(KEY_RULES, AlertScheduleRuleJson.exportRules(rules))
            .apply()
        rulesState.value = rules
    }

    private companion object {
        const val PREFERENCES_NAME = "alert-schedule-rules"
        const val KEY_RULES = "rules"
        const val KEY_OIL_DEFAULT_ADDED = "oil-default-added"
    }
}
