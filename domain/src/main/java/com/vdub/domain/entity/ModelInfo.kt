package com.vdub.domain.entity

import kotlinx.serialization.Serializable

/**
 * Represents a downloadable ML model.
 */
@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val type: ModelType,
    val downloadUrl: String,
    val mirrorUrls: List<String> = emptyList(),
    val sha256: String = "",
    val fileSize: Long = 0,
    val sampleRate: Int = 16000,
    val localPath: String = "",
    val isDownloaded: Boolean = false,
    val isBuiltIn: Boolean = false,
    val category: ModelCategory = ModelCategory.DIARIZATION,
    val tags: List<String> = emptyList()
)

enum class ModelType {
    SEGMENTATION,
    EMBEDDING,
    VAD,
    ASR,
    CUSTOM
}

enum class ModelCategory {
    DIARIZATION,
    SPEAKER_RECOGNITION,
    SPEECH_RECOGNITION,
    VOICE_ACTIVITY,
    CUSTOM
}

/**
 * State of a model download.
 */
@Serializable
data class ModelDownloadState(
    val modelId: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null,
    val speed: Long = 0
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    VERIFYING,
    VERIFY_FAILED
}

/**
 * Registry of all available models.
 */
object ModelRegistry {
    val MODELS = listOf(
        ModelInfo(
            id = "pyannote-segmentation-3.0",
            name = "Pyannote Segmentation 3.0",
            description = "Speech activity detection, segmentation, and timestamp generation. Based on pyannote.audio 3.0.",
            version = "3.0.0",
            type = ModelType.SEGMENTATION,
            downloadUrl = "https://huggingface.co/onnx-community/pyannote-segmentation-3.0/resolve/main/onnx/model.onnx",
            sha256 = "auto",
            fileSize = 17_000_000,
            sampleRate = 16000,
            localPath = "pyannote-segmentation-3.0/model.onnx",
            category = ModelCategory.DIARIZATION,
            tags = listOf("segmentation", "vad", "timestamps", "required")
        ),
        ModelInfo(
            id = "speechbrain-ecapa",
            name = "SpeechBrain ECAPA-TDNN",
            description = "Speaker embedding model based on ECAPA-TDNN trained on VoxCeleb. Generates 192-dimensional speaker embeddings.",
            version = "1.0.0",
            type = ModelType.EMBEDDING,
            downloadUrl = "https://huggingface.co/speechbrain/spkrec-ecapa-voxceleb/resolve/main/embedding_model.ckpt",
            sha256 = "auto",
            fileSize = 80_000_000,
            sampleRate = 16000,
            localPath = "speechbrain-ecapa/model.onnx",
            category = ModelCategory.SPEAKER_RECOGNITION,
            tags = listOf("embedding", "required")
        ),
        ModelInfo(
            id = "silero-vad",
            name = "Silero VAD",
            description = "Lightweight voice activity detection model. Fast and accurate for speech/non-speech detection.",
            version = "1.0.0",
            type = ModelType.VAD,
            downloadUrl = "https://huggingface.co/onnx-community/silero-vad/resolve/main/onnx/model.onnx",
            sha256 = "auto",
            fileSize = 2_000_000,
            sampleRate = 16000,
            localPath = "silero-vad/model.onnx",
            category = ModelCategory.VOICE_ACTIVITY,
            tags = listOf("vad", "lightweight")
        ),
        ModelInfo(
            id = "sherpa-onnx-speaker",
            name = "Sherpa ONNX Speaker Embedding",
            description = "Speaker embedding model from Sherpa-ONNX project. Alternative to ECAPA.",
            version = "1.0.0",
            type = ModelType.EMBEDDING,
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-recon-models/3dspeaker_speech_eres2net_base_sv_zh-cn_3d-speaker_onnx.tar.bz2",
            sha256 = "auto",
            fileSize = 65_000_000,
            sampleRate = 16000,
            localPath = "sherpa-speaker/model.onnx",
            category = ModelCategory.SPEAKER_RECOGNITION,
            tags = listOf("embedding", "alternative")
        ),
        ModelInfo(
            id = "whisper-tiny",
            name = "Whisper Tiny",
            description = "OpenAI Whisper ASR model (tiny variant). 39M parameters, fast transcription.",
            version = "1.0.0",
            type = ModelType.ASR,
            downloadUrl = "https://huggingface.co/onnx-community/whisper-tiny/resolve/main/onnx/encoder_model.onnx",
            sha256 = "auto",
            fileSize = 75_000_000,
            sampleRate = 16000,
            localPath = "whisper-tiny/encoder_model.onnx",
            category = ModelCategory.SPEECH_RECOGNITION,
            tags = listOf("asr", "transcription", "tiny")
        ),
        ModelInfo(
            id = "whisper-base",
            name = "Whisper Base",
            description = "OpenAI Whisper ASR model (base variant). 74M parameters.",
            version = "1.0.0",
            type = ModelType.ASR,
            downloadUrl = "https://huggingface.co/onnx-community/whisper-base/resolve/main/onnx/encoder_model.onnx",
            sha256 = "auto",
            fileSize = 150_000_000,
            sampleRate = 16000,
            localPath = "whisper-base/encoder_model.onnx",
            category = ModelCategory.SPEECH_RECOGNITION,
            tags = listOf("asr", "transcription", "base")
        ),
        ModelInfo(
            id = "whisper-small",
            name = "Whisper Small",
            description = "OpenAI Whisper ASR model (small variant). 244M parameters.",
            version = "1.0.0",
            type = ModelType.ASR,
            downloadUrl = "https://huggingface.co/onnx-community/whisper-small/resolve/main/onnx/encoder_model.onnx",
            sha256 = "auto",
            fileSize = 500_000_000,
            sampleRate = 16000,
            localPath = "whisper-small/encoder_model.onnx",
            category = ModelCategory.SPEECH_RECOGNITION,
            tags = listOf("asr", "transcription", "small")
        ),
        ModelInfo(
            id = "whisper-medium",
            name = "Whisper Medium",
            description = "OpenAI Whisper ASR model (medium variant). 769M parameters.",
            version = "1.0.0",
            type = ModelType.ASR,
            downloadUrl = "https://huggingface.co/onnx-community/whisper-medium/resolve/main/onnx/encoder_model.onnx",
            sha256 = "auto",
            fileSize = 1_500_000_000,
            sampleRate = 16000,
            localPath = "whisper-medium/encoder_model.onnx",
            category = ModelCategory.SPEECH_RECOGNITION,
            tags = listOf("asr", "transcription", "medium")
        ),
        ModelInfo(
            id = "whisper-large",
            name = "Whisper Large",
            description = "OpenAI Whisper ASR model (large variant). 1550M parameters. Best accuracy.",
            version = "1.0.0",
            type = ModelType.ASR,
            downloadUrl = "https://huggingface.co/onnx-community/whisper-large-v3/resolve/main/onnx/encoder_model.onnx",
            sha256 = "auto",
            fileSize = 3_000_000_000,
            sampleRate = 16000,
            localPath = "whisper-large/encoder_model.onnx",
            category = ModelCategory.SPEECH_RECOGNITION,
            tags = listOf("asr", "transcription", "large")
        )
    )

    fun getRequiredModels(): List<ModelInfo> = MODELS.filter { 
        it.tags.contains("required") 
    }

    fun getById(id: String): ModelInfo? = MODELS.find { it.id == id }

    fun getByType(type: ModelType): List<ModelInfo> = MODELS.filter { it.type == type }

    fun getByCategory(category: ModelCategory): List<ModelInfo> = MODELS.filter { it.category == category }
}
