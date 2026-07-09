package com.vdub.core.util

import android.Manifest
import android.os.Build

/**
 * Required permissions for VDub application.
 */
object VDubPermissions {
    val STORAGE_READ = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val STORAGE_VIDEO_READ = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val NOTIFICATION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null

    val FOREGROUND_SERVICE = Manifest.permission.FOREGROUND_SERVICE
    val FOREGROUND_SERVICE_DATA_SYNC = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
    } else null

    fun getAllPermissions(): List<String> {
        val perms = mutableListOf(
            STORAGE_READ,
            STORAGE_VIDEO_READ,
            FOREGROUND_SERVICE
        )
        NOTIFICATION?.let { perms.add(it) }
        FOREGROUND_SERVICE_DATA_SYNC?.let { perms.add(it) }
        return perms
    }
}
