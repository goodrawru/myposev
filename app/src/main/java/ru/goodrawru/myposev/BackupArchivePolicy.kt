package ru.goodrawru.myposev

import org.json.JSONArray
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Keeps backup import and export constraints consistent.
 */
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

    /**
     * Old or edited backups must not change an existing preference's type.
     * Otherwise getString/getInt on later app launches can throw at runtime.
     */
    fun requireCompatibleTypes(
        existing: Map<String, Any?>,
        restored: Map<String, Any?>
    ) {
        fun typeOf(value: Any): String = when (value) {
            is String -> "string"
            is Boolean -> "bool"
            is Int -> "int"
            is Long -> "long"
            is Float -> "float"
            is Set<*> -> "string_set"
            else -> throw IllegalArgumentException("Неподдерживаемый тип настройки")
        }
        restored.forEach { (key, value) ->
            val previous = existing[key]
            if (previous != null && value != null) {
                require(typeOf(previous) == typeOf(value)) {
                    "Несовместимый тип настройки в архиве: $key"
                }
            }
        }
    }

    fun requireNonEmptyPreferences(keyCount: Int) {
        require(keyCount > 0) { "Резервная копия не содержит настроек" }
    }

    /**
     * Do not silently replace damaged UTF-8 sequences with replacement glyphs:
     * they could corrupt user notes while still producing syntactically valid JSON.
     */
    fun decodeJson(bytes: ByteArray): String =
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()

    fun requireSupportedSize(byteCount: Int) {
        require(byteCount in 0..MAX_UNCOMPRESSED_BYTES) {
            "Файл резервной копии слишком большой (максимум 5 МБ)"
        }
    }
}
