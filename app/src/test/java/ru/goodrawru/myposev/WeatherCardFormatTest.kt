package ru.goodrawru.myposev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCardFormatTest {
    @Test fun actualDatesBecomeHeadingsInsteadOfRepeatedForecastLabel() {
        val summary = "Сб, 7 нояб.\n🌧️ Дождь\n🌡 2…3 °C\n\nВс, 8 нояб.\n☁️ Облачно\n🌡 0…5 °C"
        val entries = WeatherCardFormat.entries(summary)
        assertEquals(2, entries.size)
        assertEquals("Сб, 7 нояб.", entries[0].title)
        assertEquals("Вс, 8 нояб.", entries[1].title)
        assertFalse(entries[0].description.startsWith("Сб, 7 нояб."))
        assertTrue(entries[1].description.contains("☁️ Облачно"))
    }

    @Test fun weatherWarningIsSeparateFromDayCards() {
        val summary = "Пн, 9 нояб.\n☀️ Ясно\n\n⚠️ В один или несколько дней возможна температура 0 °C или ниже."
        val entries = WeatherCardFormat.entries(summary)
        assertEquals(2, entries.size)
        assertFalse(entries[0].warning)
        assertTrue(entries[1].warning)
        assertEquals("Предупреждение о температуре", entries[1].title)
        assertEquals("Temperature warning", WeatherCardFormat.entries(summary, english = true)[1].title)
    }

    @Test fun emptyAndLegacySingleLineSummariesAreSafe() {
        assertTrue(WeatherCardFormat.entries(" \n\n ").isEmpty())
        val entry = WeatherCardFormat.entries("Сб, 7 нояб.").single()
        assertEquals("Сб, 7 нояб.", entry.title)
        assertEquals("Нет подробностей", entry.description)
    }
}
