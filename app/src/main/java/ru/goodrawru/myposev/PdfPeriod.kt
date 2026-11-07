package ru.goodrawru.myposev

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Calendar months use Calendar's zero-based numbering. Winter of [year] starts in
 * December of [year - 1], followed by January and February of [year].
 * Keeping this logic independent of Activity makes date filtering testable.
 */
internal object PdfPeriod {
    fun months(periodIndex: Int): List<Int> = when (periodIndex) {
        1 -> listOf(2, 3, 4)
        2 -> listOf(5, 6, 7)
        3 -> listOf(8, 9, 10)
        4 -> listOf(11, 0, 1)
        else -> (0..11).toList()
    }

    fun yearFor(year: Int, periodIndex: Int, zeroBasedMonth: Int): Int =
        if (periodIndex == 4 && zeroBasedMonth == 11) year - 1 else year

    fun includesDate(year: Int, periodIndex: Int, date: String): Boolean {
        if (!DATE_PATTERN.matches(date)) return false
        val parsed = try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            return false
        }
        val month = parsed.monthValue - 1
        return month in months(periodIndex) && parsed.year == yearFor(year, periodIndex, month)
    }

    private val DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
}
