package de.tswebula.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tswebula.data.model.*
import de.tswebula.ui.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    onNavigateBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val trips by viewModel.trips.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var showTripDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fahrplan & Fahrten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedTab == 0) showEntryDialog = true
                else showTripDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tab-Bar
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Halte (${entries.size})") },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Fahrten (${trips.size})") },
                    icon = { Icon(Icons.Default.Train, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            when (selectedTab) {
                0 -> TimetableEntriesList(
                    entries = entries,
                    onEdit = { editingEntry = it },
                    onDelete = { viewModel.deleteTimetableEntry(it) }
                )
                1 -> TripsList(
                    trips = trips,
                    onDelete = { viewModel.deleteTrip(it) }
                )
            }
        }
    }

    if (showEntryDialog) {
        TimetableEntryDialog(
            existing = null,
            routeId = viewModel.routeId,
            onConfirm = { entry ->
                viewModel.saveTimetableEntry(entry)
                showEntryDialog = false
            },
            onDismiss = { showEntryDialog = false }
        )
    }

    editingEntry?.let { entry ->
        TimetableEntryDialog(
            existing = entry,
            routeId = viewModel.routeId,
            onConfirm = { updated ->
                viewModel.updateTimetableEntry(updated)
                editingEntry = null
            },
            onDismiss = { editingEntry = null }
        )
    }

    if (showTripDialog) {
        TripDialog(
            routeId = viewModel.routeId,
            onConfirm = { trip ->
                viewModel.saveTrip(trip)
                showTripDialog = false
            },
            onDismiss = { showTripDialog = false }
        )
    }
}

@Composable
private fun TimetableEntriesList(
    entries: List<TimetableEntry>,
    onEdit: (TimetableEntry) -> Unit,
    onDelete: (TimetableEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Noch keine Halte. Tippe auf + um einen hinzuzufügen.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (entry.isStop) Icons.Default.Stop else Icons.Default.DoubleArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(entry.stationName, style = MaterialTheme.typography.titleSmall)
                                if (entry.trackNumber.isNotEmpty()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("Gl. ${entry.trackNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Text(
                                "km %.3f  |  ".format(entry.km) +
                                buildString {
                                    if (entry.arrivalTime.isNotEmpty()) append("an: ${entry.arrivalTime}  ")
                                    if (entry.departureTime.isNotEmpty()) append("ab: ${entry.departureTime}")
                                }.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEdit(entry) }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDelete(entry) }) {
                            Icon(Icons.Default.Delete, contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TripsList(
    trips: List<Trip>,
    onDelete: (Trip) -> Unit
) {
    if (trips.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Noch keine Fahrten angelegt.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trips, key = { it.id }) { trip ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(trip.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                when (trip.calculationMode) {
                                    CalculationMode.TIME_BASED -> "Zeitbasiert · Abfahrt: ${trip.departureTimeStr}"
                                    CalculationMode.SPEED_BASED -> "Ø ${trip.avgSpeedKmh} km/h · Abfahrt: ${trip.departureTimeStr}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(trip) }) {
                            Icon(Icons.Default.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TimetableEntryDialog(
    existing: TimetableEntry?,
    routeId: Long,
    onConfirm: (TimetableEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var km by remember { mutableStateOf(existing?.km?.toString() ?: "") }
    var stationName by remember { mutableStateOf(existing?.stationName ?: "") }
    var arrival by remember { mutableStateOf(existing?.arrivalTime ?: "") }
    var departure by remember { mutableStateOf(existing?.departureTime ?: "") }
    var trackNum by remember { mutableStateOf(existing?.trackNumber ?: "") }
    var isStop by remember { mutableStateOf(existing?.isStop ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Halt hinzufügen" else "Halt bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = km, onValueChange = { km = it },
                    label = { Text("Position (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stationName, onValueChange = { stationName = it },
                    label = { Text("Bahnhofsname") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = arrival, onValueChange = { arrival = it },
                        label = { Text("Ankunft") }, placeholder = { Text("HH:mm") },
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(value = departure, onValueChange = { departure = it },
                        label = { Text("Abfahrt") }, placeholder = { Text("HH:mm") },
                        modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = trackNum, onValueChange = { trackNum = it },
                    label = { Text("Gleis (optional)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isStop, onCheckedChange = { isStop = it })
                    Text("Planmäßiger Halt (sonst Durchfahrt)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val kmVal = km.replace(",", ".").toDoubleOrNull() ?: return@TextButton
                if (stationName.isBlank()) return@TextButton
                onConfirm(TimetableEntry(
                    id = existing?.id ?: 0,
                    routeId = routeId,
                    km = kmVal,
                    stationName = stationName.trim(),
                    arrivalTime = arrival.trim(),
                    departureTime = departure.trim(),
                    trackNumber = trackNum.trim(),
                    isStop = isStop
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDialog(
    routeId: Long,
    onConfirm: (Trip) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(CalculationMode.TIME_BASED) }
    var departure by remember { mutableStateOf("") }
    var avgSpeed by remember { mutableStateOf("80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fahrt anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Zugname / Zugnummer") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = departure, onValueChange = { departure = it },
                    label = { Text("Abfahrtszeit") }, placeholder = { Text("HH:mm") },
                    modifier = Modifier.fillMaxWidth())

                Text("Berechnungsmethode:", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == CalculationMode.TIME_BASED,
                        onClick = { mode = CalculationMode.TIME_BASED })
                    Text("Zeitbasiert (Fahrplan-Interpolation)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == CalculationMode.SPEED_BASED,
                        onClick = { mode = CalculationMode.SPEED_BASED })
                    Text("Ø-Geschwindigkeit")
                }
                if (mode == CalculationMode.SPEED_BASED) {
                    OutlinedTextField(value = avgSpeed, onValueChange = { avgSpeed = it },
                        label = { Text("Ø-Geschwindigkeit (km/h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || departure.isBlank()) return@TextButton
                onConfirm(Trip(
                    routeId = routeId,
                    name = name.trim(),
                    calculationMode = mode,
                    departureTimeStr = departure.trim(),
                    avgSpeedKmh = avgSpeed.toDoubleOrNull() ?: 80.0
                ))
            }) { Text("Erstellen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
