package ru.goodrawru.myposev

/**
 * Validates untrusted XLSX metadata before it can modify local garden data.
 * Pure Kotlin so these rules can be exercised by local JVM unit tests.
 */
internal object ExcelImportGuard {
    const val MAX_FILE_BYTES = 10 * 1024 * 1024
    const val MAX_ROWS_PER_SHEET = 20_000
    const val MAX_COLUMNS_PER_SHEET = 128
    const val MAX_CELL_CHARS = 32_767
    const val MAX_SHARED_STRINGS = 100_000
    const val MAX_SHEETS = 30

    /** Detect changes made while an import confirmation dialog is open. */
    fun requireUnchangedSnapshot(before: Map<String, Any>, now: Map<String, Any>) {
        require(before == now) {
            "Данные приложения изменились после проверки Excel. Откройте книгу заново перед импортом."
        }
    }

    /**
     * Keep SharedPreferences value types stable. A type changed by editing
     * the hidden worksheet could crash subsequent reads (getString/getInt).
     */
    fun requireCompatibleTypes(previous: Map<String, Any>, incoming: Map<String, Any>) {
        fun preferenceType(value: Any): String = when (value) {
            is String -> "string"
            is Boolean -> "boolean"
            is Int -> "int"
            is Long -> "long"
            is Float -> "float"
            is Set<*> -> "string_set"
            else -> throw IllegalArgumentException("Неизвестный тип настройки Excel")
        }
        incoming.forEach { (key, value) ->
            val old = previous[key]
            if (old != null) {
                require(preferenceType(old) == preferenceType(value)) {
                    "Изменён тип настройки «$key». Исправьте служебный лист Excel."
                }
            }
        }
    }

    fun requireWorkbookSize(byteCount: Int) {
        require(byteCount in 1..MAX_FILE_BYTES) { "Файл пустой или превышает 10 МБ" }
    }

    fun requireUniquePreferenceKey(key: String, existing: MutableSet<String>) {
        require(key.isNotBlank() && existing.add(key)) { "Повторяется или отсутствует ключ в служебных данных Excel" }
    }

    fun zipEntryPath(name: String): String {
        val normalized = name.removeSuffix("/") // ZIP directory entries may end in '/'.
        require(normalized.isNotEmpty() && !normalized.startsWith('/') && !normalized.contains('\\') &&
            !normalized.contains(':') && normalized.split('/').none { it == ".." || it == "." || it.isEmpty() }) {
            "Недопустимый путь в книге Excel"
        }
        return name
    }

    fun worksheetPath(target: String): String {
        val cleaned = target.removePrefix("/")
        val path = if (cleaned.startsWith("xl/")) cleaned else "xl/$cleaned"
        require(Regex("^xl/worksheets/[A-Za-z0-9_.-]+\\.xml$").matches(path) &&
            !path.substringAfterLast('/').contains("..")) {
            "Недопустимый адрес листа Excel"
        }
        return path
    }

    fun cellColumn(reference: String): Int {
        val match = Regex("^([A-Za-z]{1,3})([1-9][0-9]*)$").matchEntire(reference)
            ?: throw IllegalArgumentException("Неверный адрес ячейки Excel")
        val column = match.groupValues[1].uppercase().fold(0) { n, ch ->
            n * 26 + (ch - 'A' + 1)
        } - 1
        require(column in 0 until MAX_COLUMNS_PER_SHEET) { "Слишком много столбцов в книге Excel" }
        return column
    }

    fun requireRowCount(count: Int) {
        require(count in 0..MAX_ROWS_PER_SHEET) { "Слишком много строк в книге Excel" }
    }

    fun requireCellLength(length: Int) {
        require(length in 0..MAX_CELL_CHARS) { "Слишком длинная ячейка в книге Excel" }
    }

    fun requireSharedStringCount(count: Int) {
        require(count in 0..MAX_SHARED_STRINGS) { "Слишком много общих строк в книге Excel" }
    }

    fun requireHeaders(header: List<String>, expected: List<String>, sheetName: String) {
        expected.forEach { name ->
            require(header.count { it == name } == 1) {
                "На листе «$sheetName» отсутствует или повторяется столбец «$name»"
            }
        }
    }

    /** Text fields must retain the user's spaces and line breaks. */
    fun recordCell(value: String, field: String): String =
        if (field in setOf("notes", "advice", "title")) value else value.trim()

    fun requireUniqueCell(column: Int, existing: MutableMap<Int, String>) {
        require(column !in existing) { "Повторяется адрес ячейки Excel" }
    }

    fun requireRecordArray(value: String, key: String) {
        require(value.trim().startsWith("[")) { "Повреждён список записей: $key" }
    }

    fun parseBoolean(value: String, allowBlank: Boolean = false): Boolean = when (value.trim().lowercase()) {
        "true", "да", "1" -> true
        "false", "нет", "0" -> false
        "" -> if (allowBlank) false else throw IllegalArgumentException("Некорректное логическое значение")
        else -> throw IllegalArgumentException("Некорректное логическое значение")
    }

    fun parseInt(value: String): Int = value.toIntOrNull()
        ?: throw IllegalArgumentException("Некорректное целое число в служебных данных")

    fun parseLong(value: String): Long = value.toLongOrNull()
        ?: throw IllegalArgumentException("Некорректное длинное число в служебных данных")

    fun parseFloat(value: String): Float = value.toFloatOrNull()?.takeIf { it.isFinite() }
        ?: throw IllegalArgumentException("Некорректное дробное число в служебных данных")
}
