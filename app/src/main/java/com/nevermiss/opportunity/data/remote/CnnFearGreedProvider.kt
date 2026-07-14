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
import java.time.Instant
import java.util.Locale

class CnnFearGreedProvider(
    baseUrl: String = "https://production.dataviz.cnn.io/",
    client: OkHttpClient = OkHttpClient(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : MarketDataProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val apiClient = client.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", CNN_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9,ko;q=0.8")
                .header("Origin", "https://www.cnn.com")
                .header("Referer", "https://www.cnn.com/markets/fear-and-greed")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .build()
            chain.proceed(request)
        }
        .build()
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(apiClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CnnApi::class.java)

    override suspend fun fetch(indicator: Indicator): ProviderResult {
        if (indicator != Indicator.CNN_FEAR_GREED) {
            return ProviderResult.Failure(indicator, "unsupported-indicator", retryable = false)
        }

        return runCatching {
            val payload = api.graphData()
            val previous = payload.fearAndGreed.previousClose ?: payload.fearAndGreed.score
            val change = payload.fearAndGreed.score - previous
            ProviderResult.Success(
                MarketQuote(
                    indicator = indicator,
                    value = payload.fearAndGreed.score,
                    change = change,
                    percentChange = null,
                    observedAtEpochMillis = payload.fearAndGreed.timestamp?.let { Instant.parse(it).toEpochMilli() } ?: clockMillis(),
                    source = indicator.sourceLabel,
                    state = QuoteState.FRESH,
                    signalLabel = payload.fearAndGreed.rating?.toDisplayLabel(),
                ),
            )
        }.getOrElse { ProviderFailureClassifier.failureFor(indicator, it) }
    }
}

private const val CNN_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

private fun String.toDisplayLabel(): String =
    trim()
        .lowercase(Locale.US)
        .split(Regex("\\s+"))
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }

private interface CnnApi {
    @GET("index/fearandgreed/graphdata")
    suspend fun graphData(): CnnFearGreedResponse
}
