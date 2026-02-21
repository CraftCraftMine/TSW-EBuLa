package de.tswebula.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────
//  Streckenobjekt-Typen
// ─────────────────────────────────────────────
@Serializable
enum class TrackObjectType(
    val label: String,
    val shortLabel: String,
    val colorHex: String
) {
    STATION("Bahnhof / Haltepunkt", "Bf", "#2196F3"),
    SPEED_CHANGE("Geschwindigkeitswechsel", "V", "#FF9800"),
    BAHNUEBERGANG("Bahnübergang", "BÜ", "#F44336"),
    SIGNAL("Signal", "Sig", "#9C27B0"),
    TUNNEL_START("Tunnelanfang", "Tu▶", "#607D8B"),
    TUNNEL_END("Tunnelende", "◀Tu", "#607D8B"),
    GRADIENT("Steigung / Gefälle", "‰", "#4CAF50"),
    SLOW_SPEED("Langsamfahrstelle", "La", "#FF5722")
}

@Serializable
enum class CalculationMode {
    TIME_BASED,   // Ingame-Zeit eingeben → km berechnen
    SPEED_BASED   // Abfahrtszeit + Ø-Geschwindigkeit
}

// ─────────────────────────────────────────────
//  Strecke (Route)
// ─────────────────────────────────────────────
@Entity(tableName = "routes")
@Serializable
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val startKm: Double = 0.0,
    val endKm: Double = 100.0,
    val isFromCloud: Boolean = false,
    val cloudUrl: String = "",           // GitHub Raw URL
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  Streckenobjekt (z.B. Bahnhof bei km 12.3)
// ─────────────────────────────────────────────
@Entity(tableName = "track_objects")
@Serializable
data class TrackObject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val km: Double,                        // Position auf der Strecke
    val type: TrackObjectType,
    val name: String = "",                 // z.B. Bahnhofsname, Signalbezeichnung
    val speedLimit: Int? = null,           // km/h – für V-Wechsel und La
    val speedLimitEnd: Double? = null,     // km Ende der La
    val gradient: Double? = null,          // ‰ positiv = Steigung, negativ = Gefälle
    val notes: String = "",
    val sortOrder: Int = 0                 // Reihenfolge bei gleichem km
)

// ─────────────────────────────────────────────
//  Fahrplaneintrag (Halt mit Zeiten)
// ─────────────────────────────────────────────
@Entity(tableName = "timetable_entries")
@Serializable
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val km: Double,
    val stationName: String,
    val arrivalTime: String = "",          // "HH:mm" – leer bei erster Station
    val departureTime: String = "",        // "HH:mm" – leer bei letzter Station
    val trackNumber: String = "",
    val dwellTimeSeconds: Int = 0,         // Haltezeit in Sekunden
    val isStop: Boolean = true             // false = nur Durchfahrt / Vorbeifahrt
)

// ─────────────────────────────────────────────
//  Fahrt (aktive Session mit Positionsberechnung)
// ─────────────────────────────────────────────
@Entity(tableName = "trips")
@Serializable
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val name: String,
    val calculationMode: CalculationMode = CalculationMode.TIME_BASED,
    val avgSpeedKmh: Double = 80.0,        // Nur für SPEED_BASED
    val departureTimeStr: String = "",     // "HH:mm"
    val startKmOffset: Double = 0.0        // Korrektur falls km nicht stimmt
)

// ─────────────────────────────────────────────
//  Export-Container (für GitHub JSON)
// ─────────────────────────────────────────────
@Serializable
data class RouteExportBundle(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val route: Route,
    val trackObjects: List<TrackObject>,
    val timetableEntries: List<TimetableEntry>
)
