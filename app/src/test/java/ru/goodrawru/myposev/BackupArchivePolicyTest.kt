package ru.goodrawru.myposev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

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
}
