package com.nevermiss.opportunity.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState

@Entity(tableName = "indicator_quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val indicator: Indicator,
    val value: Double,
    val change: Double,
    val percentChange: Double?,
    val observedAtEpochMillis: Long,
    val source: String,
    val state: QuoteState,
)

fun QuoteEntity.toDomain(): MarketQuote =
    MarketQuote(indicator, value, change, percentChange, observedAtEpochMillis, source, state)

fun MarketQuote.toEntity(): QuoteEntity =
    QuoteEntity(
        indicator = indicator,
        value = value,
        change = change,
        percentChange = percentChange,
        observedAtEpochMillis = observedAtEpochMillis,
        source = source,
        state = state,
    )
