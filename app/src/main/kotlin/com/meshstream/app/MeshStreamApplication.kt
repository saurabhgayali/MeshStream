package com.meshstream.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for MeshStream. Annotated with [@HiltAndroidApp] to trigger
 * Hilt's code generation and set up the application-level dependency injection component.
 */
@HiltAndroidApp
class MeshStreamApplication : Application()
