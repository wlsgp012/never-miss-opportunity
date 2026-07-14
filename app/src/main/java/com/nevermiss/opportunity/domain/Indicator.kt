package com.nevermiss.opportunity.domain

enum class Indicator(
    val displayName: String,
    val yahooSymbol: String?,
    val sourceLabel: String,
) {
    USD_KRW("USD/KRW", "USDKRW=X", "Yahoo Finance"),
    CNN_FEAR_GREED("CNN Fear & Greed", null, "CNN"),
    SP500("S&P 500", "^GSPC", "Yahoo Finance"),
    NASDAQ100("Nasdaq 100", "^NDX", "Yahoo Finance"),
    KRX_GOLD_1G("KRX gold spot 1g", null, "KRX public web"),
    KOSPI("KOSPI", "^KS11", "Yahoo Finance");

    companion object {
        val displayOrder = listOf(USD_KRW, CNN_FEAR_GREED, SP500, NASDAQ100, KRX_GOLD_1G, KOSPI)
    }
}
