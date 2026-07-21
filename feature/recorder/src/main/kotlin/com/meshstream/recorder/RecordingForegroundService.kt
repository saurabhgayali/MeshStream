package com.meshstream.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meshstream.core.model.RecordingSession
import com.meshstream.core.usecase.RecordAndChunkUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground [Service] that manages the lifecycle of a [ChunkRecorder].
 *
 * The service:
 * 1. Posts a mandatory foreground notification (required for background camera access).
 * 2. Creates a new [RecordingSession] and calls [RecordAndChunkUseCase.startSession].
 * 3. Starts the [ChunkRecorder] and collects produced [VideoChunk]s.
 * 4. For each chunk, calls [RecordAndChunkUseCase.onChunkProduced] to persist it.
 * 5. On stop intent, calls [ChunkRecorder.stop] and [RecordAndChunkUseCase.endSession].
 *
 * Start the service with [ACTION_START]; stop with [ACTION_STOP].
 */
@AndroidEntryPoint
class RecordingForegroundService : Service() {

    @Inject lateinit var chunkRecorder: ChunkRecorder
    @Inject lateinit var recordAndChunkUseCase: RecordAndChunkUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeSession: RecordingSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    private fun handleStart() {
        val session = RecordingSession()
        activeSession = session

        startForeground(NOTIFICATION_ID, buildNotification("Recording…"))

        serviceScope.launch {
            recordAndChunkUseCase.startSession(session)
            chunkRecorder.start(session).collect { chunk ->
                recordAndChunkUseCase.onChunkProduced(chunk)
            }
        }
    }

    private fun handleStop() {
        serviceScope.launch {
            chunkRecorder.stop()
            activeSession?.let { recordAndChunkUseCase.endSession(it.id) }
            activeSession = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MeshStream Recording",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Indicates that MeshStream is recording video." }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MeshStream")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

    companion object {
        const val ACTION_START = "com.meshstream.recorder.ACTION_START"
        const val ACTION_STOP = "com.meshstream.recorder.ACTION_STOP"

        private const val CHANNEL_ID = "meshstream_recording"
        private const val NOTIFICATION_ID = 1001
    }
}
