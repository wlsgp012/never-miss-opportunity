package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.MarketDataProvider
import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.QuoteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime

class GoldSpotProvider(
    private val baseUrl: String = "https://m.stock.naver.com/",
    private val client: OkHttpClient = OkHttpClient(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : MarketDataProvider {
    override suspend fun fetch(indicator: Indicator): ProviderResult {
        if (indicator != Indicator.KRX_GOLD_1G) {
            return ProviderResult.Failure(indicator, "unsupported-indicator", retryable = false)
        }

        return runCatching {
            val body = fetchGoldBody()
            if (body == "LOGOUT") {
                return ProviderResult.Failure(indicator, "krx-logout", retryable = true)
            }
            val quote = parseGoldQuote(body)
                ?: return ProviderResult.Failure(indicator, "missing-gold-price", retryable = false)
            ProviderResult.Success(quote)
        }.getOrElse {
            if (it is KrxHttpException) {
                ProviderResult.Failure(
                    indicator = indicator,
                    reason = "http-${it.statusCode}",
                    retryable = it.statusCode == 429 || it.statusCode in 500..599,
                )
            } else {
                ProviderFailureClassifier.failureFor(indicator, it)
            }
        }
    }

    private suspend fun fetchGoldBody(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/front-api/marketIndex/productDetail?category=metals&reutersCode=M04020000")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw KrxHttpException(response.code)
            }
            response.body?.string()?.trim().orEmpty()
        }
    }

    private fun parseGoldQuote(body: String): MarketQuote? {
        val payload = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
        val row = payload.krxRows().firstOrNull() ?: return null
        val value = row.number("closePrice", "TDD_CLSPRC", "tradeBasPrc", "clpr") ?: return null
        val rawChange = row.number("fluctuations", "CMPPREVDD_PRC", "vs") ?: 0.0
        val change = rawChange.withKrxDirection(row.text("fluctuationsType.code", "FLUC_TP_CD", "upDownType"))
        val percent = row.number("fluctuationsRatio", "FLUC_RT", "fltRt")
        val observedAt = row.text("localTradedAt")?.toEpochMillisOrNull() ?: clockMillis()

        return MarketQuote(
            indicator = Indicator.KRX_GOLD_1G,
            value = value,
            change = change,
            percentChange = percent,
            observedAtEpochMillis = observedAt,
            source = "Naver Finance (KRX)",
            state = QuoteState.FRESH,
        )
    }

    private class KrxHttpException(val statusCode: Int) : RuntimeException("http-$statusCode")

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Android) NeverMissOpportunity/1.0"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

private fun JsonElement.krxRows(): List<JsonObject> =
    when (this) {
        is JsonObject -> {
            val directResult = this["result"]?.runCatchingJsonObject()
            val nestedRows = listOf("output", "OutBlock_1", "block1", "data")
                .firstNotNullOfOrNull { key ->
                    this[key]?.runCatchingJsonArrayObjects()
                }
            nestedRows ?: listOfNotNull(directResult ?: this)
        }
        else -> emptyList()
    }

private fun JsonElement.runCatchingJsonObject(): JsonObject? =
    runCatching { jsonObject }.getOrNull()

private fun JsonElement.runCatchingJsonArrayObjects(): List<JsonObject>? =
    runCatching { jsonArray.mapNotNull { it as? JsonObject } }.getOrNull()

private fun JsonObject.number(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { key -> text(key)?.toMarketNumberOrNull() }

private fun JsonObject.text(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
        key.split('.').fold(this as JsonElement?) { current, part ->
            current?.runCatchingJsonObject()?.get(part)
        }?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

private fun String.toMarketNumberOrNull(): Double? {
    val normalized = replace(",", "")
        .replace("%", "")
        .trim()
    return normalized.toDoubleOrNull()
}

private fun Double.withKrxDirection(directionCode: String?): Double =
    when (directionCode) {
        "4", "5", "-", "down", "DOWN", "FALLING", "LOWER_LIMIT" -> -kotlin.math.abs(this)
        else -> this
    }

private fun String.toEpochMillisOrNull(): Long? =
    runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }.getOrNull()
