package com.ehan.rupiahku.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val idLocale = Locale("id", "ID")

    fun formatDate(timeMillis: Long, formatPattern: String = "dd MMM yyyy"): String {
        val sdf = SimpleDateFormat(formatPattern, idLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatShortDate(timeMillis: Long): String {
        return formatDate(timeMillis, "dd MMM yyyy")
    }

    fun formatDateTime(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
        return sdf.format(Date(timeMillis))
    }

    fun getBillStatusLabel(dueDateMillis: Long, isPaid: Boolean): BillStatus {
        if (isPaid) return BillStatus.PAID

        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val due = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diffDays = ((due - now) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> BillStatus.OVERDUE
            diffDays == 0 -> BillStatus.DUE_TODAY
            diffDays <= 3 -> BillStatus.DUE_SOON
            else -> BillStatus.UPCOMING
        }
    }

    enum class BillStatus(val label: String) {
        PAID("Lunas"),
        DUE_TODAY("Jatuh Tempo Hari Ini"),
        OVERDUE("Terlambat"),
        DUE_SOON("Mendatang (Segera)"),
        UPCOMING("Mendatang")
    }
}
