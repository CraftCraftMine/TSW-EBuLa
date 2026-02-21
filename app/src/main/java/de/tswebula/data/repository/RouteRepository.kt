package de.tswebula.data.repository

import de.tswebula.data.database.AppDatabase
import de.tswebula.data.model.*
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RouteRepository(private val db: AppDatabase) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val http = HttpClient(Android)

    // ── Strecken ──────────────────────────────
    fun getAllRoutes(): Flow<List<Route>> = db.routeDao().getAllRoutes()

    suspend fun getRouteById(id: Long) = db.routeDao().getRouteById(id)

    suspend fun saveRoute(route: Route): Long = db.routeDao().insertRoute(route)

    suspend fun updateRoute(route: Route) = db.routeDao().updateRoute(route)

    suspend fun deleteRoute(route: Route) {
        db.trackObjectDao().deleteAllForRoute(route.id)
        db.timetableDao().deleteAllForRoute(route.id)
        db.routeDao().deleteRoute(route)
    }

    // ── Streckenobjekte ───────────────────────
    fun getObjectsForRoute(routeId: Long): Flow<List<TrackObject>> =
        db.trackObjectDao().getObjectsForRoute(routeId)

    suspend fun saveTrackObject(obj: TrackObject): Long =
        db.trackObjectDao().insertObject(obj)

    suspend fun updateTrackObject(obj: TrackObject) =
        db.trackObjectDao().updateObject(obj)

    suspend fun deleteTrackObject(obj: TrackObject) =
        db.trackObjectDao().deleteObject(obj)

    // ── Fahrplan ──────────────────────────────
    fun getTimetableForRoute(routeId: Long): Flow<List<TimetableEntry>> =
        db.timetableDao().getEntriesForRoute(routeId)

    suspend fun saveTimetableEntry(entry: TimetableEntry): Long =
        db.timetableDao().insertEntry(entry)

    suspend fun updateTimetableEntry(entry: TimetableEntry) =
        db.timetableDao().updateEntry(entry)

    suspend fun deleteTimetableEntry(entry: TimetableEntry) =
        db.timetableDao().deleteEntry(entry)

    // ── Fahrten ───────────────────────────────
    fun getTripsForRoute(routeId: Long): Flow<List<Trip>> =
        db.tripDao().getTripsForRoute(routeId)

    suspend fun getTripById(id: Long) = db.tripDao().getTripById(id)

    suspend fun saveTrip(trip: Trip): Long = db.tripDao().insertTrip(trip)

    suspend fun updateTrip(trip: Trip) = db.tripDao().updateTrip(trip)

    suspend fun deleteTrip(trip: Trip) = db.tripDao().deleteTrip(trip)

    // ── Export (JSON für GitHub) ───────────────
    suspend fun exportRouteAsJson(routeId: Long): String {
        val route = db.routeDao().getRouteById(routeId) ?: error("Route nicht gefunden")
        val objects = db.trackObjectDao().getObjectsForRouteOnce(routeId)
        val entries = db.timetableDao().getEntriesForRouteOnce(routeId)

        val bundle = RouteExportBundle(
            route = route.copy(id = 0),   // IDs zurücksetzen für portabilität
            trackObjects = objects.map { it.copy(id = 0, routeId = 0) },
            timetableEntries = entries.map { it.copy(id = 0, routeId = 0) }
        )
        return json.encodeToString(bundle)
    }

    // ── Import aus lokalem JSON ────────────────
    suspend fun importFromJson(jsonString: String): Long {
        val bundle = json.decodeFromString<RouteExportBundle>(jsonString)
        val newRouteId = db.routeDao().insertRoute(bundle.route)

        db.trackObjectDao().insertAll(
            bundle.trackObjects.map { it.copy(routeId = newRouteId) }
        )
        db.timetableDao().insertAll(
            bundle.timetableEntries.map { it.copy(routeId = newRouteId) }
        )
        return newRouteId
    }

    // ── Import von GitHub Raw URL ──────────────
    suspend fun importFromGitHub(rawUrl: String): Result<Long> = runCatching {
        val response = http.get(rawUrl)
        val jsonString = response.bodyAsText()
        importFromJson(jsonString)
    }
}
