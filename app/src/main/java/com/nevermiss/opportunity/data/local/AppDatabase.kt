package com.nevermiss.opportunity.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.QuoteState

@Database(
    entities = [QuoteEntity::class, FetchLogEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(MarketConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun marketDao(): MarketDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE indicator_quotes ADD COLUMN signalLabel TEXT")
                db.execSQL("ALTER TABLE indicator_quotes ADD COLUMN rsi REAL")
            }
        }
    }
}

class MarketConverters {
    @TypeConverter fun indicatorToString(value: Indicator): String = value.name
    @TypeConverter fun stringToIndicator(value: String): Indicator = Indicator.valueOf(value)
    @TypeConverter fun stateToString(value: QuoteState): String = value.name
    @TypeConverter fun stringToState(value: String): QuoteState = QuoteState.valueOf(value)
}
