package com.nevermiss.opportunity.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDao {
    @Insert
    suspend fun insertQuote(entity: QuoteEntity)

    @Insert
    suspend fun insertFetchLog(entity: FetchLogEntity)

    @Query(
        """
        SELECT * FROM indicator_quotes
        WHERE id IN (
            SELECT MAX(id) FROM indicator_quotes GROUP BY indicator
        )
        """,
    )
    fun observeLatestQuotes(): Flow<List<QuoteEntity>>

    @Query(
        """
        SELECT * FROM indicator_quotes
        WHERE id IN (
            SELECT MAX(id) FROM indicator_quotes GROUP BY indicator
        )
        """,
    )
    suspend fun latestQuotes(): List<QuoteEntity>
}
