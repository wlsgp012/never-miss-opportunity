package com.nevermiss.opportunity.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MovementFormatter {
    private val valueFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val signedFormat = DecimalFormat("+#,##0.00;-#,##0.00", DecimalFormatSymbols(Locale.US))

    fun value(quote: MarketQuote): String = valueFormat.format(quote.value)

    fun movement(quote: MarketQuote): String {
        val change = signedFormat.format(quote.change)
        val percent = quote.percentChange?.let { " (${signedFormat.format(it)}%)" }.orEmpty()
        return change + percent
    }
}
