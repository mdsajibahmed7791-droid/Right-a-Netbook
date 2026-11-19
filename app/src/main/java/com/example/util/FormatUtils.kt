package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun localeFor(langCode: String): Locale = when (langCode) {
        "bn" -> Locale.forLanguageTag("bn-BD")
        "ar" -> Locale.forLanguageTag("ar-SA")
        else -> Locale.ENGLISH
    }

    fun formatNumber(value: Int, langCode: String): String {
        val nf = NumberFormat.getIntegerInstance(localeFor(langCode))
        return nf.format(value)
    }

    fun formatNumber(value: Long, langCode: String): String {
        val nf = NumberFormat.getIntegerInstance(localeFor(langCode))
        return nf.format(value)
    }

    fun formatDecimal(value: Double, langCode: String): String {
        val nf = NumberFormat.getNumberInstance(localeFor(langCode)).apply {
            val isWhole = kotlin.math.abs(value - kotlin.math.round(value)) < 0.005
            minimumFractionDigits = 0
            maximumFractionDigits = if (isWhole) 0 else 2
        }
        return nf.format(value)
    }

    fun formatCurrency(amount: Double, langCode: String, srUnit: String): String {
        return "${formatDecimal(amount, langCode)} $srUnit"
    }

    fun formatMonthYear(yearMonth: YearMonth, langCode: String): String {
        val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", localeFor(langCode))
        return yearMonth.atDay(1).format(formatter)
    }

    fun formatDateTime(ts: Long, langCode: String): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", localeFor(langCode))
        return sdf.format(Date(ts))
    }

    fun formatDateOnly(ts: Long, langCode: String): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", localeFor(langCode))
        return sdf.format(Date(ts))
    }

    fun formatTimeOnly(ts: Long, langCode: String): String {
        val sdf = SimpleDateFormat("HH:mm", localeFor(langCode))
        return sdf.format(Date(ts))
    }

    fun yearMonthOf(ts: Long): YearMonth {
        val zdt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
        return YearMonth.from(zdt)
    }

    fun dayOfMonthOf(ts: Long): Int {
        val zdt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
        return zdt.dayOfMonth
    }

    fun isToday(ts: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val entryDate = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
        val today = Instant.now().atZone(zone).toLocalDate()
        return entryDate == today
    }

    fun pdfFilename(prefix: String, yearMonth: YearMonth): String {
        val ym = String.format(Locale.US, "%04d-%02d", yearMonth.year, yearMonth.monthValue)
        return "$prefix-$ym.pdf"
    }
}
