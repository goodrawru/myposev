package ru.goodrawru.myposev

/**
 * Presentation-only formatter for cached 7Timer daily summaries.
 *
 * The saved weather_summary_7timer format stays unchanged, including entries
 * created by earlier releases. The first line of each daily entry becomes the
 * card heading (the actual date), rather than showing a fixed calendar emoji.
 */
internal object WeatherCardFormat {
    data class Entry(val title: String, val description: String, val warning: Boolean)

    fun entries(summary: String, english: Boolean = false): List<Entry> =
        summary.trim().split(Regex("\\n\\s*\\n"))
            .filter { it.isNotBlank() }
            .mapNotNull { block ->
                val text = block.trim()
                if (text.startsWith("⚠")) {
                    Entry(
                        if (english) "Temperature warning" else "Предупреждение о температуре",
                        text, true
                    )
                } else {
                    val day = text.substringBefore('\n').trim()
                    val details = text.substringAfter('\n', "").trim()
                    if (day.isEmpty()) null
                    else Entry(
                        day,
                        details.ifBlank { if (english) "No details" else "Нет подробностей" },
                        false
                    )
                }
            }
}