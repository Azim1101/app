package com.vdub.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.vdub.app.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VDubApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        enableFfmpegLoggingIfAvailable()
        NotificationHelper.createNotificationChannel(this)

        Log.i(TAG, "VDub Application initialized")
    }

    private fun enableFfmpegLoggingIfAvailable() {
        runCatching {
            val configClass = Class.forName("com.arthenica.ffmpegkit.FFmpegKitConfig")
            configClass.getMethod("enableRedirection").invoke(null)
        }.onSuccess {
            Log.i(TAG, "FFmpegKit redirection enabled")
        }.onFailure {
            Log.i(TAG, "FFmpegKitConfig not available, skipping log redirection")
        }
    }

    companion object {
        private const val TAG = "VDubApp"
    }
}
