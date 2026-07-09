package com.vdub.feature.processing

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vdub.core.util.formatDuration
import com.vdub.core.util.formatTimestamp
import com.vdub.core.theme.SpeakerColors
import com.vdub.domain.entity.AnalysisResult
import com.vdub.domain.entity.AnalysisStatus
import com.vdub.domain.entity.ExportFormat
import com.vdub.domain.entity.ProcessingProgress
import com.vdub.domain.entity.Segment
import com.vdub.domain.entity.Speaker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (Long) -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val progress by viewModel.progress.collectAsState()
    val result by viewModel.result.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val speakers by viewModel.speakers.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()

    val isComplete = progress.status == AnalysisStatus.COMPLETED
    val isFailed = progress.status == AnalysisStatus.FAILED
    val isPaused = progress.isPaused

    LaunchedEffect(Unit) {
        viewModel.startProcessing()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isComplete) "Results" else "Processing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isComplete) {
                        var showExportMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(Icons.Default.Share, contentDescription = "Export")
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                ExportFormat.entries.forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format.name) },
                                        onClick = {
                                            viewModel.exportResult(format)
                                            showExportMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress section
            if (!isComplete && !isFailed) {
                item {
                    ProcessingProgressCard(
                        progress = progress,
                        elapsedTime = elapsedTime,
                        isPaused = isPaused,
                        onPause = { viewModel.pauseProcessing() },
                        onResume = { viewModel.resumeProcessing() },
                        onCancel = { viewModel.cancelProcessing() }
                    )
                }
            }

            // Error section
            if (isFailed) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(progress.currentStep, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Waveform
            item {
                WaveformView(
                    segments = segments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            }

            // Live segments
            if (segments.isNotEmpty()) {
                item {
                    Text(
                        "Segments (${segments.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(segments) { segment ->
                    SegmentItem(
                        segment = segment,
                        speakerColor = SpeakerColors.getOrElse(segment.clusterId) { Color.Gray }
                    )
                }
            }

            // Statistics (when complete)
            if (isComplete && result != null) {
                item {
                    StatisticsCard(result!!)
                }

                item {
                    Text(
                        "Speakers (${speakers.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(speakers) { speaker ->
                    SpeakerCard(speaker = speaker)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ProcessingProgressCard(
    progress: ProcessingProgress,
    elapsedTime: Long,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    progress.currentStep,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    elapsedTime.formatDuration(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${(progress.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "${progress.segmentsDetected} segments detected",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPaused) {
                    TextButton(onClick = onResume) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume")
                    }
                } else {
                    TextButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }
                }
                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun WaveformView(
    segments: List<Segment>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Draw speaker regions
            if (segments.isNotEmpty()) {
                val maxTime = segments.maxOf { it.endTimeMs }.toFloat().coerceAtLeast(1f)

                segments.forEach { segment ->
                    val startX = (segment.startTimeMs / maxTime) * width
                    val endX = (segment.endTimeMs / maxTime) * width
                    val color = SpeakerColors.getOrElse(segment.clusterId) { Color.Gray }

                    drawLine(
                        color = color.copy(alpha = 0.4f),
                        start = Offset(startX, 0f),
                        end = Offset(startX, height),
                        strokeWidth = (endX - startX).coerceAtLeast(1f),
                        cap = StrokeCap.Butt
                    )
                }

                // Draw center line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1f
                )
            }
        }
    }
}

@Composable
private fun SegmentItem(
    segment: Segment,
    speakerColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = speakerColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(speakerColor, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    segment.speakerLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = speakerColor
                )
                Text(
                    segment.formatTimestamp(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "${(segment.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatisticsCard(result: AnalysisResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Statistics", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            StatRow("Duration", result.durationMs.formatDuration())
            StatRow("Speech", result.speechDurationMs.formatDuration())
            StatRow("Silence", result.silenceDurationMs.formatDuration())
            StatRow("Speakers", result.speakerCount.toString())
            StatRow("Segments", result.segmentCount.toString())
            StatRow("Confidence", "${(result.averageConfidence * 100).toInt()}%")
            StatRow("Processing", result.processingTimeMs.formatDuration())
            StatRow("Model", result.modelUsed)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SpeakerCard(speaker: Speaker) {
    val speakerColor = SpeakerColors.getOrElse(speaker.clusterId) { Color.Gray }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(speakerColor, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    speaker.label.substringAfterLast(" "),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(speaker.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${speaker.segmentCount} segments, ${speaker.totalDurationMs.formatDuration()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${(speaker.averageConfidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
