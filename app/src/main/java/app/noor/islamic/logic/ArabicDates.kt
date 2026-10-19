package app.noor.islamic.logic

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** Arabic names and the Umm al-Qura Hijri calendar (built into java.time, no library needed). */
object ArabicDates {
    // Levantine names for the Gregorian months, as used in Syria / Lebanon / Jordan / Palestine.
    val gregorianMonths = listOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
        "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول",
    )
    val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )
    private val weekdays = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

    fun weekday(date: LocalDate): String = weekdays[date.dayOfWeek.value - 1]

    /** "الخميس، 15 تشرين الأول 2026" (digits are converted by the caller). */
    fun gregorian(date: LocalDate): String =
        "${weekday(date)}، ${date.dayOfMonth} ${gregorianMonths[date.monthValue - 1]} ${date.year}"

    class Hijri(val year: Int, val month: Int, val day: Int) {
        val monthName: String get() = hijriMonths[month - 1]
        override fun toString(): String = "$day $monthName $year هـ"
    }

    /** [adjust] shifts the civil date by a few days to follow local moon sighting. */
    fun hijri(date: LocalDate, adjust: Int = 0): Hijri? = try {
        val h = HijrahDate.from(date.plusDays(adjust.toLong()))
        Hijri(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
    } catch (e: Exception) {
        null
    }

    private const val EASTERN = "٠١٢٣٤٥٦٧٨٩"

    fun toEastern(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') EASTERN[c - '0'] else c)
        return sb.toString()
    }
}
