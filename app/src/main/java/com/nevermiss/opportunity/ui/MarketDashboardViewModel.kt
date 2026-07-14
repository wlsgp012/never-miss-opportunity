package com.nevermiss.opportunity.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevermiss.opportunity.data.MarketRepository
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
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
)

class MarketDashboardViewModel(
    private val repository: MarketRepository,
) : ViewModel() {
    val uiState: StateFlow<MarketDashboardUiState> = repository.observeLatestQuotes()
        .map { quotes ->
            MarketDashboardUiState(Indicator.displayOrder.map { indicator -> MarketRowState(indicator, quotes[indicator]) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarketDashboardUiState.empty())

    fun refreshAll() {
        viewModelScope.launch {
            repository.refreshIndicators(Indicator.displayOrder, "manual")
        }
    }
}
