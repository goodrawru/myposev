package ru.goodrawru.myposev

/**
 * Backups are stored as ZIP, but their uncompressed JSON size must be bounded.
 * Apply the same limit during both export and import so this app never creates
 * an archive it will immediately reject on restore.
 */
import org.json.JSONArray

internal object BackupArchivePolicy {
    const val MAX_UNCOMPRESSED_BYTES = 5 * 1024 * 1024

    /**
     * Android JSONObject.put() does not automatically wrap arbitrary Sets.
     * Export StringSets as a proper JSON array, matching the existing "ss"
     * restore format. Preserve all other supported preference types.
     */
    fun encodeValue(value: Any?): Any = when (value) {
        is Set<*> -> JSONArray(value.map {
            require(it is String) { "Строковый набор содержит неверное значение" }
            it
        }.sorted())
        is String, is Boolean, is Int, is Long, is Float -> value
        else -> throw IllegalArgumentException("Неподдерживаемое значение в резервной копии")
    }

    fun requireNonEmptyPreferences(keyCount: Int) {
        require(keyCount > 0) { "Резервная копия не содержит настроек" }
    }

    fun requireSupportedSize(byteCount: Int) {
        require(byteCount in 0..MAX_UNCOMPRESSED_BYTES) {
            "Файл резервной копии слишком большой (максимум 5 МБ)"
        }
    }
}
