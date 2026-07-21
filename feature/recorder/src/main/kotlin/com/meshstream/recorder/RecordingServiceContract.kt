
package com.meshstream.recorder

import com.meshstream.core.domain.RecordingSession

enum class RecordingStatus {
    IDLE,
    RECORDING,
    STOPPED
}

interface RecordingServiceContract {
    fun start(session: RecordingSession)
    fun stop()
    fun status(): RecordingStatus
}
