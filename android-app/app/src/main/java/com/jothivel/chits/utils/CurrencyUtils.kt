package com.jothivel.chits.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    /**
     * Converts an integer value in paise to a formatted Indian Rupee string.
     * Example: 100000000 -> ₹10,00,000.00 (or ₹10,00,000 if no decimals wanted)
     */
    @JvmStatic
    fun formatPaiseToRupee(paise: Int): String {
        val rupees = paise / 100.0
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        return format.format(rupees)
    }

    /**
     * Backwards compatibility for existing Java code.
     * Expects amount in Rupees (since adapters do / 100).
     */
    @JvmStatic
    fun formatInr(rupees: Int): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        return format.format(rupees.toLong())
    }
}
