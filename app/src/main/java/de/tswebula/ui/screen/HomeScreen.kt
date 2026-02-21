package de.tswebula.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tswebula.data.model.Route
import de.tswebula.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToTimetable: (Long) -> Unit,
    onNavigateToDrive: (Long, Long) -> Unit
) {
    val routes by viewModel.routes.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TSW EBuLa") },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Von GitHub importieren")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Neue Strecke")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Train, contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Noch keine Strecken.\nErstelle eine neue oder importiere aus GitHub.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteCard(
                        route = route,
                        onEdit = { onNavigateToEditor(route.id) },
                        onTimetable = { onNavigateToTimetable(route.id) },
                        onDrive = { tripId -> onNavigateToDrive(route.id, tripId) },
                        onDelete = { viewModel.deleteRoute(route) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showImportDialog) {
        GitHubImportDialog(
            onImport = { url ->
                viewModel.importFromGitHub(url) { success, msg ->
                    snackbarMessage = msg
                }
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }

    if (showCreateDialog) {
        CreateRouteDialog(
            onCreate = { name, desc, startKm, endKm ->
                showCreateDialog = false
                viewModel.createRoute(name, desc, startKm, endKm) { newId: Long ->
                    onNavigateToEditor(newId)
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun RouteCard(
    route: Route,
    onEdit: () -> Unit,
    onTimetable: () -> Unit,
    onDrive: (Long) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cloud-Badge
                if (route.isFromCloud) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = "Aus Cloud",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = "Lokal",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    route.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            if (route.description.isNotEmpty()) {
                Text(
                    route.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "km %.1f – %.1f".format(route.startKm, route.endKm),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Strecke bearbeiten
                    FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Strecke", style = MaterialTheme.typography.labelSmall)
                    }
                    // Fahrplan bearbeiten
                    FilledTonalButton(onClick = onTimetable, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Fahrplan", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Fahrt starten
                    Button(onClick = { onDrive(0L) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Fahrt starten")
                    }
                    // Löschen
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubImportDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Von GitHub importieren") },
        text = {
            Column {
                Text(
                    "Füge die GitHub Raw-URL der JSON-Datei ein.\nZ.B.: https://raw.githubusercontent.com/…/strecke.json",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("GitHub Raw-URL") },
                    placeholder = { Text("https://raw.githubusercontent.com/…") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (url.isNotBlank()) onImport(url) }) { Text("Importieren") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun CreateRouteDialog(
    onCreate: (String, String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var startKm by remember { mutableStateOf("0.0") }
    var endKm by remember { mutableStateOf("100.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Strecke") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Beschreibung (optional)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startKm, onValueChange = { startKm = it },
                        label = { Text("Start-km") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endKm, onValueChange = { endKm = it },
                        label = { Text("End-km") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onCreate(
                        name.trim(), desc.trim(),
                        startKm.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        endKm.replace(",", ".").toDoubleOrNull() ?: 100.0
                    )
                }
            }) { Text("Erstellen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
