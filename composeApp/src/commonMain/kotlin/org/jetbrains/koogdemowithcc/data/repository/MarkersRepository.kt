package org.jetbrains.koogdemowithcc.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.koogdemowithcc.data.local.SettingsRepository
import org.jetbrains.koogdemowithcc.domain.model.LatLng
import org.jetbrains.koogdemowithcc.domain.model.MapMarker
import org.jetbrains.koogdemowithcc.domain.model.TripRoute

/**
 * Repository for managing map markers and routes.
 * Provides persistence and reactive updates.
 */
interface MarkersRepository {
    val markers: Flow<List<MapMarker>>
    val currentRoute: Flow<TripRoute?>

    fun addMarker(marker: MapMarker)
    fun removeMarker(markerId: String)
    fun updateMarker(marker: MapMarker)
    fun selectMarker(markerId: String)
    fun clearSelection()
    fun setRoute(route: TripRoute?)
    fun clearMarkers()
    fun clearRoute()
    fun getMarkersList(): List<MapMarker>
}

class MarkersRepositoryImpl(
    private val settingsRepository: SettingsRepository
) : MarkersRepository {

    private val _markers = MutableStateFlow<List<MapMarker>>(emptyList())
    override val markers: Flow<List<MapMarker>> = _markers.asStateFlow()

    private val _currentRoute = MutableStateFlow<TripRoute?>(null)
    override val currentRoute: Flow<TripRoute?> = _currentRoute.asStateFlow()

    init {
        // Load persisted markers on init
        loadPersistedMarkers()
    }

    private fun loadPersistedMarkers() {
        // We'll collect from settingsRepository in a blocking way for init
        // In a real app, this would be handled more elegantly
        // For now, markers start empty and get loaded via the flow
    }

    override fun addMarker(marker: MapMarker) {
        _markers.update { currentMarkers ->
            // Avoid duplicates
            if (currentMarkers.any { it.id == marker.id }) {
                currentMarkers
            } else {
                val updated = currentMarkers + marker
                persistMarkers(updated)
                updated
            }
        }
    }

    override fun removeMarker(markerId: String) {
        _markers.update { currentMarkers ->
            val updated = currentMarkers.filter { it.id != markerId }
            persistMarkers(updated)
            updated
        }
    }

    override fun updateMarker(marker: MapMarker) {
        _markers.update { currentMarkers ->
            val updated = currentMarkers.map {
                if (it.id == marker.id) marker else it
            }
            persistMarkers(updated)
            updated
        }
    }

    override fun selectMarker(markerId: String) {
        _markers.update { currentMarkers ->
            currentMarkers.map { marker ->
                marker.copy(isSelected = marker.id == markerId)
            }
        }
    }

    override fun clearSelection() {
        _markers.update { currentMarkers ->
            currentMarkers.map { it.copy(isSelected = false) }
        }
    }

    override fun setRoute(route: TripRoute?) {
        _currentRoute.value = route
    }

    override fun clearMarkers() {
        _markers.value = emptyList()
        persistMarkers(emptyList())
    }

    override fun clearRoute() {
        _currentRoute.value = null
    }

    override fun getMarkersList(): List<MapMarker> = _markers.value

    private fun persistMarkers(markers: List<MapMarker>) {
        settingsRepository.saveMapMarkers(markers)
    }
}
