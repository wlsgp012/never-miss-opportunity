package com.nevermiss.opportunity.data.local

import androidx.room.Room
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.QuoteState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MarketDaoTest {
    @Test
    fun `latest quote uses observation time instead of insertion order`() = runTest {
        withDatabase { dao ->
            dao.insertQuote(quote(value = 200.0, observedAtEpochMillis = 2_000))
            dao.insertQuote(quote(value = 100.0, observedAtEpochMillis = 1_000))

            assertEquals(200.0, dao.latestQuotes().single().value, 0.001)
            assertEquals(200.0, dao.observeLatestQuotes().first().single().value, 0.001)
        }
    }

    @Test
    fun `latest quote breaks equal observation time ties by id`() = runTest {
        withDatabase { dao ->
            dao.insertQuote(quote(value = 100.0, observedAtEpochMillis = 2_000))
            dao.insertQuote(quote(value = 200.0, observedAtEpochMillis = 2_000))

            assertEquals(200.0, dao.latestQuotes().single().value, 0.001)
        }
    }

    private suspend fun withDatabase(block: suspend (MarketDao) -> Unit) {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            block(database.marketDao())
        } finally {
            database.close()
        }
    }

    private fun quote(value: Double, observedAtEpochMillis: Long) = QuoteEntity(
        indicator = Indicator.KOSPI,
        value = value,
        change = 0.0,
        percentChange = 0.0,
        observedAtEpochMillis = observedAtEpochMillis,
        source = "Test",
        state = QuoteState.FRESH,
    )
}
