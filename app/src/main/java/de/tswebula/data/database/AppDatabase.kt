package de.tswebula.data.database

import androidx.room.*
import de.tswebula.data.model.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
//  Type Converters für Room
// ─────────────────────────────────────────────
class Converters {
    @TypeConverter fun fromTrackObjectType(v: TrackObjectType) = v.name
    @TypeConverter fun toTrackObjectType(v: String) = TrackObjectType.valueOf(v)
    @TypeConverter fun fromCalculationMode(v: CalculationMode) = v.name
    @TypeConverter fun toCalculationMode(v: String) = CalculationMode.valueOf(v)
}

// ─────────────────────────────────────────────
//  Route DAO
// ─────────────────────────────────────────────
@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY name ASC")
    fun getAllRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: Long): Route?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route): Long

    @Update
    suspend fun updateRoute(route: Route)

    @Delete
    suspend fun deleteRoute(route: Route)
}

// ─────────────────────────────────────────────
//  TrackObject DAO
// ─────────────────────────────────────────────
@Dao
interface TrackObjectDao {
    @Query("SELECT * FROM track_objects WHERE routeId = :routeId ORDER BY km ASC, sortOrder ASC")
    fun getObjectsForRoute(routeId: Long): Flow<List<TrackObject>>

    @Query("SELECT * FROM track_objects WHERE routeId = :routeId ORDER BY km ASC, sortOrder ASC")
    suspend fun getObjectsForRouteOnce(routeId: Long): List<TrackObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(obj: TrackObject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(objects: List<TrackObject>)

    @Update
    suspend fun updateObject(obj: TrackObject)

    @Delete
    suspend fun deleteObject(obj: TrackObject)

    @Query("DELETE FROM track_objects WHERE routeId = :routeId")
    suspend fun deleteAllForRoute(routeId: Long)
}

// ─────────────────────────────────────────────
//  TimetableEntry DAO
// ─────────────────────────────────────────────
@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries WHERE routeId = :routeId ORDER BY km ASC")
    fun getEntriesForRoute(routeId: Long): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries WHERE routeId = :routeId ORDER BY km ASC")
    suspend fun getEntriesForRouteOnce(routeId: Long): List<TimetableEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimetableEntry>)

    @Update
    suspend fun updateEntry(entry: TimetableEntry)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries WHERE routeId = :routeId")
    suspend fun deleteAllForRoute(routeId: Long)
}

// ─────────────────────────────────────────────
//  Trip DAO
// ─────────────────────────────────────────────
@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE routeId = :routeId ORDER BY name ASC")
    fun getTripsForRoute(routeId: Long): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)
}

// ─────────────────────────────────────────────
//  Room Datenbank
// ─────────────────────────────────────────────
@Database(
    entities = [Route::class, TrackObject::class, TimetableEntry::class, Trip::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun trackObjectDao(): TrackObjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tsw_ebula.db"
                ).build().also { INSTANCE = it }
            }
    }
}
