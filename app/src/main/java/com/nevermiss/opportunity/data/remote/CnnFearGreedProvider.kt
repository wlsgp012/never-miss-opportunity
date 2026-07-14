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

class CnnFearGreedProvider(
    baseUrl: String = "https://production.dataviz.cnn.io/",
    client: OkHttpClient = OkHttpClient(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : MarketDataProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
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
                ),
            )
        }.getOrElse {
            ProviderResult.Failure(indicator, it.javaClass.simpleName, retryable = true)
        }
    }
}

private interface CnnApi {
    @GET("index/fearandgreed/graphdata")
    suspend fun graphData(): CnnFearGreedResponse
}
