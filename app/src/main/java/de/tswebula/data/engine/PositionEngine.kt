package de.tswebula.data.engine

import de.tswebula.data.model.CalculationMode
import de.tswebula.data.model.TimetableEntry
import de.tswebula.data.model.Trip
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Berechnet die aktuelle Position (km) basierend auf Ingame-Zeit und Fahrplan.
 */
object PositionEngine {

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Gibt den aktuell berechneten Streckenkilometer zurück.
     *
     * @param currentTimeStr  Aktuelle Ingame-Zeit "HH:mm"
     * @param trip            Die aktive Fahrt
     * @param timetable       Fahrplaneinträge der Strecke (nach km sortiert)
     * @return                Berechneter Streckenkilometer
     */
    fun calculateCurrentKm(
        currentTimeStr: String,
        trip: Trip,
        timetable: List<TimetableEntry>
    ): Double {
        return when (trip.calculationMode) {
            CalculationMode.TIME_BASED -> calculateTimeBased(currentTimeStr, timetable, trip)
            CalculationMode.SPEED_BASED -> calculateSpeedBased(currentTimeStr, trip)
        }
    }

    // ── Zeitbasiert: Interpolation zwischen Fahrplanpunkten ──
    private fun calculateTimeBased(
        currentTimeStr: String,
        timetable: List<TimetableEntry>,
        trip: Trip
    ): Double {
        if (timetable.isEmpty()) return 0.0
        val currentTime = parseTime(currentTimeStr) ?: return 0.0

        // Alle Einträge mit validen Zeiten als "Ankerpunkte"
        val anchors = buildAnchors(timetable)
        if (anchors.isEmpty()) return 0.0

        // Vor erster Station
        val first = anchors.first()
        if (currentTime <= first.time) return first.km + trip.startKmOffset

        // Nach letzter Station
        val last = anchors.last()
        if (currentTime >= last.time) return last.km + trip.startKmOffset

        // Zwischen zwei Stationen interpolieren
        for (i in 0 until anchors.size - 1) {
            val a = anchors[i]
            val b = anchors[i + 1]
            if (currentTime >= a.time && currentTime <= b.time) {
                val totalSeconds = timeDiffSeconds(a.time, b.time).coerceAtLeast(1)
                val elapsedSeconds = timeDiffSeconds(a.time, currentTime)
                val progress = elapsedSeconds.toDouble() / totalSeconds
                val interpolatedKm = a.km + (b.km - a.km) * progress
                return interpolatedKm + trip.startKmOffset
            }
        }
        return anchors.last().km + trip.startKmOffset
    }

    // ── Geschwindigkeitsbasiert: Einfache Extrapolation ──
    private fun calculateSpeedBased(currentTimeStr: String, trip: Trip): Double {
        val departure = parseTime(trip.departureTimeStr) ?: return 0.0
        val current = parseTime(currentTimeStr) ?: return 0.0
        val elapsedHours = timeDiffSeconds(departure, current) / 3600.0
        return (elapsedHours * trip.avgSpeedKmh) + trip.startKmOffset
    }

    // ── Hilfsfunktionen ──────────────────────────────────
    private data class Anchor(val km: Double, val time: LocalTime)

    private fun buildAnchors(timetable: List<TimetableEntry>): List<Anchor> {
        val anchors = mutableListOf<Anchor>()
        timetable.forEach { entry ->
            // Abfahrtszeit bevorzugen, sonst Ankunft
            val timeStr = entry.departureTime.ifEmpty { entry.arrivalTime }
            val time = parseTime(timeStr) ?: return@forEach
            anchors.add(Anchor(km = entry.km, time = time))
        }
        return anchors.sortedBy { it.km }
    }

    private fun parseTime(str: String): LocalTime? = runCatching {
        if (str.isBlank()) null else LocalTime.parse(str, timeFormat)
    }.getOrNull()

    private fun timeDiffSeconds(from: LocalTime, to: LocalTime): Long {
        var diff = to.toSecondOfDay().toLong() - from.toSecondOfDay().toLong()
        // Mitternacht überqueren
        if (diff < 0) diff += 86400
        return diff
    }

    /**
     * Gibt den prozentualen Fortschritt auf der Strecke zurück (0.0 – 1.0)
     */
    fun calculateProgress(currentKm: Double, startKm: Double, endKm: Double): Float {
        if (endKm <= startKm) return 0f
        return ((currentKm - startKm) / (endKm - startKm)).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Nächste geplante Station mit Ankunftszeit
     */
    fun nextStation(currentKm: Double, timetable: List<TimetableEntry>): TimetableEntry? {
        return timetable.firstOrNull { it.km > currentKm && it.isStop }
    }

    /**
     * Vorherige Station
     */
    fun previousStation(currentKm: Double, timetable: List<TimetableEntry>): TimetableEntry? {
        return timetable.lastOrNull { it.km <= currentKm && it.isStop }
    }
}
