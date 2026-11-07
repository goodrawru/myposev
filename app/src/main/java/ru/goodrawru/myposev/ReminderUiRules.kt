package ru.goodrawru.myposev

import java.util.Locale

/**
 * Reminder screen presentation: keep persisted crop IDs unchanged.
 */
internal object ReminderUiRules {
    fun recommendationLimit(count: Int, english: Boolean): String {
        if (english) return "$count " + if (count == 1) "item" else "items"
        val lastTwo = count % 100
        val ending = when {
            lastTwo in 11..14 -> "пунктов"
            count % 10 == 1 -> "пункт"
            count % 10 in 2..4 -> "пункта"
            else -> "пунктов"
        }
        return "$count $ending"
    }

    fun orderedCrops(crops: List<Pair<String, String>>, favorites: Set<String>): List<Pair<String, String>> =
        crops.distinctBy { it.first }.sortedWith(
            compareBy<Pair<String, String>> { it.first !in favorites }
                .thenBy { it.second.lowercase(Locale.ROOT) }
        )

    fun toggleFavorite(current: Set<String>, id: String): Set<String> =
        if (id in current) current - id else current + id
}
