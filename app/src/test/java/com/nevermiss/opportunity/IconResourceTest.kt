package com.nevermiss.opportunity

import org.junit.Assert.assertNotEquals
import org.junit.Test

class IconResourceTest {
    @Test
    fun `app and notification icons are present`() {
        assertNotEquals(0, R.drawable.ic_launcher_foreground)
        assertNotEquals(0, R.drawable.ic_stat_market)
        assertNotEquals(0, R.mipmap.ic_launcher)
        assertNotEquals(0, R.mipmap.ic_launcher_round)
    }
}
