package ru.goodrawru.myposev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPeriodTest {
    @Test
    fun winterCrossesCalendarYear() {
        assertEquals(listOf(11, 0, 1), PdfPeriod.months(4))
        assertEquals(2026, PdfPeriod.yearFor(2027, 4, 11))
        assertEquals(2027, PdfPeriod.yearFor(2027, 4, 0))
        assertTrue(PdfPeriod.includesDate(2027, 4, "2026-12-15"))
        assertTrue(PdfPeriod.includesDate(2027, 4, "2027-01-01"))
        assertTrue(PdfPeriod.includesDate(2027, 4, "2027-02-28"))
        assertFalse(PdfPeriod.includesDate(2027, 4, "2027-12-15"))
        assertFalse(PdfPeriod.includesDate(2027, 4, "2027-03-01"))
        assertFalse(PdfPeriod.includesDate(2027, 4, "2026-02-01"))
    }

    @Test
    fun fullYearDoesNotIncludePreviousDecember() {
        assertEquals((0..11).toList(), PdfPeriod.months(0))
        assertTrue(PdfPeriod.includesDate(2027, 0, "2027-01-01"))
        assertTrue(PdfPeriod.includesDate(2027, 0, "2027-12-31"))
        assertFalse(PdfPeriod.includesDate(2027, 0, "2026-12-31"))
        assertFalse(PdfPeriod.includesDate(2027, 0, "2028-01-01"))
    }

    @Test
    fun allThreeOtherSeasonsUseRequestedYear() {
        assertEquals(listOf(2, 3, 4), PdfPeriod.months(1))
        assertEquals(listOf(5, 6, 7), PdfPeriod.months(2))
        assertEquals(listOf(8, 9, 10), PdfPeriod.months(3))
        assertTrue(PdfPeriod.includesDate(2027, 1, "2027-03-01"))
        assertTrue(PdfPeriod.includesDate(2027, 1, "2027-05-31"))
        assertTrue(PdfPeriod.includesDate(2027, 2, "2027-06-01"))
        assertTrue(PdfPeriod.includesDate(2027, 2, "2027-08-31"))
        assertTrue(PdfPeriod.includesDate(2027, 3, "2027-09-01"))
        assertTrue(PdfPeriod.includesDate(2027, 3, "2027-11-30"))
        assertFalse(PdfPeriod.includesDate(2027, 1, "2027-06-01"))
        assertFalse(PdfPeriod.includesDate(2027, 3, "2026-11-30"))
    }

    @Test
    fun rejectsMalformedAndImpossibleDates() {
        for (value in listOf("", "2027-2-01", "2027-02-1", "2027-02-29",
            "2027-02-30", "2027-13-01", "2027-00-01", "2027-04-31",
            "not-a-date", "2027-01-01 extra")) {
            assertFalse("Accepted invalid date: $value", PdfPeriod.includesDate(2027, 0, value))
        }
        assertTrue(PdfPeriod.includesDate(2028, 0, "2028-02-29"))
        assertTrue(PdfPeriod.includesDate(2028, 4, "2028-02-29"))
        assertFalse(PdfPeriod.includesDate(2027, 4, "2027-02-29"))
    }

    @Test
    fun winterAtEarliestSelectableYearIncludesPreviousDecember() {
        assertTrue(PdfPeriod.includesDate(2000, 4, "1999-12-31"))
        assertTrue(PdfPeriod.includesDate(2000, 4, "2000-02-29"))
        assertFalse(PdfPeriod.includesDate(2000, 4, "2000-12-31"))
    }
}
