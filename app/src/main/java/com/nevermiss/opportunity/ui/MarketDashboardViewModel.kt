package com.nevermiss.opportunity.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevermiss.opportunity.data.local.AlertScheduleStore
import com.nevermiss.opportunity.data.MarketRepository
import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.AlertScheduleFormatter
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.scheduler.MarketAlarmScheduler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MarketDashboardUiState(
    val rows: List<MarketRowState>,
    val isRefreshing: Boolean = false,
) {
    companion object {
        fun empty(): MarketDashboardUiState =
            MarketDashboardUiState(Indicator.displayOrder.map { MarketRowState(it, null) })
    }
}

data class MarketRowState(
    val indicator: Indicator,
    val quote: MarketQuote?,
    val scheduleRules: List<AlertScheduleRule> = emptyList(),
)

class MarketDashboardViewModel(
    private val repository: MarketRepository,
    private val scheduleStore: AlertScheduleStore,
    private val scheduler: MarketAlarmScheduler,
) : ViewModel() {
    val uiState: StateFlow<MarketDashboardUiState> = combine(
        repository.observeLatestQuotes(),
        scheduleStore.observeRules(),
    ) { quotes, rules ->
        val rulesByIndicator = rules.groupBy { it.indicator }
        MarketDashboardUiState(
            Indicator.displayOrder.map { indicator ->
                MarketRowState(
                    indicator = indicator,
                    quote = quotes[indicator],
                    scheduleRules = rulesByIndicator[indicator].orEmpty(),
                )
            },
        )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarketDashboardUiState.empty())

    fun refreshAll() {
        viewModelScope.launch {
            repository.refreshIndicators(Indicator.displayOrder, "manual")
        }
    }

    fun addFixedRule(indicator: Indicator, timeText: String): Boolean {
        val secondOfDay = AlertScheduleFormatter.parseSecondOfDay(timeText) ?: return false
        scheduleStore.addRule(
            AlertScheduleRule.fixed(
                id = newRuleId(indicator),
                indicator = indicator,
                secondOfDay = secondOfDay,
            ),
        )
        scheduler.scheduleAll()
        return true
    }

    fun addRecurringRule(
        indicator: Indicator,
        startText: String,
        endText: String,
        intervalValueText: String,
        intervalUnitSeconds: Int,
    ): Boolean {
        val start = AlertScheduleFormatter.parseSecondOfDay(startText) ?: return false
        val end = AlertScheduleFormatter.parseSecondOfDay(endText) ?: return false
        val intervalValue = intervalValueText.toIntOrNull()?.takeIf { it > 0 } ?: return false
        val intervalSeconds = intervalValue * intervalUnitSeconds
        if (start > end) return false
        scheduleStore.addRule(
            AlertScheduleRule.recurring(
                id = newRuleId(indicator),
                indicator = indicator,
                startSecondOfDay = start,
                endSecondOfDay = end,
                intervalSeconds = intervalSeconds,
            ),
        )
        scheduler.scheduleAll()
        return true
    }

    fun toggleRule(rule: AlertScheduleRule) {
        scheduleStore.updateRule(rule.copy(enabled = !rule.enabled))
        scheduler.scheduleAll()
    }

    fun deleteRule(ruleId: String) {
        scheduleStore.deleteRule(ruleId)
        scheduler.scheduleAll()
    }

    fun exportScheduleJson(): String = scheduleStore.exportJson()

    fun importScheduleJson(rawJson: String): Boolean {
        val imported = scheduleStore.importJson(rawJson)
        if (imported) scheduler.scheduleAll()
        return imported
    }

    private fun newRuleId(indicator: Indicator): String =
        "user-${indicator.name.lowercase()}-${System.currentTimeMillis()}"
}
