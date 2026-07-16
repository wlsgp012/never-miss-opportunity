package com.nevermiss.opportunity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertScheduleFormatterTest {
    @Test
    fun `parses hour minute time`() {
        assertEquals(9 * 3600 + 30 * 60, AlertScheduleFormatter.parseSecondOfDay("09:30"))
    }

    @Test
    fun `parses hour minute second time`() {
        assertEquals(9 * 3600 + 30 * 60 + 15, AlertScheduleFormatter.parseSecondOfDay("09:30:15"))
    }

    @Test
    fun `rejects invalid time`() {
        assertNull(AlertScheduleFormatter.parseSecondOfDay("25:00"))
    }

    @Test
    fun `formats second of day`() {
        assertEquals("09:30:15", AlertScheduleFormatter.formatSecondOfDay(9 * 3600 + 30 * 60 + 15))
    }
}
