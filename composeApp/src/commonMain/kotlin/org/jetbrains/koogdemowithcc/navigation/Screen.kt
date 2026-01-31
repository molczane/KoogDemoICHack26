package org.jetbrains.koogdemowithcc.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation routes for the app.
 * Using kotlinx.serialization for type-safe navigation with Navigation Compose.
 */
@Serializable
sealed class Screen {
    @Serializable
    data object Weather : Screen()

    @Serializable
    data object TripPlan : Screen()
}
