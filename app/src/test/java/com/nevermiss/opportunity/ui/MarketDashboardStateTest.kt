package com.nevermiss.opportunity.ui

import com.nevermiss.opportunity.domain.Indicator
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketDashboardStateTest {
    @Test
    fun `empty rows preserve fixed indicator order`() {
        val rows = MarketDashboardUiState.empty().rows.map { it.indicator }

        assertEquals(Indicator.displayOrder, rows)
    }

    @Test
    fun `empty row status includes its configured source and unavailable state`() {
        val row = MarketRowState(Indicator.USD_KRW, quote = null)

        assertEquals(
            "Unavailable · ${Indicator.USD_KRW.sourceLabel}",
            row.statusText(),
        )
    }
}
