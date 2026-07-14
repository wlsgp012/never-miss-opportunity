package com.nevermiss.opportunity.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponse(val chart: YahooChart)

@Serializable
data class YahooChart(
    val result: List<YahooResult>? = null,
    val error: YahooError? = null,
)

@Serializable
data class YahooError(val code: String? = null, val description: String? = null)

@Serializable
data class YahooResult(val meta: YahooMeta)

@Serializable
data class YahooMeta(
    val regularMarketPrice: Double? = null,
    val previousClose: Double? = null,
    val chartPreviousClose: Double? = null,
    val regularMarketTime: Long? = null,
)

@Serializable
data class CnnFearGreedResponse(
    @SerialName("fear_and_greed") val fearAndGreed: CnnFearGreed,
)

@Serializable
data class CnnFearGreed(
    val score: Double,
    @SerialName("previous_close") val previousClose: Double? = null,
    val timestamp: String? = null,
)

@Serializable
data class CoinMetricsAssetMetricsResponse(
    val data: List<CoinMetricsAssetMetricRow> = emptyList(),
)

@Serializable
data class CoinMetricsAssetMetricRow(
    val time: String,
    @SerialName("CapMrktCurUSD") val marketCapUsd: String? = null,
    @SerialName("CapRealUSD") val realizedCapUsd: String? = null,
)
