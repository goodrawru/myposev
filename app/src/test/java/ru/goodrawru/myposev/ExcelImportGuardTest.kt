package ru.goodrawru.myposev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelImportGuardTest {
    @Test fun acceptsNormalArchivePathsAndRejectsTraversal() {
        assertEquals("xl/workbook.xml", ExcelImportGuard.zipEntryPath("xl/workbook.xml"))
        assertEquals("xl/", ExcelImportGuard.zipEntryPath("xl/"))
        for (path in listOf("../outside.xml", "xl/../outside.xml", "/xl/workbook.xml",
            "xl\\workbook.xml", "http://example.com/sheet.xml", "xl//sheet.xml")) {
            assertThrows(IllegalArgumentException::class.java) {
                ExcelImportGuard.zipEntryPath(path)
            }
        }
    }

    @Test fun onlyLocalWorksheetTargetsAreAccepted() {
        for (path in listOf("worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "/xl/worksheets/sheet3.xml")) {
            assertTrue(ExcelImportGuard.worksheetPath(path).startsWith("xl/worksheets/"))
        }
        for (path in listOf("../outside.xml", "http://host/file.xml",
            "worksheets/../../evil.xml", "/xl/externalLinks/link1.xml",
            "worksheets/another.xlsx")) {
            assertThrows(IllegalArgumentException::class.java) {
                ExcelImportGuard.worksheetPath(path)
            }
        }
    }

    @Test fun boundedCellAddressesPreventUnboundedRowAllocation() {
        assertEquals(0, ExcelImportGuard.cellColumn("A1"))
        assertEquals(25, ExcelImportGuard.cellColumn("Z42"))
        assertEquals(26, ExcelImportGuard.cellColumn("AA2"))
        for (reference in listOf("ZZ999", "XFD1", "AAAA1", "A0", "A-1", "1A", "", "A")) {
            assertThrows(IllegalArgumentException::class.java) {
                ExcelImportGuard.cellColumn(reference)
            }
        }
    }

    @Test fun oversizedRowsCellsAndStringTablesAreRejected() {
        ExcelImportGuard.requireRowCount(ExcelImportGuard.MAX_ROWS_PER_SHEET)
        ExcelImportGuard.requireCellLength(ExcelImportGuard.MAX_CELL_CHARS)
        ExcelImportGuard.requireSharedStringCount(ExcelImportGuard.MAX_SHARED_STRINGS)
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireRowCount(ExcelImportGuard.MAX_ROWS_PER_SHEET + 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireCellLength(ExcelImportGuard.MAX_CELL_CHARS + 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireSharedStringCount(ExcelImportGuard.MAX_SHARED_STRINGS + 1)
        }
    }

    @Test fun allEditableColumnsMustExistExactlyOnce() {
        val header = listOf("ID", "Культура", "Дата", "Технические данные (не изменять)")
        ExcelImportGuard.requireHeaders(header, header, "Посадки")
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireHeaders(header, header + "Место", "Посадки")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireHeaders(header + "ID", header, "Посадки")
        }
    }

    @Test fun booleanParsingDoesNotTurnTyposIntoFalse() {
        assertTrue(ExcelImportGuard.parseBoolean("ДА"))
        assertTrue(ExcelImportGuard.parseBoolean("true"))
        assertFalse(ExcelImportGuard.parseBoolean("Нет"))
        assertFalse(ExcelImportGuard.parseBoolean("0"))
        assertFalse(ExcelImportGuard.parseBoolean("", allowBlank = true))
        for (value in listOf("", "yes", "тру", "2", "na")) {
            assertThrows(IllegalArgumentException::class.java) {
                ExcelImportGuard.parseBoolean(value)
            }
        }
    }

    @Test fun rejectsWorkbookLargerThanItsOwnImportLimit() {
        ExcelImportGuard.requireWorkbookSize(1)
        ExcelImportGuard.requireWorkbookSize(ExcelImportGuard.MAX_FILE_BYTES)
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireWorkbookSize(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireWorkbookSize(ExcelImportGuard.MAX_FILE_BYTES + 1)
        }
    }

    @Test fun rejectsRepeatedHiddenPreferenceKeys() {
        val keys = mutableSetOf<String>()
        ExcelImportGuard.requireUniquePreferenceKey("garden_city", keys)
        ExcelImportGuard.requireUniquePreferenceKey("care_tasks", keys)
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireUniquePreferenceKey("garden_city", keys)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireUniquePreferenceKey("", keys)
        }
    }

    @Test fun notesAndSettingsMustKeepRequiredColumns() {
        val noteHeaders = listOf("Дата", "Заметка", "Ключ записи")
        ExcelImportGuard.requireHeaders(noteHeaders, noteHeaders, "Заметки")
        val settingsHeaders = listOf("Параметр", "Значение", "Ключ", "Тип")
        ExcelImportGuard.requireHeaders(settingsHeaders, settingsHeaders, "Настройки")
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireHeaders(listOf("Дата", "Заметка"), noteHeaders, "Заметки")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExcelImportGuard.requireHeaders(listOf("Параметр", "Значение", "Ключ"),
                settingsHeaders, "Настройки")
        }
    }

    @Test fun corruptNumericSettingsAreRejectedInsteadOfReset() {
        assertEquals(19, ExcelImportGuard.parseInt("19"))
        assertEquals(5000000000L, ExcelImportGuard.parseLong("5000000000"))
        assertEquals(2.5f, ExcelImportGuard.parseFloat("2.5"))
        assertThrows(IllegalArgumentException::class.java) { ExcelImportGuard.parseInt("oops") }
        assertThrows(IllegalArgumentException::class.java) { ExcelImportGuard.parseLong("") }
        assertThrows(IllegalArgumentException::class.java) { ExcelImportGuard.parseFloat("NaN") }
        assertThrows(IllegalArgumentException::class.java) { ExcelImportGuard.parseFloat("Infinity") }
    }
}
