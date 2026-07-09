# VDub - Speaker Diarization App

VDub is a production-ready Android application for on-device speaker diarization. It allows users to upload audio or video files and detect speakers, generate timelines, and export results — all running 100% offline on the device.

## Features

### Core Functionality
- **Speaker Diarization**: Detect and label speakers in audio/video
- **Speech Segmentation**: Identify speech vs. silence with timestamps
- **Speaker Embeddings**: Generate 192-dimensional speaker embeddings
- **Live Processing**: See segments as they're detected in real-time
- **Background Processing**: Continue processing when app is minimized

### Media Support
- **Audio formats**: WAV, MP3, FLAC, AAC, M4A, OGG, OPUS
- **Video formats**: MP4, MKV, AVI, MOV, WebM, MPEG, 3GP
- **Audio extraction**: Extract audio from video using FFmpeg
- **Media conversion**: Convert between formats, resample, normalize
- **Trim & merge**: Cut and join audio segments

### Speaker Analysis
- **Agglomerative clustering**: Automatic speaker grouping
- **Cosine similarity**: Embedding-based speaker matching
- **Dynamic threshold**: Automatic speaker count estimation
- **Manual control**: Adjust threshold, rename, merge, delete speakers

### Model Management
- **Pyannote Segmentation 3.0**: Speech activity detection & segmentation
- **SpeechBrain ECAPA-TDNN**: Speaker embedding generation
- **Silero VAD**: Lightweight voice activity detection
- **Whisper models**: Speech recognition (tiny through large)
- **Download manager**: Pause, resume, retry, verify checksums

### Export Formats
- TXT, CSV, JSON, SRT, VTT, RTTM

### UI/UX
- Material 3 / Material You design
- Interactive waveform with speaker highlighting
- Live progress with segment updates
- Dark/Light/Dynamic themes
- Tablet and foldable support

## Architecture

The project follows **Clean Architecture** with multi-module structure:

```
VDub/
├── app/                    # Main application, navigation, DI
├── core/                   # Shared theme, utilities, components
├── domain/                 # Entities, repository interfaces, use cases
├── data/                   # Repository implementations, data sources
├── ml/                     # ONNX Runtime, ML models, clustering
├── media/                  # FFmpeg, audio processing, waveform
├── database/               # Room database, DAOs, entities
├── feature-home/           # Home screen with upload
├── feature-models/         # Model manager with downloads
├── feature-processing/     # Processing screen with live updates
├── feature-history/        # Analysis history
└── feature-settings/       # App settings
```

### Processing Pipeline

```
Upload → Extract Audio → Resample → Segmentation → Chunk Generation
→ Embedding → Speaker Clustering → Timeline → Export
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Clean Architecture |
| DI | Hilt |
| Database | Room |
| Navigation | Navigation Compose |
| Async | Coroutines, Flow |
| ML | ONNX Runtime Android |
| Media | FFmpeg Kit, Media3 |
| Background | WorkManager |
| Serialization | Kotlin Serialization |
| HTTP | OkHttp |
| Storage | DataStore Preferences |

## ML Models

### Required Models
1. **Pyannote Segmentation 3.0** (ONNX)
   - Source: [onnx-community/pyannote-segmentation-3.0](https://huggingface.co/onnx-community/pyannote-segmentation-3.0)
   - Purpose: Speech activity detection, segmentation, timestamps
   - Size: ~17MB

2. **SpeechBrain ECAPA-TDNN**
   - Source: [speechbrain/spkrec-ecapa-voxceleb](https://huggingface.co/speechbrain/spkrec-ecapa-voxceleb)
   - Purpose: 192-dimensional speaker embeddings
   - Size: ~80MB

### Optional Models
- Silero VAD (~2MB)
- Sherpa ONNX Speaker (~65MB)
- Whisper Tiny/Base/Small/Medium/Large
- Future custom ONNX models

## Building

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35
- NDK (for ONNX Runtime)

### Steps
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Build the project
5. Deploy to device

### Build Variants
- `debug`: No minification, debuggable
- `release`: Full minification with ProGuard

## Permissions

- `READ_MEDIA_AUDIO` / `READ_MEDIA_VIDEO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)
- `POST_NOTIFICATIONS` (Android 13+)
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `INTERNET` (for model downloads only)

## Configuration

### Compute Backends
- **CPU**: Default, uses XNNPACK for optimization
- **NNAPI**: Android Neural Networks API (device-dependent)
- **GPU**: GPU delegate support (device-dependent)

### Settings
- Theme: System/Light/Dark/Dynamic Color
- Sample rate: 8000-48000 Hz
- Thread count: 1-8
- Clustering threshold: 0.3-0.95
- Max speakers: 2-20
- Chunk size: 1-30 seconds
- Low RAM mode for memory-constrained devices

## Performance Tips

1. Use NNAPI backend on supported devices for faster inference
2. Enable GPU delegate for compatible GPUs
3. Increase thread count on high-end devices
4. Use Low RAM mode on devices with <4GB RAM
5. Larger chunk sizes improve accuracy but increase latency

## License

Copyright 2024 VDub. All rights reserved.
