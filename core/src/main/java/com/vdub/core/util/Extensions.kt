package com.vdub.core.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Format milliseconds to mm:ss or hh:mm:ss
 */
fun Long.formatDuration(): String {
    val hours = this / 3600000
    val minutes = (this % 3600000) / 60000
    val seconds = (this % 60000) / 1000
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Format file size to human readable string.
 */
fun Long.formatFileSize(): String = when {
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> "%.1f KB".format(this / 1024.0)
    this < 1024 * 1024 * 1024 -> "%.1f MB".format(this / (1024.0 * 1024))
    else -> "%.1f GB".format(this / (1024.0 * 1024 * 1024))
}

/**
 * Format timestamp for display.
 */
fun Long.formatTimestamp(): String {
    val hours = this / 3600000
    val minutes = (this % 3600000) / 60000
    val seconds = (this % 60000) / 1000
    val millis = this % 1000
    return if (hours > 0) {
        String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    } else {
        String.format("%02d:%02d.%03d", minutes, seconds, millis)
    }
}

/**
 * Format date from timestamp.
 */
fun Long.formatDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Copy Uri content to a local file.
 */
fun Uri.copyToLocalFile(context: Context, destination: File) {
    context.contentResolver.openInputStream(this)?.use { input ->
        FileOutputStream(destination).use { output ->
            input.copyTo(output)
        }
    }
}

/**
 * Get file extension from path.
 */
fun String.fileExtension(): String = this.substringAfterLast('.', "").lowercase()

/**
 * Get file name from path.
 */
fun String.fileName(): String = this.substringAfterLast('/')

/**
 * Clamp a float value between min and max.
 */
fun Float.clamp(min: Float, max: Float): Float = this.coerceIn(min, max)

/**
 * Clamp an int value between min and max.
 */
fun Int.clamp(min: Int, max: Int): Int = this.coerceIn(min, max)
