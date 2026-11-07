package ru.goodrawru.myposev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUiRulesTest {
    @Test fun russianRecommendationCountDeclinesProperly() {
        val values = mapOf(
            1 to "1 пункт", 2 to "2 пункта", 3 to "3 пункта",
            4 to "4 пункта", 5 to "5 пунктов", 6 to "6 пунктов",
            11 to "11 пунктов", 12 to "12 пунктов", 14 to "14 пунктов",
            21 to "21 пункт", 22 to "22 пункта", 25 to "25 пунктов",
            101 to "101 пункт", 111 to "111 пунктов"
        )
        values.forEach { (count, label) ->
            assertEquals(label, ReminderUiRules.recommendationLimit(count, false))
        }
    }

    @Test fun englishRecommendationLabels() {
        assertEquals("1 item", ReminderUiRules.recommendationLimit(1, true))
        assertEquals("2 items", ReminderUiRules.recommendationLimit(2, true))
        assertEquals("6 items", ReminderUiRules.recommendationLimit(6, true))
    }

    @Test fun favoriteSortingKeepsStableCropIds() {
        val crops = listOf("carrot" to "Морковь", "tomato" to "Томат",
            "onion" to "Лук", "tomato" to "Томат")
        val ordered = ReminderUiRules.orderedCrops(crops, setOf("tomato"))
        assertEquals(listOf("tomato", "onion", "carrot"), ordered.map { it.first })
        assertEquals("Томат", ordered.first().second)
    }

    @Test fun favoriteToggleDoesNotMutateOtherIds() {
        val before = setOf("tomato", "carrot")
        val added = ReminderUiRules.toggleFavorite(before, "onion")
        assertTrue("onion" in added)
        assertEquals(setOf("tomato", "carrot", "onion"), added)
        val removed = ReminderUiRules.toggleFavorite(added, "carrot")
        assertFalse("carrot" in removed)
        assertEquals(before, setOf("tomato", "carrot"))
    }
}
