package ru.goodrawru.myposev

import android.content.SharedPreferences
import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Русскоязычный двусторонний обмен данными приложения с обычной книгой XLSX.
 * Формат книги — OOXML без сторонних Excel-библиотек, поэтому приложение не получает
 * тяжёлых desktop-зависимостей.
 */
internal object ExcelTransfer {
    private const val MAGIC = "МОЙПОСЕВ_XLSX_1"
    private const val MAX_UNPACKED_BYTES = 20 * 1024 * 1024
    private const val MAX_ENTRIES = 80
    private const val TECH = "Технические данные (не изменять)"

    private data class Sheet(val name: String, val rows: List<List<String>>, val hidden: Boolean = false)
    private data class SheetRef(val name: String, val relId: String)
    data class ImportPlan(
        internal val values: Map<String, Any>,
        internal val original: Map<String, Any>,
        val added: Int,
        val changed: Int,
        val deleted: Int,
        val notes: Int
    ) {
        fun summary(): String = "Новых записей: $added\nИзменённых записей: $changed\nУдаляемых записей: $deleted\nЗаметок в книге: $notes"
    }

    fun export(
        prefs: SharedPreferences,
        builtInPlants: List<Plant>,
        cropIconCatalog: JSONArray
    ): ByteArray {
        val all = prefs.all
        val sheets = mutableListOf<Sheet>()
        sheets += Sheet("Инструкция", listOf(
            listOf("Книга данных приложения «Мой Посевной»"),
            listOf("Как работать с файлом"),
            listOf("Редактируйте строки на листах «Посадки», «Урожай», «Задачи», «Мои культуры» и «Заметки»."),
            listOf("Не меняйте ID существующих записей и последний технический столбец: они нужны для сохранения связей и дополнительных полей."),
            listOf("Для новой строки оставьте ID пустым — приложение автоматически создаст новый идентификатор при импорте."),
            listOf("Чтобы удалить запись из приложения, удалите её строку на соответствующем листе. Перед применением приложение покажет количество удалений."),
            listOf("Даты вводите в формате дд.мм.гггг или гггг-мм-дд. Не меняйте формат идентификаторов."),
            listOf("Лист «Каталог культур» справочный. Он содержит доступные в приложении культуры и значки; изменения этого листа не импортируются."),
            listOf("Лист «Настройки» содержит переносимые настройки. Служебный лист скрыт и нужен для сохранения всех данных приложения."),
            listOf("Для возврата данных откройте приложение → О программе → Импорт из Excel и выберите эту книгу."),
            listOf("Импорт заменяет содержимое рабочих списков данными из книги. Перед применением создаётся внутренняя точка отката.")
        ))
        sheets += recordsSheet(
            "Посадки", "planting_records",
            listOf("ID", "Культура", "Сорт", "Дата посева", "Место", "Заметки", "Создано"),
            listOf("id", "crop", "variety", "sowing", "place", "notes", "created"), all
        )
        val legacyPlantings = mutableListOf<List<String>>()
        legacyPlantings += listOf("Старая запись")
        (all["plantings"] as? String).orEmpty().split("\n").filter { it.isNotBlank() }.forEach { legacyPlantings += listOf(it) }
        sheets += Sheet("Старые посадки", legacyPlantings)

        sheets += recordsSheet(
            "Урожай", "harvest_records",
            listOf("ID", "Культура", "Количество", "Дата сбора", "Примечание"),
            listOf("id", "crop", "amount", "date", "notes"), all
        )
        sheets += recordsSheet(
            "Задачи", "care_tasks",
            listOf("ID", "Задача", "Культура или грядка", "Тип работы", "Дата", "Выполнено"),
            listOf("id", "title", "crop", "type", "date", "done"), all
        )
        sheets += recordsSheet(
            "Мои культуры", "user_crops",
            listOf("ID", "Культура", "Категория", "Срок посева", "Глубина", "Всходы", "Рекомендации", "Значок", "Эмодзи"),
            listOf("id", "name", "category", "sowing", "depth", "sprouts", "advice", "icon", "emoji"), all
        )

        val notes = mutableListOf<List<String>>()
        notes += listOf("Дата", "Заметка", "Ключ записи")
        all.toSortedMap().forEach { (key, value) ->
            if (key.startsWith("note_") && value is String && value.isNotBlank()) {
                val rawDate = key.removePrefix("note_")
                val iso = normalizeNoteDate(rawDate) ?: rawDate
                notes += listOf(iso, value, key)
            }
        }
        sheets += Sheet("Заметки", notes)

        val settingsKeys = listOf(
            "garden_city", "garden_region", "garden_zone", "garden_lat", "garden_lon",
            "garden_location_set", "reminder_enabled", "reminder_hour", "reminder_minute",
            "reminder_limit", "app_language"
        )
        val settingLabels = mapOf(
            "garden_city" to "Город", "garden_region" to "Регион", "garden_zone" to "Климатическая зона",
            "garden_lat" to "Широта", "garden_lon" to "Долгота", "garden_location_set" to "Место выбрано",
            "reminder_enabled" to "Напоминания включены", "reminder_hour" to "Час напоминания",
            "reminder_minute" to "Минута напоминания", "reminder_limit" to "Лимит напоминаний",
            "app_language" to "Язык приложения"
        )
        val settings = mutableListOf<List<String>>()
        settings += listOf("Параметр", "Значение", "Ключ", "Тип")
        settingsKeys.forEach { key ->
            val safeValue = all[key] ?: return@forEach
            val type = typeOf(safeValue)
            settings += listOf(settingLabels[key] ?: key, valueToText(safeValue), key, type)
        }
        sheets += Sheet("Настройки", settings)

        val catalog = mutableListOf<List<String>>()
        catalog += listOf("Значок", "Культура", "Группа", "Срок посева", "Глубина", "Всходы", "Рекомендации", "ID значка")
        val iconByName = (0 until cropIconCatalog.length()).mapNotNull { cropIconCatalog.optJSONObject(it) }
            .associateBy { it.optString("name") }
        builtInPlants.forEach { plant ->
            val icon = iconByName[plant.name]
            val glyph = cropEmoji(plant.name)
            catalog += listOf(glyph, plant.name, plant.group, plant.sowing, plant.depth, plant.sprouts, plant.advice, icon?.optString("id").orEmpty())
        }
        sheets += Sheet("Каталог культур", catalog)

        val raw = mutableListOf<List<String>>()
        raw += listOf("Ключ", "Тип", "Значение")
        raw += listOf("__формат__", "s", MAGIC)
        all.toSortedMap().forEach { (key, value) ->
            val safeValue = value ?: return@forEach
            raw += listOf(key, typeOf(safeValue), valueToText(safeValue))
        }
        sheets += Sheet("_Данные", raw, hidden = true)
        // Never export an XLSX that the same application will reject as oversized.
        return writeWorkbook(sheets).also { ExcelImportGuard.requireWorkbookSize(it.size) }
    }

