package com.nevermiss.opportunity.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nevermiss.opportunity.data.MarketDataProvider
import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class YahooFinanceProvider(
    baseUrl: String = "https://query1.finance.yahoo.com/",
    client: OkHttpClient = OkHttpClient(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : MarketDataProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(YahooApi::class.java)

    override suspend fun fetch(indicator: Indicator): ProviderResult {
        val symbol = indicator.yahooSymbol
            ?: return ProviderResult.Failure(indicator, "missing-yahoo-symbol", retryable = false)

        return runCatching {
            val response = api.chart(symbol)
            val meta = response.chart.result?.firstOrNull()?.meta
                ?: return ProviderResult.Failure(indicator, "empty-chart", retryable = true)
            val value = meta.regularMarketPrice
                ?: return ProviderResult.Failure(indicator, "missing-price", retryable = true)
            val previous = meta.previousClose ?: meta.chartPreviousClose ?: value
            val change = value - previous
            val percent = if (previous == 0.0) null else (change / previous) * 100.0
            ProviderResult.Success(
                MarketQuote(
                    indicator = indicator,
                    value = value,
                    change = change,
                    percentChange = percent,
                    observedAtEpochMillis = (meta.regularMarketTime?.times(1000)) ?: clockMillis(),
                    source = indicator.sourceLabel,
                    state = QuoteState.FRESH,
                ),
            )
        }.getOrElse {
            ProviderResult.Failure(indicator, it.javaClass.simpleName, retryable = true)
        }
    }
}

private interface YahooApi {
    @GET("v8/finance/chart/{symbol}")
    suspend fun chart(
        @Path("symbol", encoded = true) symbol: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "1m",
    ): YahooChartResponse
}
