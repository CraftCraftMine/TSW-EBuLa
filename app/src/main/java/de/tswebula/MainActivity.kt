package de.tswebula

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import de.tswebula.data.database.AppDatabase
import de.tswebula.data.model.Route
import de.tswebula.data.repository.RouteRepository
import de.tswebula.ui.screen.*
import de.tswebula.ui.theme.TSWEBulaTheme
import de.tswebula.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(this)
        val repo = RouteRepository(db)

        setContent {
            TSWEBulaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {

                        // ── Home ─────────────────────────────────────
                        composable("home") {
                            val vm = remember {
                                HomeViewModel(repo)
                            }
                            HomeScreen(
                                viewModel = vm,
                                onNavigateToEditor = { routeId ->
                                    navController.navigate("editor/$routeId")
                                },
                                onNavigateToTimetable = { routeId ->
                                    navController.navigate("timetable/$routeId")
                                },
                                onNavigateToDrive = { routeId, tripId ->
                                    navController.navigate("drive/$routeId/$tripId")
                                }
                            )
                        }

                        // ── Strecken-Editor ──────────────────────────
                        composable(
                            "editor/{routeId}",
                            arguments = listOf(navArgument("routeId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val routeId = backStackEntry.arguments?.getLong("routeId") ?: 0L
                            val vm = remember(routeId) {
                                RouteEditorViewModel(repo, routeId)
                            }
                            RouteEditorScreen(
                                viewModel = vm,
                                routeId = routeId,
                                onNavigateBack = { navController.popBackStack() },
                                onExport = { json ->
                                    // JSON in Teilen-Dialog öffnen
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    startActivity(Intent.createChooser(intent, "Strecke exportieren"))
                                }
                            )
                        }

                        // ── Fahrplan-Editor ──────────────────────────
                        composable(
                            "timetable/{routeId}",
                            arguments = listOf(navArgument("routeId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val routeId = backStackEntry.arguments?.getLong("routeId") ?: 0L
                            val vm = remember(routeId) {
                                TimetableViewModel(repo, routeId)
                            }
                            TimetableScreen(
                                viewModel = vm,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // ── EBuLa Fahrt-Ansicht ──────────────────────
                        composable(
                            "drive/{routeId}/{tripId}",
                            arguments = listOf(
                                navArgument("routeId") { type = NavType.LongType },
                                navArgument("tripId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val routeId = backStackEntry.arguments?.getLong("routeId") ?: 0L
                            val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                            val vm = remember(routeId, tripId) {
                                DriveViewModel(repo, routeId, tripId)
                            }
                            DriveScreen(
                                viewModel = vm,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