    fun prepareImport(bytes: ByteArray, current: SharedPreferences): ImportPlan {
        val snapshot: Map<String, Any> = current.all.mapNotNull { (key, value) ->
            value?.let { key to (if (it is Set<*>) it.toSet() else it) }
        }.toMap()
        ExcelImportGuard.requireWorkbookSize(bytes.size)
        val entries = readZip(bytes)
        val workbook = entries["xl/workbook.xml"] ?: throw IllegalArgumentException("Это не книга Excel XLSX")
        val rels = entries["xl/_rels/workbook.xml.rels"] ?: throw IllegalArgumentException("В книге отсутствуют связи листов")
        val refs = parseWorkbookRefs(workbook)
        require(refs.size <= ExcelImportGuard.MAX_SHEETS) { "Слишком много листов Excel" }
        val targets = parseRelationships(rels)
        val shared = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheets = linkedMapOf<String, List<List<String>>>()
        refs.forEach { ref ->
            val target = targets[ref.relId] ?: throw IllegalArgumentException("Не найден лист: ${ref.name}")
            val path = normalizeSheetPath(target)
            val xml = entries[path] ?: throw IllegalArgumentException("Не удалось прочитать лист: ${ref.name}")
            require(ref.name !in sheets) { "Повторяется название листа Excel" }
            sheets[ref.name] = parseSheet(xml, shared)
        }
        val rawRows = sheets["_Данные"] ?: throw IllegalArgumentException("В книге нет служебного листа. Экспортируйте файл заново из приложения.")
        val rawHeader = rawRows.firstOrNull().orEmpty()
        val keyCol = rawHeader.indexOf("Ключ")
        val typeCol = rawHeader.indexOf("Тип")
        val valueCol = rawHeader.indexOf("Значение")
        ExcelImportGuard.requireHeaders(rawHeader, listOf("Ключ", "Тип", "Значение"), "_Данные")
        val stored = linkedMapOf<String, Any>()
        val rawKeys = mutableSetOf<String>()
        var marker = false
        rawRows.drop(1).forEach { row ->
            val key = row.getOrNull(keyCol).orEmpty()
            val type = row.getOrNull(typeCol).orEmpty()
            val value = row.getOrNull(valueCol).orEmpty()
            if (key == "__формат__") {
                require(!marker && type == "s" && value == MAGIC) { "Повреждён маркер формата Excel" }
                marker = true
            } else if (key.isNotBlank()) {
                ExcelImportGuard.requireUniquePreferenceKey(key, rawKeys)
                stored[key] = parseStoredValue(type, value)
            } else {
                require(type.isBlank() && value.isBlank()) { "Обнаружена строка настроек без ключа" }
            }
        }
        require(marker) { "Версия книги не поддерживается или служебный лист изменён" }

        val existingCounts = listOf("planting_records", "harvest_records", "care_tasks", "user_crops")
            .associateWith { jsonArray(current.getString(it, "[]") ?: "[]").let { a -> a.length() } }
        val incoming = mutableMapOf<String, JSONArray>()
        incoming["planting_records"] = parseRecordSheet(
            sheets["Посадки"], listOf("ID","Культура","Сорт","Дата посева","Место","Заметки","Создано"),
            listOf("id","crop","variety","sowing","place","notes","created"), setOf("id","crop","sowing"),
            emptySet(), "Посадки"
        )
        val legacyRows = sheets["Старые посадки"] ?: throw IllegalArgumentException("В книге отсутствует лист «Старые посадки»")
        val legacyHeader = legacyRows.firstOrNull().orEmpty()
        val legacyCol = legacyHeader.indexOf("Старая запись")
        require(legacyCol >= 0) { "На листе «Старые посадки» отсутствует столбец записи" }
        stored["plantings"] = legacyRows.drop(1).mapNotNull { it.getOrNull(legacyCol)?.trim()?.takeIf(String::isNotBlank) }.joinToString("\n")

        incoming["harvest_records"] = parseRecordSheet(
            sheets["Урожай"], listOf("ID","Культура","Количество","Дата сбора","Примечание"),
            listOf("id","crop","amount","date","notes"), setOf("id","crop","amount","date"),
            emptySet(), "Урожай"
        )
        incoming["care_tasks"] = parseRecordSheet(
            sheets["Задачи"], listOf("ID","Задача","Культура или грядка","Тип работы","Дата","Выполнено"),
            listOf("id","title","crop","type","date","done"), setOf("id","title","date"),
            setOf("done"), "Задачи"
        )
        incoming["user_crops"] = parseRecordSheet(
            sheets["Мои культуры"], listOf("ID","Культура","Категория","Срок посева","Глубина","Всходы","Рекомендации","Значок","Эмодзи"),
            listOf("id","name","category","sowing","depth","sprouts","advice","icon","emoji"), setOf("id","name"),
            emptySet(), "Мои культуры"
        )
        incoming.forEach { (key, value) -> stored[key] = value.toString() }

        // Notes are editable rows too. Remove the old note keys from the snapshot, then rebuild them.
        stored.keys.filter { it.startsWith("note_") }.toList().forEach { stored.remove(it) }
        val noteRows = sheets["Заметки"] ?: throw IllegalArgumentException("В книге отсутствует лист «Заметки»")
        val noteHeader = noteRows.firstOrNull().orEmpty()
        val dateCol = noteHeader.indexOf("Дата")
        val textCol = noteHeader.indexOf("Заметка")
        ExcelImportGuard.requireHeaders(noteHeader, listOf("Дата", "Заметка", "Ключ записи"), "Заметки")
        var noteCount = 0
        val noteKeys = mutableSetOf<String>()
        noteRows.drop(1).forEachIndexed { index, row ->
            val value = row.getOrNull(textCol).orEmpty()
            if (value.isBlank()) return@forEachIndexed
            val date = normalizeNoteDate(row.getOrNull(dateCol).orEmpty())
                ?: throw IllegalArgumentException("Проверьте дату заметки в строке ${index + 2}")
            val key = "note_$date"
            require(noteKeys.add(key)) { "Повторяется дата заметки $date" }
            stored[key] = value
            noteCount++
        }

        // Allow safe editing of the small, explicitly supported settings sheet.
        val settingRows = sheets["Настройки"] ?: throw IllegalArgumentException("В книге отсутствует лист «Настройки»")
        val settingHeader = settingRows.firstOrNull().orEmpty()
        ExcelImportGuard.requireHeaders(settingHeader, listOf("Параметр", "Значение", "Ключ", "Тип"), "Настройки")
        val sKey = settingHeader.indexOf("Ключ")
        val sType = settingHeader.indexOf("Тип")
        val sValue = settingHeader.indexOf("Значение")
        if (sKey >= 0 && sType >= 0 && sValue >= 0) {
            settingRows.drop(1).forEach { row ->
                val key = row.getOrNull(sKey).orEmpty()
                if (key in setOf("garden_city","garden_region","garden_zone","garden_lat","garden_lon","garden_location_set","reminder_enabled","reminder_hour","reminder_minute","reminder_limit","app_language")) {
                    val value = row.getOrNull(sValue).orEmpty().trim()
                    stored[key] = when (key) {
                        "garden_location_set", "reminder_enabled" -> ExcelImportGuard.parseBoolean(value)
                        "reminder_hour" -> value.toIntOrNull()?.takeIf { it in 0..23 } ?: throw IllegalArgumentException("Час напоминания должен быть от 0 до 23")
                        "reminder_minute" -> value.toIntOrNull()?.takeIf { it in 0..59 } ?: throw IllegalArgumentException("Минута напоминания должна быть от 0 до 59")
                        "reminder_limit" -> value.toIntOrNull()?.takeIf { it in 1..20 } ?: throw IllegalArgumentException("Лимит напоминаний должен быть от 1 до 20")
                        "garden_lat" -> {
                            if (value.isNotBlank()) require(value.toDoubleOrNull()?.let { it in -90.0..90.0 } == true) { "Некорректная широта" }
                            value
                        }
                        "garden_lon" -> {
                            if (value.isNotBlank()) require(value.toDoubleOrNull()?.let { it in -180.0..180.0 } == true) { "Некорректная долгота" }
                            value
                        }
                        "app_language" -> value.also { require(it == "ru" || it == "en") { "Язык приложения: ru или en" } }
                        else -> value
                    }
                }
            }
        }

        val oldIds = existingCounts.keys.associateWith { key -> ids(jsonArray(current.getString(key, "[]") ?: "[]")) }
        val newIds = incoming.mapValues { ids(it.value) }
        var added = 0
        var deleted = 0
        var changed = 0
        incoming.forEach { (key, arr) ->
            val before = oldIds[key].orEmpty()
            val after = newIds[key].orEmpty()
            added += (after - before).size
            deleted += (before - after).size
            if (before.intersect(after).isNotEmpty()) {
                val oldById = jsonArray(current.getString(key, "[]") ?: "[]").let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.associateBy { it.optString("id") } }
                val newById = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.associateBy { it.optString("id") }
                changed += before.intersect(after).count { id -> oldById[id]?.toString() != newById[id]?.toString() }
            }
        }
        // Keep existing non-editable settings that are missing from the workbook.
        // Notes and editable lists are still replaced from their visible sheets.
        ExcelImportGuard.preserveUneditedPreferences(snapshot, stored)
        ExcelImportGuard.requireCompatibleTypes(snapshot, stored)
        return ImportPlan(stored, snapshot, added, changed, deleted, noteCount)
    }

    fun applyImport(plan: ImportPlan, prefs: SharedPreferences): Boolean {
        val old: Map<String, Any> = prefs.all.mapNotNull { (key, value) ->
            value?.let { key to (if (it is Set<*>) it.toSet() else it) }
        }.toMap()
        ExcelImportGuard.requireUnchangedSnapshot(plan.original, old)
        ExcelImportGuard.requireCompatibleTypes(old, plan.values)
        fun write(values: Map<String, Any>): Boolean {
            val editor = prefs.edit().clear()
            values.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> {
                        require(value.all { it is String }) { "Некорректные строки в данных Excel" }
                        editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                    else -> throw IllegalArgumentException("Неподдерживаемый тип настройки: $key")
                }
            }
            return editor.commit()
        }
        val applied = runCatching { write(plan.values) }.getOrDefault(false)
        if (applied) return true
        val rollbackSucceeded = runCatching { write(old) }.getOrDefault(false)
        if (!rollbackSucceeded) {
            throw IllegalStateException("Ошибка импорта и отката. Проверьте данные приложения и восстановите резервную копию.")
        }
        return false
    }

    private fun recordsSheet(
        name: String, key: String, headers: List<String>, fields: List<String>, all: Map<String, *>
    ): Sheet {
        val rows = mutableListOf<List<String>>()
        rows += headers + TECH
        val array = jsonArray(all[key] as? String ?: "[]")
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val values = fields.map { field ->
                when (field) {
                    "done" -> if (item.optBoolean(field, false)) "Да" else "Нет"
                    else -> item.optString(field, "")
                }
            }
            rows += values + item.toString()
        }
        return Sheet(name, rows)
    }

    private fun parseRecordSheet(
        rows: List<List<String>>?, expectedHeaders: List<String>, fields: List<String>,
        required: Set<String>, booleanFields: Set<String>, sheetName: String
    ): JSONArray {
        val data = rows ?: throw IllegalArgumentException("В книге отсутствует лист «$sheetName»")
        val header = data.firstOrNull().orEmpty()
        // Never treat deleted columns as blank: import replaces entire lists and
        // would otherwise silently discard crop details, dates or hidden fields.
        ExcelImportGuard.requireHeaders(header, expectedHeaders + TECH, sheetName)
        val indexes = fields.map { field -> header.indexOf(expectedHeaders[fields.indexOf(field)]) }
        val techIndex = header.indexOf(TECH)
        val result = JSONArray()
        val ids = mutableSetOf<String>()
        data.drop(1).forEachIndexed { rowIndex, row ->
            // Preserve spaces/newlines in notes, advice and task titles while trimming IDs/dates.
            val values = fields.mapIndexed { i, field ->
                ExcelImportGuard.recordCell(row.getOrNull(indexes[i]).orEmpty(), field)
            }
            if (values.all { it.isBlank() }) return@forEachIndexed
            var item = if (techIndex >= 0) {
                val raw = row.getOrNull(techIndex).orEmpty()
                if (raw.isBlank()) JSONObject() else try { JSONObject(raw) } catch (_: Exception) {
                    throw IllegalArgumentException("Повреждены технические данные на листе «$sheetName», строка ${rowIndex + 2}")
                }
            } else JSONObject()
            fields.forEachIndexed { i, field ->
                val value = values[i]
                if (field in booleanFields) item.put(field, ExcelImportGuard.parseBoolean(value, allowBlank = true))
                else if (field == "id") {
                    if (value.isNotBlank()) item.put(field, value.toLongOrNull() ?: value)
                    else {
                        val generated = if (sheetName == "Мои культуры") UUID.randomUUID().toString()
                            else (System.currentTimeMillis() + rowIndex).toString()
                        item.put("id", generated)
                    }
                } else if (field == "date" || (field == "sowing" && sheetName != "Мои культуры")) {
                    val normalizedDate = normalizeAppDate(value)
                        ?: throw IllegalArgumentException("Проверьте дату на листе «$sheetName», строка ${rowIndex + 2}. Используйте дд.мм.гггг или гггг-мм-дд.")
                    item.put(field, normalizedDate)
                } else if (field == "created") {
                    if (value.isNotBlank()) item.put(field, value.toLongOrNull() ?: value)
                    else if (!item.has("created")) item.put("created", System.currentTimeMillis())
                } else item.put(field, value)
            }
            required.forEach { field ->
                require(item.optString(field).isNotBlank()) { "Заполните «${expectedHeaders[fields.indexOf(field)]}» на листе «$sheetName», строка ${rowIndex + 2}" }
            }
            val id = item.optString("id")
            require(ids.add(id)) { "Повторяется ID $id на листе «$sheetName»" }
            result.put(item)
        }
        return result
    }

    private fun ids(array: JSONArray): Set<String> =
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("id")?.takeIf(String::isNotBlank) }.toSet()

    private fun jsonArray(value: String): JSONArray = try {
        JSONArray(value)
    } catch (error: Exception) {
        // Never silently erase planted crops, harvests or tasks when a stored list is damaged.
        throw IllegalArgumentException("Повреждён список записей приложения", error)
    }

    private fun typeOf(value: Any): String = when (value) {
        is String -> "s"; is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"
        is Set<*> -> "ss"; else -> "s"
    }

    private fun valueToText(value: Any): String = when (value) {
        is Set<*> -> JSONArray(value.filterIsInstance<String>().sorted()).toString()
        else -> value.toString()
    }

    private fun parseStoredValue(type: String, value: String): Any = when (type) {
        "s" -> value
        "b" -> ExcelImportGuard.parseBoolean(value)
        "i" -> ExcelImportGuard.parseInt(value)
        "l" -> ExcelImportGuard.parseLong(value)
        "f" -> ExcelImportGuard.parseFloat(value)
        "ss" -> {
            val array = try { JSONArray(value) } catch (_: Exception) {
                throw IllegalArgumentException("Повреждены строковые настройки в книге Excel")
            }
            (0 until array.length()).map { index ->
                array.opt(index) as? String
                    ?: throw IllegalArgumentException("Повреждён строковый набор в книге Excel")
            }.toSet()
        }
        else -> throw IllegalArgumentException("Неподдерживаемый тип в служебных данных Excel")
    }

    private fun normalizeAppDate(value: String): String? {
        val iso = normalizeNoteDate(value) ?: return null
        val parsed = try { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(iso) } catch (_: Exception) { null } ?: return null
        return SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(parsed)
    }

    private fun normalizeNoteDate(value: String): String? {
        val input = value.trim()
        val formats = listOf("yyyy-MM-dd", "dd.MM.yyyy", "d.M.yyyy")
        formats.forEach { pattern ->
            try {
                val format = SimpleDateFormat(pattern, Locale.ROOT).apply { isLenient = false }
                val parsed = format.parse(input) ?: return@forEach
                if (format.format(parsed) != input) return@forEach
                return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(parsed)
            } catch (_: Exception) { }
        }
        return null
    }

    private fun cropEmoji(name: String): String {
        val n = name.lowercase(Locale.ROOT)
        return when {
            "томат" in n -> "🍅"; "перец" in n -> "🫑"; "огур" in n -> "🥒"; "морков" in n -> "🥕"
            "редис" in n -> "🌶️"; "свёкл" in n || "свекл" in n -> "🫜"; "капуст" in n -> "🥬"
            "клубник" in n || "землян" in n -> "🍓"; "тыкв" in n -> "🎃"; "кабач" in n -> "🥒"
            "горох" in n || "фасол" in n || "боб" in n -> "🫛"; "яблон" in n || "яблок" in n -> "🍎"
            "малин" in n || "ежев" in n || "смород" in n -> "🫐"; "лук" in n || "чеснок" in n -> "🧅"
            "зел" in n || "салат" in n || "петруш" in n || "укроп" in n || "базилик" in n -> "🌿"
            else -> "🌱"
        }
    }

    private fun writeWorkbook(sheets: List<Sheet>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out, Charsets.UTF_8).use { zip ->
            fun entry(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val overrides = sheets.mapIndexed { i, _ -> """<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" }.joinToString("")
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>$overrides</Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            val sheetNodes = sheets.mapIndexed { i, sheet ->
                """<sheet name="${xml(sheet.name)}" sheetId="${i + 1}" r:id="rId${i + 1}"${if (sheet.hidden) " state=\"hidden\"" else ""}/>"""
            }.joinToString("")
            entry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><bookViews><workbookView/></bookViews><sheets>$sheetNodes</sheets></workbook>""")
            val relNodes = sheets.mapIndexed { i, _ ->
                """<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>"""
            }.joinToString("") + """<Relationship Id="rId${sheets.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>"""
            entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$relNodes</Relationships>""")
            entry("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF275B39"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="49" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="49" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>""")
            sheets.forEachIndexed { sheetIndex, sheet ->
                val maxCols = sheet.rows.maxOfOrNull { it.size } ?: 1
                val lastCol = columnName(maxCols)
                val lastRow = sheet.rows.size.coerceAtLeast(1)
                val widths = (1..maxCols).joinToString("") { col ->
                    val header = sheet.rows.firstOrNull()?.getOrNull(col - 1).orEmpty()
                    if (header == TECH) """<col min="$col" max="$col" width="12" hidden="1" customWidth="1"/>"""
                    else {
                        val width = header.length.coerceIn(10, 36)
                        """<col min="$col" max="$col" width="$width" customWidth="1"/>"""
                    }
                }
                val data = sheet.rows.mapIndexed { rowIndex, row ->
                    val cells = row.mapIndexed { colIndex, value ->
                        val ref = columnName(colIndex + 1) + (rowIndex + 1)
                        val style = if (rowIndex == 0) 1 else 0
                        """<c r="$ref" t="inlineStr" s="$style"><is><t xml:space="preserve">${xml(value)}</t></is></c>"""
                    }.joinToString("")
                    """<row r="${rowIndex + 1}">$cells</row>"""
                }.joinToString("")
                val filter = if (sheet.rows.size > 1 && sheet.name != "Инструкция") """<autoFilter ref="A1:$lastCol$lastRow"/>""" else ""
                entry("xl/worksheets/sheet${sheetIndex + 1}.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews><cols>$widths</cols><sheetData>$data</sheetData>$filter</worksheet>""")
            }
        }
        val result = out.toByteArray()
        // An exported workbook must remain re-importable by this app.
        ExcelImportGuard.requireWorkbookSize(result.size)
        return result
    }

    private fun readZip(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var total = 0
        var count = 0
        ZipInputStream(ByteArrayInputStream(bytes), Charsets.UTF_8).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                count++
                require(count <= MAX_ENTRIES) { "В архиве слишком много элементов" }
                val name = ExcelImportGuard.zipEntryPath(entry.name)
                require(name !in result) { "Повторяющийся файл в книге Excel" }
                if (!entry.isDirectory) {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= MAX_UNPACKED_BYTES) { "Распакованный файл слишком велик" }
                        output.write(buffer, 0, n)
                    }
                    result[name] = output.toByteArray()
                }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun parseWorkbookRefs(bytes: ByteArray): List<SheetRef> {
        val parser = parser(bytes)
        val result = mutableListOf<SheetRef>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: continue
                val rel = attributeByLocalName(parser, "id") ?: continue
                result += SheetRef(name, rel)
                require(result.size <= ExcelImportGuard.MAX_SHEETS) { "Слишком много листов Excel" }
            }
        }
        require(result.isNotEmpty()) { "В книге нет листов" }
        return result
    }

    private fun parseRelationships(bytes: ByteArray): Map<String, String> {
        val parser = parser(bytes)
        val result = mutableMapOf<String, String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id") ?: continue
                val target = parser.getAttributeValue(null, "Target") ?: continue
                require(id !in result) { "Повторяется связь листа Excel" }
                result[id] = target
            }
        }
        return result
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val parser = parser(bytes)
        val result = mutableListOf<String>()
        var current: StringBuilder? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "si") current = StringBuilder()
            else if (parser.eventType == XmlPullParser.START_TAG && parser.name == "t" && current != null) {
                current!!.append(parser.nextText())
                ExcelImportGuard.requireCellLength(current!!.length)
            }
            else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "si") {
                result += current?.toString().orEmpty()
                ExcelImportGuard.requireSharedStringCount(result.size)
                current = null
            }
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val parser = parser(bytes)
        val rows = mutableListOf<List<String>>()
        var rowCells: MutableMap<Int, String>? = null
        var cellIndex = -1
        var cellType = ""
        var cellValue = StringBuilder()
        var maxColumn = -1
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "row") {
                rowCells = mutableMapOf()
                maxColumn = -1
            } else if (parser.eventType == XmlPullParser.START_TAG && parser.name == "c" && rowCells != null) {
                val ref = parser.getAttributeValue(null, "r").orEmpty()
                cellIndex = ExcelImportGuard.cellColumn(ref)
                cellType = parser.getAttributeValue(null, "t").orEmpty()
                cellValue = StringBuilder()
            } else if (parser.eventType == XmlPullParser.START_TAG && (parser.name == "v" || parser.name == "t") && rowCells != null && cellIndex >= 0) {
                cellValue.append(parser.nextText())
                ExcelImportGuard.requireCellLength(cellValue.length)
            } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "c" && rowCells != null && cellIndex >= 0) {
                val raw = cellValue.toString()
                val value = if (cellType == "s") {
                    shared.getOrNull(raw.toIntOrNull() ?: -1)
                        ?: throw IllegalArgumentException("Неверная ссылка на общую строку Excel")
                } else raw
                ExcelImportGuard.requireCellLength(value.length)
                rowCells!![cellIndex] = value
                maxColumn = maxOf(maxColumn, cellIndex)
                cellIndex = -1
            } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "row" && rowCells != null) {
                rows += (0..maxColumn).map { rowCells!![it].orEmpty() }
                ExcelImportGuard.requireRowCount(rows.size)
                rowCells = null
            }
        }
        return rows
    }

    private fun parser(bytes: ByteArray): XmlPullParser {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(ByteArrayInputStream(bytes), "UTF-8")
        return p
    }

    private fun attributeByLocalName(parser: XmlPullParser, local: String): String? {
        for (i in 0 until parser.attributeCount) if (parser.getAttributeName(i).substringAfter(':') == local) return parser.getAttributeValue(i)
        return null
    }

    private fun normalizeSheetPath(target: String): String =
        ExcelImportGuard.worksheetPath(target)

    private fun columnName(number: Int): String {
        var n = number
        val result = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            result.insert(0, ('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return result.toString()
    }

    private fun columnIndex(reference: String): Int {
        var value = 0
        var found = false
        for (ch in reference) {
            if (!ch.isLetter()) break
            value = value * 26 + (ch.uppercaseChar().code - 'A'.code + 1)
            found = true
        }
        return if (found) value - 1 else -1
    }

    private fun xml(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;")
                '"' -> append("&quot;"); '\'' -> append("&apos;")
                else -> if (ch == '\t' || ch == '\n' || ch == '\r' || ch.code >= 0x20) append(ch)
            }
        }
    }
}
