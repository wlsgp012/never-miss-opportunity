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
}
