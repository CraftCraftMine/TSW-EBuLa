package de.tswebula.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tswebula.data.model.TrackObject
import de.tswebula.data.model.TrackObjectType
import de.tswebula.ui.viewmodel.RouteEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditorScreen(
    viewModel: RouteEditorViewModel,
    routeId: Long,
    onNavigateBack: () -> Unit,
    onExport: (String) -> Unit
) {
    val trackObjects by viewModel.trackObjects.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingObject by remember { mutableStateOf<TrackObject?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<TrackObject?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strecken-Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.exportRoute { json -> onExport(json) }
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = "Exportieren")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Objekt hinzufügen")
            }
        }
    ) { padding ->
        if (trackObjects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Route, contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Noch keine Streckenobjekte.\nTippe auf + um eines hinzuzufügen.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trackObjects, key = { it.id }) { obj ->
                    TrackObjectCard(
                        obj = obj,
                        onEdit = { editingObject = obj },
                        onDelete = { showDeleteConfirm = obj }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        TrackObjectDialog(
            existing = null,
            routeId = routeId,
            onConfirm = { obj ->
                viewModel.saveTrackObject(obj)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingObject?.let { obj ->
        TrackObjectDialog(
            existing = obj,
            routeId = routeId,
            onConfirm = { updated ->
                viewModel.updateTrackObject(updated)
                editingObject = null
            },
            onDismiss = { editingObject = null }
        )
    }

    showDeleteConfirm?.let { obj ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Löschen?") },
            text = { Text("${obj.type.label} bei km ${obj.km} wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTrackObject(obj)
                    showDeleteConfirm = null
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun TrackObjectCard(
    obj: TrackObject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = runCatching { Color(android.graphics.Color.parseColor(obj.type.colorHex)) }
        .getOrDefault(Color.Gray)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Typ-Badge
            Surface(
                color = typeColor,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(width = 48.dp, height = 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        obj.type.shortLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        "km %.3f".format(obj.km),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        obj.type.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (obj.name.isNotEmpty()) Text(obj.name, style = MaterialTheme.typography.bodyMedium)
                // Zusatzinfos
                val details = buildList {
                    obj.speedLimit?.let { add("${it} km/h") }
                    obj.gradient?.let { add("%.1f‰".format(it)) }
                    if (obj.notes.isNotEmpty()) add(obj.notes)
                }
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackObjectDialog(
    existing: TrackObject?,
    routeId: Long,
    onConfirm: (TrackObject) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(existing?.type ?: TrackObjectType.STATION) }
    var km by remember { mutableStateOf(existing?.km?.toString() ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var speedLimit by remember { mutableStateOf(existing?.speedLimit?.toString() ?: "") }
    var speedLimitEnd by remember { mutableStateOf(existing?.speedLimitEnd?.toString() ?: "") }
    var gradient by remember { mutableStateOf(existing?.gradient?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Objekt hinzufügen" else "Objekt bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Typ-Auswahl
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Typ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        TrackObjectType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Km-Position
                OutlinedTextField(
                    value = km,
                    onValueChange = { km = it },
                    label = { Text("Position (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name / Bezeichnung") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Typ-abhängige Felder
                when (selectedType) {
                    TrackObjectType.SPEED_CHANGE, TrackObjectType.SLOW_SPEED -> {
                        OutlinedTextField(
                            value = speedLimit,
                            onValueChange = { speedLimit = it },
                            label = { Text("Geschwindigkeit (km/h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (selectedType == TrackObjectType.SLOW_SPEED) {
                            OutlinedTextField(
                                value = speedLimitEnd,
                                onValueChange = { speedLimitEnd = it },
                                label = { Text("Ende La (km)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    TrackObjectType.GRADIENT -> {
                        OutlinedTextField(
                            value = gradient,
                            onValueChange = { gradient = it },
                            label = { Text("Steigung in ‰ (negativ = Gefälle)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }

                // Notizen
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val kmVal = km.replace(",", ".").toDoubleOrNull() ?: return@TextButton
                onConfirm(
                    TrackObject(
                        id = existing?.id ?: 0,
                        routeId = routeId,
                        km = kmVal,
                        type = selectedType,
                        name = name.trim(),
                        speedLimit = speedLimit.toIntOrNull(),
                        speedLimitEnd = speedLimitEnd.replace(",", ".").toDoubleOrNull(),
                        gradient = gradient.replace(",", ".").toDoubleOrNull(),
                        notes = notes.trim()
                    )
                )
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
