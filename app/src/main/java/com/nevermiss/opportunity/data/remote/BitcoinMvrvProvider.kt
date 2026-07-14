package com.nevermiss.opportunity.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nevermiss.opportunity.data.MarketDataProvider
import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import java.time.Instant
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

class BitcoinMvrvProvider(
    baseUrl: String = "https://community-api.coinmetrics.io/",
    client: OkHttpClient = OkHttpClient(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : MarketDataProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CoinMetricsApi::class.java)

    override suspend fun fetch(indicator: Indicator): ProviderResult {
        if (indicator != Indicator.BTC_MVRV) {
            return ProviderResult.Failure(indicator, "unsupported-indicator", retryable = false)
        }

        return runCatching {
            val rows = api.assetMetrics().data
                .mapNotNull { row -> row.toMvrvPoint() }
                .sortedBy { it.observedAtEpochMillis }
            val latest = rows.lastOrNull()
                ?: return ProviderResult.Failure(indicator, "missing-mvrv", retryable = false)
            val previous = rows.dropLast(1).lastOrNull()
            val change = previous?.let { latest.value - it.value } ?: 0.0
            val percent = previous?.value?.takeIf { it != 0.0 }?.let { change / it * 100.0 }

            ProviderResult.Success(
                MarketQuote(
                    indicator = indicator,
                    value = latest.value,
                    change = change,
                    percentChange = percent,
                    observedAtEpochMillis = latest.observedAtEpochMillis,
                    source = indicator.sourceLabel,
                    state = QuoteState.FRESH,
                ),
            )
        }.getOrElse { ProviderFailureClassifier.failureFor(indicator, it) }
    }
}

private data class MvrvPoint(
    val value: Double,
    val observedAtEpochMillis: Long,
)

private fun CoinMetricsAssetMetricRow.toMvrvPoint(): MvrvPoint? {
    val mvrv = mvrv?.toDoubleOrNull() ?: return null
    val observedAt = runCatching { Instant.parse(time).toEpochMilli() }.getOrNull() ?: return null
    return MvrvPoint(value = mvrv, observedAtEpochMillis = observedAt)
}

private interface CoinMetricsApi {
    @GET("v4/timeseries/asset-metrics")
    suspend fun assetMetrics(
        @Query("assets") assets: String = "btc",
        @Query("metrics") metrics: String = "CapMVRVCur",
        @Query("page_size") pageSize: Int = 7,
    ): CoinMetricsAssetMetricsResponse
}
