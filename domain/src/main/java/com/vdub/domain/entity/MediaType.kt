package com.vdub.domain.entity

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    AUDIO,
    VIDEO;

    companion object {
        fun fromMimeType(mimeType: String): MediaType = when {
            mimeType.startsWith("audio/") -> AUDIO
            mimeType.startsWith("video/") -> VIDEO
            else -> AUDIO
        }

        fun fromExtension(ext: String): MediaType = when (ext.lowercase()) {
            "mp4", "mkv", "avi", "mov", "webm", "mpeg", "3gp" -> VIDEO
            "wav", "mp3", "flac", "aac", "m4a", "ogg", "opus" -> AUDIO
            else -> AUDIO
        }
    }
}
