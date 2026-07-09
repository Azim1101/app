package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Represents a media file that can be processed.
 */
@Serializable
data class MediaFile(
    val name: String,
    val path: String,
    val size: Long = 0,
    val mimeType: String = "",
    val mediaType: MediaType = MediaType.AUDIO,
    val durationMs: Long = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val bitrate: Long = 0
) {
    val extension: String get() = name.substringAfterLast('.', "")
    val formattedSize: String get() = formatFileSize(size)

    companion object {
        val SUPPORTED_AUDIO_EXTENSIONS = setOf("wav", "mp3", "flac", "aac", "m4a", "ogg", "opus")
        val SUPPORTED_VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "mpeg", "3gp")
        val SUPPORTED_EXTENSIONS = SUPPORTED_AUDIO_EXTENSIONS + SUPPORTED_VIDEO_EXTENSIONS

        fun isSupported(name: String): Boolean {
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in SUPPORTED_EXTENSIONS
        }

        fun formatFileSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
