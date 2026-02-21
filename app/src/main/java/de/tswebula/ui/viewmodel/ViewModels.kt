package de.tswebula.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tswebula.data.engine.PositionEngine
import de.tswebula.data.model.*
import de.tswebula.data.repository.RouteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Home Screen ────────────────────────────────────────────────────────
class HomeViewModel(private val repo: RouteRepository) : ViewModel() {

    val routes: StateFlow<List<Route>> = repo.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRoute(route: Route) = viewModelScope.launch { repo.deleteRoute(route) }

    fun createRoute(name: String, desc: String, startKm: Double, endKm: Double, onCreated: (Long) -> Unit) =
        viewModelScope.launch {
            val id = repo.saveRoute(Route(name = name, description = desc, startKm = startKm, endKm = endKm))
            onCreated(id)
        }

    fun importFromGitHub(url: String, onResult: (Boolean, String) -> Unit) =
        viewModelScope.launch {
            repo.importFromGitHub(url).fold(
                onSuccess = { onResult(true, "Strecke erfolgreich importiert!") },
                onFailure = { onResult(false, "Fehler: ${it.message}") }
            )
        }
}

// ── Route Editor ───────────────────────────────────────────────────────
class RouteEditorViewModel(
    private val repo: RouteRepository,
    private val routeId: Long
) : ViewModel() {

    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    val trackObjects: StateFlow<List<TrackObject>> = if (routeId > 0)
        repo.getObjectsForRoute(routeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    else MutableStateFlow(emptyList())

    init {
        if (routeId > 0) {
            viewModelScope.launch {
                _route.value = repo.getRouteById(routeId)
            }
        }
    }

    fun saveRoute(route: Route, onSaved: (Long) -> Unit) = viewModelScope.launch {
        val id = repo.saveRoute(route)
        onSaved(id)
    }

    fun saveTrackObject(obj: TrackObject) = viewModelScope.launch {
        repo.saveTrackObject(obj)
    }

    fun updateTrackObject(obj: TrackObject) = viewModelScope.launch {
        repo.updateTrackObject(obj)
    }

    fun deleteTrackObject(obj: TrackObject) = viewModelScope.launch {
        repo.deleteTrackObject(obj)
    }

    fun exportRoute(onJson: (String) -> Unit) = viewModelScope.launch {
        val json = repo.exportRouteAsJson(routeId)
        onJson(json)
    }
}

// ── Timetable Editor ───────────────────────────────────────────────────
class TimetableViewModel(
    private val repo: RouteRepository,
    val routeId: Long
) : ViewModel() {

    val entries: StateFlow<List<TimetableEntry>> = repo.getTimetableForRoute(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trips: StateFlow<List<Trip>> = repo.getTripsForRoute(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTimetableEntry(entry: TimetableEntry) = viewModelScope.launch {
        repo.saveTimetableEntry(entry)
    }
    fun updateTimetableEntry(entry: TimetableEntry) = viewModelScope.launch {
        repo.updateTimetableEntry(entry)
    }
    fun deleteTimetableEntry(entry: TimetableEntry) = viewModelScope.launch {
        repo.deleteTimetableEntry(entry)
    }
    fun saveTrip(trip: Trip) = viewModelScope.launch { repo.saveTrip(trip) }
    fun deleteTrip(trip: Trip) = viewModelScope.launch { repo.deleteTrip(trip) }
}

// ── EBuLa Drive View ───────────────────────────────────────────────────
data class DriveViewState(
    val route: Route? = null,
    val trackObjects: List<TrackObject> = emptyList(),
    val timetable: List<TimetableEntry> = emptyList(),
    val trip: Trip? = null,
    val currentKm: Double = 0.0,
    val currentTimeStr: String = "",
    val progress: Float = 0f,
    val nextStation: TimetableEntry? = null,
    val prevStation: TimetableEntry? = null,
    val autoScrollEnabled: Boolean = true,
    val isRunning: Boolean = false,
    val kmOffset: Double = 0.0          // manuelle Korrektur
)

class DriveViewModel(
    private val repo: RouteRepository,
    private val routeId: Long,
    private val tripId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(DriveViewState())
    val state: StateFlow<DriveViewState> = _state.asStateFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            val route = repo.getRouteById(routeId)
            val trip = repo.getTripById(tripId)
            _state.update { it.copy(route = route, trip = trip) }
        }
        viewModelScope.launch {
            repo.getObjectsForRoute(routeId).collect { objects ->
                _state.update { it.copy(trackObjects = objects) }
            }
        }
        viewModelScope.launch {
            repo.getTimetableForRoute(routeId).collect { entries ->
                _state.update { it.copy(timetable = entries) }
            }
        }
    }

    /** Ingame-Zeit manuell setzen und Position berechnen */
    fun setCurrentTime(timeStr: String) {
        val s = _state.value
        val trip = s.trip ?: return
        val km = PositionEngine.calculateCurrentKm(timeStr, trip, s.timetable) + s.kmOffset
        val route = s.route
        _state.update {
            it.copy(
                currentTimeStr = timeStr,
                currentKm = km,
                progress = if (route != null)
                    PositionEngine.calculateProgress(km, route.startKm, route.endKm) else 0f,
                nextStation = PositionEngine.nextStation(km, s.timetable),
                prevStation = PositionEngine.previousStation(km, s.timetable)
            )
        }
    }

    /** Auto-Ticker: jede Sekunde die Realzeit vorwärts zählen (1:1 mit Echtzeit) */
    fun startAutoTick() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            _state.update { it.copy(isRunning = true) }
            while (true) {
                delay(1000L)
                val current = _state.value.currentTimeStr
                if (current.isNotEmpty()) {
                    val advanced = advanceTimeBySeconds(current, 1)
                    setCurrentTime(advanced)
                }
            }
        }
    }

    fun stopAutoTick() {
        tickJob?.cancel()
        _state.update { it.copy(isRunning = false) }
    }

    fun toggleAutoScroll() {
        _state.update { it.copy(autoScrollEnabled = !it.autoScrollEnabled) }
    }

    /** Km-Korrektur: wenn ingame-km nicht stimmt */
    fun adjustKmOffset(delta: Double) {
        _state.update { it.copy(kmOffset = it.kmOffset + delta) }
        setCurrentTime(_state.value.currentTimeStr) // neu berechnen
    }

    fun resetKmOffset() {
        _state.update { it.copy(kmOffset = 0.0) }
        setCurrentTime(_state.value.currentTimeStr)
    }

    private fun advanceTimeBySeconds(timeStr: String, seconds: Int): String {
        return runCatching {
            val parts = timeStr.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val totalSecs = h * 3600 + m * 60 + seconds
            val newH = (totalSecs / 3600) % 24
            val newM = (totalSecs % 3600) / 60
            "%02d:%02d".format(newH, newM)
        }.getOrDefault(timeStr)
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
