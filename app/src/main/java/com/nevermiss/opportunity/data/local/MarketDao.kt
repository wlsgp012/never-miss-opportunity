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
        SELECT * FROM indicator_quotes AS quote
        WHERE NOT EXISTS (
            SELECT 1 FROM indicator_quotes AS newer
            WHERE newer.indicator = quote.indicator
              AND (
                newer.observedAtEpochMillis > quote.observedAtEpochMillis
                OR (
                    newer.observedAtEpochMillis = quote.observedAtEpochMillis
                    AND newer.id > quote.id
                )
              )
        )
        """,
    )
    fun observeLatestQuotes(): Flow<List<QuoteEntity>>

    @Query(
        """
        SELECT * FROM indicator_quotes AS quote
        WHERE NOT EXISTS (
            SELECT 1 FROM indicator_quotes AS newer
            WHERE newer.indicator = quote.indicator
              AND (
                newer.observedAtEpochMillis > quote.observedAtEpochMillis
                OR (
                    newer.observedAtEpochMillis = quote.observedAtEpochMillis
                    AND newer.id > quote.id
                )
              )
        )
        """,
    )
    suspend fun latestQuotes(): List<QuoteEntity>
}
