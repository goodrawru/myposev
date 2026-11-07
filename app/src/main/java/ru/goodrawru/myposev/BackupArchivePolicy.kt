package ru.goodrawru.myposev

/**
 * Backups are stored as ZIP, but their uncompressed JSON size must be bounded.
 * Apply the same limit during both export and import so this app never creates
 * an archive it will immediately reject on restore.
 */
internal object BackupArchivePolicy {
    const val MAX_UNCOMPRESSED_BYTES = 5 * 1024 * 1024

    fun requireSupportedSize(byteCount: Int) {
        require(byteCount in 0..MAX_UNCOMPRESSED_BYTES) {
            "Файл резервной копии слишком большой (максимум 5 МБ)"
        }
    }
}
