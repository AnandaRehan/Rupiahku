package com.ehan.rupiahku.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double, withPrefix: Boolean = true): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(indonesianLocale)
            formatter.maximumFractionDigits = 0
            var formatted = formatter.format(amount)
            // Clean formatting space if needed
            formatted = formatted.replace("Rp", "Rp ").replace("Rp  ", "Rp ")
            if (!withPrefix) {
                formatted = formatted.replace("Rp ", "").trim()
            }
            formatted
        } catch (e: Exception) {
            val rounded = amount.toLong()
            val formattedString = "%,d".format(rounded).replace(",", ".")
            if (withPrefix) "Rp $formattedString" else formattedString
        }
    }

    fun formatRupiahCompact(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format(Locale.getDefault(), "Rp %.1f M", amount / 1_000_000_000)
            amount >= 1_000_000 -> String.format(Locale.getDefault(), "Rp %.1f Jt", amount / 1_000_000)
            amount >= 1_000 -> String.format(Locale.getDefault(), "Rp %.0f Rb", amount / 1_000)
            else -> formatRupiah(amount)
        }
    }

    fun parseRupiahInput(input: String): Double {
        val cleanString = input.replace("[^0-9]".toRegex(), "")
        return cleanString.toDoubleOrNull() ?: 0.0
    }
}
