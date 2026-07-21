package com.meshstream.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RecordingSessionTest {

    @Test
    fun `new session is active when endedAt is null`() {
        val session = RecordingSession(
            id = UUID.randomUUID(),
            startedAt = Instant.now(),
            endedAt = null,
        )
        assertTrue(session.isActive)
    }

    @Test
    fun `session is not active when endedAt is set`() {
        val session = RecordingSession(
            id = UUID.randomUUID(),
            startedAt = Instant.now(),
            endedAt = Instant.now(),
        )
        assertFalse(session.isActive)
    }

    @Test
    fun `default chunk duration is 30 seconds`() {
        val session = RecordingSession()
        assertEquals(30_000L, session.chunkDurationMs)
    }

    @Test
    fun `default resolution is HD_720P`() {
        val session = RecordingSession()
        assertEquals(VideoResolution.HD_720P, session.resolution)
    }

    @Test
    fun `default frame rate is 30`() {
        val session = RecordingSession()
        assertEquals(30, session.frameRate)
    }

    @Test
    fun `default totalChunks is zero`() {
        val session = RecordingSession()
        assertEquals(0, session.totalChunks)
    }

    @Test
    fun `default endedAt is null`() {
        val session = RecordingSession()
        assertNull(session.endedAt)
    }

    @Test
    fun `VideoResolution SD_480P has correct dimensions`() {
        assertEquals(854, VideoResolution.SD_480P.width)
        assertEquals(480, VideoResolution.SD_480P.height)
    }

    @Test
    fun `VideoResolution HD_720P has correct dimensions`() {
        assertEquals(1280, VideoResolution.HD_720P.width)
        assertEquals(720, VideoResolution.HD_720P.height)
    }

    @Test
    fun `VideoResolution FHD_1080P has correct dimensions`() {
        assertEquals(1920, VideoResolution.FHD_1080P.width)
        assertEquals(1080, VideoResolution.FHD_1080P.height)
    }
}
