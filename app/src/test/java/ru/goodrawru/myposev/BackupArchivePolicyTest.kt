package ru.goodrawru.myposev

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.CharacterCodingException

class BackupArchivePolicyTest {
    @Test fun acceptsExactlyFiveMegabytes() {
        assertEquals(5 * 1024 * 1024, BackupArchivePolicy.MAX_UNCOMPRESSED_BYTES)
        BackupArchivePolicy.requireSupportedSize(0)
        BackupArchivePolicy.requireSupportedSize(1024)
        BackupArchivePolicy.requireSupportedSize(BackupArchivePolicy.MAX_UNCOMPRESSED_BYTES)
    }

    @Test fun rejectsOversizedOrInvalidSizes() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.requireSupportedSize(BackupArchivePolicy.MAX_UNCOMPRESSED_BYTES + 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.requireSupportedSize(-1)
        }
    }

    @Test fun stringSetsAreSavedAsJsonArraysAndCanBeRestored() {
        val stored = linkedSetOf("Томат", "Огурец", "Базилик")
        val encoded = BackupArchivePolicy.encodeValue(stored)
        assertTrue(encoded is JSONArray)
        val entry = JSONObject().put("type", "ss").put("value", encoded)
        val restored = JSONObject(entry.toString()).getJSONArray("value")
        val decoded = (0 until restored.length()).map { restored.getString(it) }.toSet()
        assertEquals(stored, decoded)
    }

    @Test fun supportedScalarValuesRoundTripThroughJson() {
        val cases: List<Pair<String, Any>> = listOf(
            "s" to "Привет 🌱",
            "b" to true,
            "i" to 25,
            "l" to 5000000000L,
            "f" to 2.5f
        )
        for ((kind, value) in cases) {
            val encoded = JSONObject().put("type", kind)
                .put("value", BackupArchivePolicy.encodeValue(value))
            val restored = JSONObject(encoded.toString())
            assertEquals(kind, restored.getString("type"))
            when (kind) {
                "s" -> assertEquals(value, restored.getString("value"))
                "b" -> assertEquals(value, restored.getBoolean("value"))
                "i" -> assertEquals(value, restored.getInt("value"))
                "l" -> assertEquals(value, restored.getLong("value"))
                "f" -> assertEquals((value as Float).toDouble(), restored.getDouble("value"), 0.0001)
            }
        }
    }

    @Test fun emptyBackupsCannotEraseExistingPreferences() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.requireNonEmptyPreferences(0)
        }
        BackupArchivePolicy.requireNonEmptyPreferences(1)
    }

    @Test fun malformedUtf8IsRejectedInsteadOfCorruptingNotes() {
        val notes = "{\"note\":\"Морковь 🥕\"}"
        assertEquals(notes, BackupArchivePolicy.decodeJson(notes.toByteArray(Charsets.UTF_8)))
        assertThrows(CharacterCodingException::class.java) {
            BackupArchivePolicy.decodeJson(byteArrayOf(0xC3.toByte(), 0x28))
        }
    }

    @Test fun rejectsUnsupportedPreferenceTypes() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.encodeValue(listOf("not", "a", "set"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.encodeValue(setOf("OK", 42))
        }
    }

    @Test fun backupRejectsChangesToExistingPreferenceTypes() {
        val old: Map<String, Any?> = mapOf(
            "garden_city" to "Москва",
            "reminder_hour" to 9,
            "reminder_enabled" to true,
            "favorite_crop_ids" to setOf("tomato"),
            "last_checked" to 1234567890123L
        )
        BackupArchivePolicy.requireCompatibleTypes(old, old)
        BackupArchivePolicy.requireCompatibleTypes(old, old + ("garden_city" to "Ялта"))
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.requireCompatibleTypes(old, old + ("reminder_hour" to "9"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.requireCompatibleTypes(old, old + ("favorite_crop_ids" to "tomato"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchivePolicy.requireCompatibleTypes(old, old + ("last_checked" to 5))
        }
    }
}
