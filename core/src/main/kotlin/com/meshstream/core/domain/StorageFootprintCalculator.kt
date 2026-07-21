
package com.meshstream.core.domain

class StorageFootprintCalculator {
    fun calculateMaxRecordingDuration(freeStorageBytes: Long, videoBitrateBps: Long): Long {
        if (freeStorageBytes <= 0 || videoBitrateBps <= 0) {
            return 0L
        }

        // The 2.2x multiplier accounts for the master recording, active chunk copies,
        // and temporary processing overhead that must remain available during capture.
        val safeBudgetBits = (freeStorageBytes.toDouble() * 8.0) / 2.2
        return (safeBudgetBits / videoBitrateBps).toLong()
    }

    fun shouldStopRecording(freeStorageBytes: Long, minimumFreeBytes: Long): Boolean {
        return freeStorageBytes < minimumFreeBytes
    }
}
