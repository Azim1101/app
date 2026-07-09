# VDub Architecture Documentation

## Overview

VDub follows Clean Architecture principles with a multi-module structure that separates concerns across layers:

- **Presentation Layer**: Compose UI, ViewModels, Navigation
- **Domain Layer**: Entities, Repository Interfaces, Use Cases
- **Data Layer**: Repository Implementations, Data Sources
- **ML Layer**: ONNX Runtime, Model Inference, Clustering
- **Media Layer**: FFmpeg Processing, Audio I/O, Waveform
- **Database Layer**: Room Database, DAOs, Entities

## Module Dependencies

```
app → core, domain, data, ml, media, database, feature-*
feature-* → core, domain
data → domain, database
ml → domain
media → domain
database → domain
```

The `domain` module has no external dependencies and defines the business contracts.

## Key Components

### ONNX Runtime Integration

The `ONNXRuntimeManager` manages model sessions with support for:
- CPU inference with XNNPACK optimization
- NNAPI delegate for Android Neural Networks API
- GPU delegate support
- Configurable thread count and memory optimization

### Speaker Diarization Pipeline

1. **Audio Extraction**: FFmpeg extracts and converts audio to 16kHz mono WAV
2. **Segmentation**: Pyannote model detects speech segments with timestamps
3. **Embedding**: ECAPA-TDNN generates 192-dim embeddings per segment
4. **Clustering**: Agglomerative clustering groups segments by speaker
5. **Timeline**: Final timeline with speaker labels and timestamps

### Live Processing

The segmentation model processes audio in overlapping windows and emits segments as they are detected via Kotlin Flow. The UI observes this Flow and updates in real-time without waiting for complete processing.

### Model Management

Models are downloaded from HuggingFace using OkHttp with:
- Resume support via Range headers
- SHA256 checksum verification
- Pause/resume/retry functionality
- Multiple mirror URLs for fallback

### Background Processing

WorkManager handles long-running diarization tasks:
- Persistent notification during processing
- Survives app minimization
- Resumes after device reboot
- Coroutine-based with cancellable jobs

## Data Flow

```
UI (Compose) → ViewModel → UseCase → Repository → Data Source
                                                      ↓
                                              ML Models (ONNX)
                                              FFmpeg (Media)
                                              Room (Database)
                                              OkHttp (Downloads)
                                              DataStore (Settings)
```

## Threading Model

- **UI Thread**: Compose rendering, user interactions
- **Main Dispatcher**: ViewModel operations, Flow collection
- **IO Dispatcher**: File I/O, database, network, FFmpeg
- **Default Dispatcher**: ML inference, audio processing, clustering

## Error Handling

- Repository layer returns `Result<T>` for suspend functions
- Flow-based APIs emit error states
- UI shows Snackbar for user-facing errors
- WorkManager handles background failures with retry logic

## Memory Management

- ONNX sessions are cached and reused
- Audio data is processed in chunks for large files
- Low RAM mode disables memory pattern optimization
- ProGuard removes unused code in release builds
