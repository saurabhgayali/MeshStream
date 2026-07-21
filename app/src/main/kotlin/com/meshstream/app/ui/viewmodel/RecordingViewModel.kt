package com.meshstream.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshstream.core.model.ChunkStatus
import com.meshstream.recorder.RecordingForegroundService
import com.meshstream.storage.ChunkStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for [RecordingScreen].
 *
 * Exposes [UiState] derived from [ChunkStore] and manages the lifecycle of
 * [RecordingForegroundService] via [startRecording] / [stopRecording].
 */
@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chunkStore: ChunkStore,
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)

    val uiState: StateFlow<UiState> = combine(
        _isRecording,
        chunkStore.observeAll(),
        chunkStore.observeByStatus(ChunkStatus.DELIVERED),
    ) { isRecording, allChunks, deliveredChunks ->
        UiState(
            isRecording = isRecording,
            chunksRecorded = allChunks.size,
            chunksDelivered = deliveredChunks.size,
            nearbyPeers = 0, // Populated in a future phase via NodeRepository
            storageUsedBytes = allChunks.sumOf { it.sizeBytes },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(),
    )

    fun startRecording() {
        _isRecording.update { true }
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopRecording() {
        _isRecording.update { false }
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    data class UiState(
        val isRecording: Boolean = false,
        val chunksRecorded: Int = 0,
        val chunksDelivered: Int = 0,
        val nearbyPeers: Int = 0,
        val storageUsedBytes: Long = 0L,
    )
}
