package com.vdub.domain.entity

import kotlinx.serialization.Serializable

@Serializable
enum class ExportFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    SRT("srt", "text/srt"),
    VTT("vtt", "text/vtt"),
    RTTM("rttm", "text/plain");

    companion object {
        fun fromExtension(ext: String): ExportFormat = entries.find { it.extension == ext.lowercase() } ?: TXT
    }
}
