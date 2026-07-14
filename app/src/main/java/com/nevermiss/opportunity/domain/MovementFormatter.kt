package com.nevermiss.opportunity.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MovementFormatter {
    private fun valueFormat() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private fun signedFormat() = DecimalFormat("+#,##0.00;-#,##0.00", DecimalFormatSymbols(Locale.US))

    fun value(quote: MarketQuote): String = valueFormat().format(quote.value)

    fun rsi(value: Double): String = DecimalFormat("#0.0", DecimalFormatSymbols(Locale.US)).format(value)

    fun movement(quote: MarketQuote): String {
        val change = signedFormat().format(quote.change)
        val percent = quote.percentChange?.let { " (${signedFormat().format(it)}%)" }.orEmpty()
        return change + percent
    }
}
