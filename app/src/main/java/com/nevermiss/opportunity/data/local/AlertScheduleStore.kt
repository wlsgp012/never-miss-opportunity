package com.nevermiss.opportunity.data.local

import android.content.Context
import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.AlertScheduleRuleJson
import com.nevermiss.opportunity.domain.DefaultAlertScheduleRules
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
        if (!preferences.contains(KEY_RULES)) {
            saveRules(DefaultAlertScheduleRules.rules)
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
    }
}
