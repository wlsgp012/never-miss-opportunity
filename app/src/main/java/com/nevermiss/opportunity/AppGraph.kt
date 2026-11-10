package com.nevermiss.opportunity

import android.content.Context
import androidx.room.Room
import com.nevermiss.opportunity.data.DefaultMarketRepository
import com.nevermiss.opportunity.data.MarketDataProvider
import com.nevermiss.opportunity.data.local.SharedPreferencesAlertScheduleStore
import com.nevermiss.opportunity.data.local.AppDatabase
import com.nevermiss.opportunity.data.remote.BitcoinMvrvProvider
import com.nevermiss.opportunity.data.remote.CnnFearGreedProvider
import com.nevermiss.opportunity.data.remote.GoldSpotProvider
import com.nevermiss.opportunity.data.remote.YahooFinanceProvider
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.notifications.MarketNotificationManager
import com.nevermiss.opportunity.scheduler.MarketAlarmScheduler

class AppGraph private constructor(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, "market-alerts.db")
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    private val yahooProvider = YahooFinanceProvider()
    private val cnnProvider = CnnFearGreedProvider()
    private val goldProvider = GoldSpotProvider()
    private val bitcoinMvrvProvider = BitcoinMvrvProvider()

    private val routedProvider = MarketDataProvider { indicator ->
        val provider = when (indicator) {
            Indicator.CNN_FEAR_GREED -> cnnProvider
            Indicator.KRX_GOLD_1G -> goldProvider
            Indicator.BTC_MVRV -> bitcoinMvrvProvider
            Indicator.USD_KRW,
            Indicator.SP500,
            Indicator.NASDAQ100,
            Indicator.VIX,
            Indicator.BTC_USD,
            Indicator.WTI_CRUDE,
            Indicator.KOSPI -> yahooProvider
        }
        provider.fetch(indicator)
    }

    val repository = DefaultMarketRepository(database.marketDao(), routedProvider)
    val notifications = MarketNotificationManager(appContext)
    val scheduleStore = SharedPreferencesAlertScheduleStore(appContext)
    val scheduler = MarketAlarmScheduler(appContext, scheduleStore)

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context).also { instance = it }
            }
    }
}
