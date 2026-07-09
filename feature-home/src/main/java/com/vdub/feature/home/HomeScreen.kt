package com.vdub.feature.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vdub.core.util.formatDuration
import com.vdub.core.util.formatFileSize
import com.vdub.domain.entity.MediaFile
import com.vdub.domain.entity.MediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToModels: () -> Unit,
    onNavigateToProcessing: (String, MediaType) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPickFile: () -> Unit,
    pendingFileUri: Uri?,
    onFileUriConsumed: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsState()
    val modelsReady by viewModel.modelsReady.collectAsState()
    val error by viewModel.error.collectAsState()

    // Handle pending file URI
    LaunchedEffect(pendingFileUri) {
        pendingFileUri?.let { uri ->
            viewModel.handleFileUri(context, uri)
            onFileUriConsumed()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VDub",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Speaker Diarization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Hero section
                Text(
                    "Upload audio or video",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Detect speakers, segment audio, and generate timelines - all on device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Upload buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio")
                    }
                    OutlinedButton(
                        onClick = onPickFile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Video")
                    }
                }
            }

            // Models status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (modelsReady)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                    onClick = onNavigateToModels
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (modelsReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (modelsReady)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (modelsReady) "Models Ready" else "Models Required",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (modelsReady) "Tap to manage models"
                                else "Download required models to start",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Selected file card
            if (selectedFile != null) {
                item {
                    AnimatedVisibility(visible = true) {
                        SelectedFileCard(
                            file = selectedFile!!,
                            onStart = {
                                onNavigateToProcessing(selectedFile!!.path, selectedFile!!.mediaType)
                                viewModel.clearSelectedFile()
                            },
                            onDismiss = { viewModel.clearSelectedFile() }
                        )
                    }
                }
            }

            // Quick actions
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Download,
                        title = "Models",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToModels
                    )
                    QuickActionCard(
                        icon = Icons.Default.History,
                        title = "History",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                    QuickActionCard(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSettings
                    )
                }
            }

            // Supported formats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Supported Formats", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Audio: WAV, MP3, FLAC, AAC, M4A, OGG, OPUS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Video: MP4, MKV, AVI, MOV, WebM, MPEG, 3GP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SelectedFileCard(
    file: MediaFile,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    file.size.formatFileSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.durationMs > 0) {
                    Text(
                        file.durationMs.formatDuration(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    file.mediaType.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Analysis")
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}
