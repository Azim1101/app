package com.vdub.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vdub.domain.entity.AppSettings
import com.vdub.domain.entity.ComputeBackend
import com.vdub.domain.entity.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance
            SettingsSectionHeader("Appearance")

            ThemeModeSetting(
                currentMode = settings.themeMode,
                onModeChange = { viewModel.updateThemeMode(it) }
            )

            SwitchSetting(
                title = "Dynamic Color",
                subtitle = "Use Material You dynamic colors",
                checked = settings.dynamicColorEnabled,
                onCheckedChange = { viewModel.updateDynamicColor(it) }
            )

            Divider()

            // Compute
            SettingsSectionHeader("Compute")

            ComputeBackendSetting(
                current = settings.computeBackend,
                onChange = { viewModel.updateComputeBackend(it) }
            )

            SliderSetting(
                title = "Thread Count",
                value = settings.threadCount,
                range = 1f..8f,
                steps = 6,
                onValueChange = { viewModel.updateThreadCount(it.toInt()) },
                valueLabel = settings.threadCount.toString()
            )

            SwitchSetting(
                title = "Low RAM Mode",
                subtitle = "Reduce memory usage for devices with less RAM",
                checked = settings.lowRamMode,
                onCheckedChange = { viewModel.updateLowRamMode(it) }
            )

            SwitchSetting(
                title = "NNAPI",
                subtitle = "Use Android Neural Networks API if available",
                checked = settings.nnapiEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(nnapiEnabled = it))
                }
            )

            SwitchSetting(
                title = "GPU Delegate",
                subtitle = "Use GPU acceleration if available",
                checked = settings.gpuDelegateEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(gpuDelegateEnabled = it))
                }
            )

            Divider()

            // Audio
            SettingsSectionHeader("Audio Processing")

            SliderSetting(
                title = "Sample Rate",
                value = settings.sampleRate.toFloat(),
                range = 8000f..48000f,
                steps = 3,
                onValueChange = { viewModel.updateSampleRate(it.toInt()) },
                valueLabel = "${settings.sampleRate} Hz"
            )

            SliderSetting(
                title = "Chunk Size",
                value = settings.chunkSizeMs.toFloat(),
                range = 1000f..30000f,
                steps = 5,
                onValueChange = {
                    viewModel.updateSettings(settings.copy(chunkSizeMs = it.toInt()))
                },
                valueLabel = "${settings.chunkSizeMs / 1000}s"
            )

            Divider()

            // Clustering
            SettingsSectionHeader("Speaker Clustering")

            SliderSetting(
                title = "Clustering Threshold",
                value = settings.clusteringThreshold,
                range = 0.3f..0.95f,
                steps = 12,
                onValueChange = { viewModel.updateClusteringThreshold(it) },
                valueLabel = String.format("%.2f", settings.clusteringThreshold)
            )

            SliderSetting(
                title = "Max Speakers",
                value = settings.maxSpeakers.toFloat(),
                range = 2f..20f,
                steps = 17,
                onValueChange = { viewModel.updateMaxSpeakers(it.toInt()) },
                valueLabel = settings.maxSpeakers.toString()
            )

            SwitchSetting(
                title = "Auto Speaker Count",
                subtitle = "Automatically estimate number of speakers",
                checked = settings.autoSpeakerCount,
                onCheckedChange = { viewModel.updateAutoSpeakerCount(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}

@Composable
private fun ThemeModeSetting(
    currentMode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

@Composable
private fun ComputeBackendSetting(
    current: ComputeBackend,
    onChange: (ComputeBackend) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Compute Backend", style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComputeBackend.entries.forEach { backend ->
                FilterChip(
                    selected = current == backend,
                    onClick = { onChange(backend) },
                    label = { Text(backend.name) }
                )
            }
        }
    }
}
