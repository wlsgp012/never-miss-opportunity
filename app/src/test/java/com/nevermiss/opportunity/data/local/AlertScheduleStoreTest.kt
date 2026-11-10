package com.nevermiss.opportunity.data.local

import android.content.Context
import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.AlertScheduleRuleJson
import com.nevermiss.opportunity.domain.Indicator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlertScheduleStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val preferences get() = context.getSharedPreferences("alert-schedule-rules", Context.MODE_PRIVATE)

    @Before fun clearPreferences() { preferences.edit().clear().commit() }

    @Test fun `upgrade preserves custom rules and adds oil only once`() {
        val custom = AlertScheduleRule.fixed("custom", Indicator.KOSPI, 36000, enabled = false)
        preferences.edit().putString("rules", AlertScheduleRuleJson.exportRules(listOf(custom))).commit()
        val store = SharedPreferencesAlertScheduleStore(context)
        assertEquals(custom, store.rule("custom"))
        val oil = store.rules().single { it.indicator.name == "WTI_CRUDE" }
        assertEquals(32400, oil.fixedSecondOfDay)
        assertTrue(oil.enabled)
        assertEquals(2, store.rules().size)
        store.deleteRule(oil.id)
        assertEquals(listOf(custom), SharedPreferencesAlertScheduleStore(context).rules())
    }

    @Test fun `upgrade preserves existing disabled oil rule without adding a duplicate`() {
        val oil = AlertScheduleRule.fixed("custom-oil", Indicator.WTI_CRUDE, 36000, enabled = false)
        preferences.edit().putString("rules", AlertScheduleRuleJson.exportRules(listOf(oil))).commit()
        assertEquals(listOf(oil), SharedPreferencesAlertScheduleStore(context).rules())
    }

    @Test fun `import excluding oil remains unchanged on reopening`() {
        val store = SharedPreferencesAlertScheduleStore(context)
        assertTrue(store.importJson(AlertScheduleRuleJson.exportRules(emptyList())))
        assertTrue(SharedPreferencesAlertScheduleStore(context).rules().isEmpty())
    }

    @Test fun `fresh install oil deletion survives reopening`() {
        val store = SharedPreferencesAlertScheduleStore(context)
        val oil = store.rules().single { it.indicator.name == "WTI_CRUDE" }
        store.deleteRule(oil.id)
        assertFalse(SharedPreferencesAlertScheduleStore(context).rules().any { it.id == oil.id })
    }
}
