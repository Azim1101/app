package com.vdub.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vdub.domain.entity.MediaType
import com.vdub.feature.history.HistoryScreen
import com.vdub.feature.home.HomeScreen
import com.vdub.feature.models.ModelsScreen
import com.vdub.feature.processing.ProcessingScreen
import com.vdub.feature.settings.SettingsScreen
import kotlin.reflect.typeOf

@Composable
fun VDubNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    onPickFile: () -> Unit,
    pendingFileUri: android.net.Uri?,
    onFileUriConsumed: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onNavigateToModels = { navController.navigate(Screen.Models) },
                onNavigateToProcessing = { filePath, mediaType ->
                    navController.navigate(Screen.Processing(filePath, mediaType.name))
                },
                onNavigateToHistory = { navController.navigate(Screen.History) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
                onPickFile = onPickFile,
                pendingFileUri = pendingFileUri,
                onFileUriConsumed = onFileUriConsumed
            )
        }

        composable<Screen.Models> {
            ModelsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Processing> {
            ProcessingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = { analysisId ->
                    navController.navigate(Screen.Results(analysisId))
                }
            )
        }

        composable<Screen.Results> {
            // Results are shown within ProcessingScreen for now
        }

        composable<Screen.History> {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenResult = { analysisId ->
                    navController.navigate(Screen.Results(analysisId))
                }
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
