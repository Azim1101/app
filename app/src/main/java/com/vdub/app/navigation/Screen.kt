package com.vdub.app.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation routes for VDub screens.
 */
@Serializable
sealed class Screen {
    @Serializable data object Home : Screen()
    @Serializable data object Models : Screen()
    @Serializable data class Processing(val filePath: String, val mediaType: String) : Screen()
    @Serializable data class Results(val analysisId: Long = -1) : Screen()
    @Serializable data object History : Screen()
    @Serializable data object Settings : Screen()
}
