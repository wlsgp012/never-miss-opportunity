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
                ?: return ProviderResult.Failure(indicator, "empty-chart", retryable = false)
            val value = meta.regularMarketPrice
                ?: return ProviderResult.Failure(indicator, "missing-price", retryable = false)
            val previous = meta.previousClose ?: meta.chartPreviousClose ?: value
            val change = value - previous
            val percent = if (previous == 0.0) null else (change / previous) * 100.0
            val rsi = if (indicator.supportsRsi) runCatching {
                api.chart(symbol, range = "1mo", interval = "1d")
                    .chart
                    .result
                    ?.firstOrNull()
                    ?.closeValues()
                    ?.let { calculateRsi(it) }
            }.getOrNull() else {
                null
            }
            ProviderResult.Success(
                MarketQuote(
                    indicator = indicator,
                    value = value,
                    change = change,
                    percentChange = percent,
                    observedAtEpochMillis = (meta.regularMarketTime?.times(1000)) ?: clockMillis(),
                    source = indicator.sourceLabel,
                    state = QuoteState.FRESH,
                    rsi = rsi,
                ),
            )
        }.getOrElse { ProviderFailureClassifier.failureFor(indicator, it) }
    }
}

private val Indicator.supportsRsi: Boolean
    get() = this == Indicator.SP500 ||
        this == Indicator.NASDAQ100 ||
        this == Indicator.KOSPI ||
        this == Indicator.BTC_USD

private fun YahooResult.closeValues(): List<Double> =
    indicators
        ?.quote
        ?.firstOrNull()
        ?.close
        ?.mapNotNull { it }
        .orEmpty()

private fun calculateRsi(closes: List<Double>, period: Int = 14): Double? {
    if (closes.size <= period) return null

    val deltas = closes.zipWithNext { previous, current -> current - previous }
    var averageGain = deltas.take(period).sumOf { if (it > 0.0) it else 0.0 } / period
    var averageLoss = deltas.take(period).sumOf { if (it < 0.0) -it else 0.0 } / period

    deltas.drop(period).forEach { delta ->
        val gain = if (delta > 0.0) delta else 0.0
        val loss = if (delta < 0.0) -delta else 0.0
        averageGain = ((averageGain * (period - 1)) + gain) / period
        averageLoss = ((averageLoss * (period - 1)) + loss) / period
    }

    return when {
        averageLoss == 0.0 && averageGain == 0.0 -> 50.0
        averageLoss == 0.0 -> 100.0
        averageGain == 0.0 -> 0.0
        else -> {
            val relativeStrength = averageGain / averageLoss
            100.0 - (100.0 / (1.0 + relativeStrength))
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
