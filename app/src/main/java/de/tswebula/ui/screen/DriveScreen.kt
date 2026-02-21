package de.tswebula.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tswebula.data.model.*
import de.tswebula.ui.viewmodel.DriveViewModel
import de.tswebula.ui.viewmodel.DriveViewState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    viewModel: DriveViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showTimeDialog by remember { mutableStateOf(false) }
    var showOffsetDialog by remember { mutableStateOf(false) }

    // Kombinierte Liste aus Streckenobjekten und Fahrplanhalten
    val allItems by remember(state.trackObjects, state.timetable) {
        derivedStateOf { buildEbulaItems(state.trackObjects, state.timetable) }
    }

    // Auto-Scroll zur aktuellen Position
    LaunchedEffect(state.currentKm, state.autoScrollEnabled) {
        if (!state.autoScrollEnabled) return@LaunchedEffect
        val idx = allItems.indexOfLast { it.km <= state.currentKm }
        if (idx >= 0) {
            scope.launch { listState.animateScrollToItem(idx.coerceAtLeast(0)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.route?.name ?: "Fahrt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Km-Offset Knopf
                    IconButton(onClick = { showOffsetDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Km-Korrektur")
                    }
                    // Auto-Scroll Toggle
                    IconButton(onClick = { viewModel.toggleAutoScroll() }) {
                        Icon(
                            if (state.autoScrollEnabled) Icons.Default.LockOpen
                            else Icons.Default.Lock,
                            contentDescription = "Auto-Scroll",
                            tint = if (state.autoScrollEnabled)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B1B2F),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF12121F)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Info-Leiste oben ──────────────────────────────
            InfoBar(state = state, onTimeClick = { showTimeDialog = true })

            // ── EBuLa Streifen ────────────────────────────────
            Row(modifier = Modifier.weight(1f)) {
                // Km-Skala links
                KmScale(
                    allItems = allItems,
                    listState = listState,
                    currentKm = state.currentKm
                )

                // Hauptstreifen
                EbulaStrip(
                    allItems = allItems,
                    currentKm = state.currentKm,
                    listState = listState
                )
            }

            // ── Steuer-Leiste unten ───────────────────────────
            ControlBar(
                state = state,
                onStartStop = {
                    if (state.isRunning) viewModel.stopAutoTick()
                    else viewModel.startAutoTick()
                },
                onTimeSet = { showTimeDialog = true }
            )
        }
    }

    // Zeit-Dialog
    if (showTimeDialog) {
        TimeInputDialog(
            currentTime = state.currentTimeStr,
            onConfirm = { time ->
                viewModel.setCurrentTime(time)
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }

    // Km-Offset Dialog
    if (showOffsetDialog) {
        KmOffsetDialog(
            currentOffset = state.kmOffset,
            onAdjust = { delta -> viewModel.adjustKmOffset(delta) },
            onReset = { viewModel.resetKmOffset() },
            onDismiss = { showOffsetDialog = false }
        )
    }
}

// ── Info-Leiste ───────────────────────────────────────────────────────
@Composable
private fun InfoBar(state: DriveViewState, onTimeClick: () -> Unit) {
    Surface(
        color = Color(0xFF1B1B2F),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aktueller km
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("km", color = Color(0xFF9E9E9E), fontSize = 10.sp)
                Text(
                    "%.1f".format(state.currentKm),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            // Vorherige / nächste Station
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                state.prevStation?.let {
                    Text("◀ ${it.stationName}", color = Color(0xFF9E9E9E), fontSize = 11.sp)
                }
                state.nextStation?.let {
                    Text(
                        "▶ ${it.stationName}  ${it.arrivalTime}",
                        color = Color(0xFF4FC3F7),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Uhrzeit (klickbar zum Setzen)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTimeClick() }
            ) {
                Text("Zeit", color = Color(0xFF9E9E9E), fontSize = 10.sp)
                Text(
                    state.currentTimeStr.ifEmpty { "--:--" },
                    color = Color(0xFF4FC3F7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
    }
}

// ── EBuLa Streifen ────────────────────────────────────────────────────
@Composable
private fun EbulaStrip(
    allItems: List<EbulaItem>,
    currentKm: Double,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(start = 4.dp)
    ) {
        items(allItems, key = { "${it.km}_${it.type}" }) { item ->
            EbulaItemRow(item = item, currentKm = currentKm)
        }
        // Puffer am Ende
        item { Spacer(modifier = Modifier.height(200.dp)) }
    }
}

@Composable
private fun EbulaItemRow(item: EbulaItem, currentKm: Double) {
    val isPassed = item.km < currentKm
    val isCurrent = item.km in (currentKm - 0.5)..(currentKm + 0.5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    isCurrent -> Color(0xFF1A3A1A)
                    isPassed -> Color(0xFF0D0D1A)
                    else -> Color.Transparent
                }
            )
            .padding(vertical = 2.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Positions-Linie + Symbol
        Box(
            modifier = Modifier.width(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vertikale Linie (Strecke)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(
                        if (isPassed) Color(0xFF37474F) else Color(0xFF4FC3F7)
                    )
            )
            // Objekt-Symbol
            item.symbol?.let { symbol ->
                Surface(
                    color = item.color.copy(alpha = if (isPassed) 0.4f else 1f),
                    shape = if (item.type == EbulaItemType.STATION)
                        RoundedCornerShape(4.dp) else CircleShape,
                    modifier = Modifier.size(if (item.type == EbulaItemType.STATION) 40.dp else 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            symbol,
                            color = Color.White,
                            fontSize = if (item.type == EbulaItemType.STATION) 8.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }

            // Aktueller Positions-Pfeil
            if (isCurrent) {
                Box(
                    modifier = Modifier.offset(x = 28.dp)
                ) {
                    Text("◀", color = Color(0xFF76FF03), fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text-Info
        Column {
            Text(
                item.label,
                color = if (isPassed) Color(0xFF616161)
                else if (item.type == EbulaItemType.STATION) Color.White
                else Color(0xFFB0BEC5),
                fontWeight = if (item.type == EbulaItemType.STATION) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (item.type == EbulaItemType.STATION) 15.sp else 12.sp
            )
            item.sublabel?.let {
                Text(it, color = Color(0xFF78909C), fontSize = 11.sp)
            }
        }
    }
}

// ── Km-Skala ──────────────────────────────────────────────────────────
@Composable
private fun KmScale(
    allItems: List<EbulaItem>,
    listState: LazyListState,
    currentKm: Double
) {
    LazyColumn(
        state = listState,
        userScrollEnabled = false,
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(Color(0xFF0D0D1A))
    ) {
        items(allItems, key = { "km_${it.km}_${it.type}" }) { item ->
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .fillMaxWidth()
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "%.1f".format(item.km),
                    color = if (item.km < currentKm) Color(0xFF37474F) else Color(0xFF546E7A),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        item { Spacer(modifier = Modifier.height(200.dp)) }
    }
}

// ── Steuerleiste ──────────────────────────────────────────────────────
@Composable
private fun ControlBar(
    state: DriveViewState,
    onStartStop: () -> Unit,
    onTimeSet: () -> Unit
) {
    Surface(
        color = Color(0xFF1B1B2F),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start/Stop Auto-Ticker
            Button(
                onClick = onStartStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) Color(0xFFB71C1C) else Color(0xFF1B5E20)
                )
            ) {
                Icon(
                    if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(if (state.isRunning) "Stop" else "Start")
            }

            // Zeit manuell setzen
            OutlinedButton(onClick = onTimeSet) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Zeit setzen")
            }
        }
    }
}

// ── Dialoge ───────────────────────────────────────────────────────────
@Composable
private fun TimeInputDialog(
    currentTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var time by remember { mutableStateOf(currentTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ingame-Zeit setzen") },
        text = {
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Zeit (HH:mm)") },
                placeholder = { Text("09:43") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(time) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun KmOffsetDialog(
    currentOffset: Double,
    onAdjust: (Double) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Km-Korrektur") },
        text = {
            Column {
                Text(
                    "Aktueller Versatz: %.2f km".format(currentOffset),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(-1.0, -0.1, 0.1, 1.0).forEach { delta ->
                        OutlinedButton(
                            onClick = { onAdjust(delta) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                if (delta > 0) "+$delta" else "$delta",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onReset) { Text("Zurücksetzen") }
        }
    )
}

// ── Datenmodell für EBuLa-Liste ───────────────────────────────────────
enum class EbulaItemType { STATION, SPEED_CHANGE, BUE, SIGNAL, TUNNEL, GRADIENT, SLOW_SPEED, KM_MARKER }

data class EbulaItem(
    val km: Double,
    val type: EbulaItemType,
    val label: String,
    val sublabel: String? = null,
    val symbol: String? = null,
    val color: Color = Color.Gray
)

fun buildEbulaItems(
    trackObjects: List<TrackObject>,
    timetable: List<TimetableEntry>
): List<EbulaItem> {
    val items = mutableListOf<EbulaItem>()

    // Streckenobjekte
    trackObjects.forEach { obj ->
        val item = when (obj.type) {
            TrackObjectType.STATION -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.STATION,
                label = obj.name.ifEmpty { "Bahnhof" },
                sublabel = obj.notes.ifEmpty { null },
                symbol = "Bf",
                color = Color(0xFF1565C0)
            )
            TrackObjectType.SPEED_CHANGE -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.SPEED_CHANGE,
                label = "V ${obj.speedLimit ?: "??"} km/h",
                symbol = "${obj.speedLimit}",
                color = Color(0xFFE65100)
            )
            TrackObjectType.BAHNUEBERGANG -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.BUE,
                label = "BÜ ${obj.name}",
                symbol = "BÜ",
                color = Color(0xFFC62828)
            )
            TrackObjectType.SIGNAL -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.SIGNAL,
                label = "Signal ${obj.name}",
                symbol = "Sig",
                color = Color(0xFF6A1B9A)
            )
            TrackObjectType.TUNNEL_START -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.TUNNEL,
                label = "▶ Tunnel ${obj.name}",
                symbol = "Tu",
                color = Color(0xFF37474F)
            )
            TrackObjectType.TUNNEL_END -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.TUNNEL,
                label = "Tunnel Ende ◀",
                symbol = "Tu",
                color = Color(0xFF37474F)
            )
            TrackObjectType.GRADIENT -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.GRADIENT,
                label = "%.1f‰ ${if ((obj.gradient ?: 0.0) >= 0) "↗" else "↘"}".format(obj.gradient ?: 0.0),
                symbol = "‰",
                color = Color(0xFF2E7D32)
            )
            TrackObjectType.SLOW_SPEED -> EbulaItem(
                km = obj.km,
                type = EbulaItemType.SLOW_SPEED,
                label = "La ${obj.speedLimit ?: "??"} km/h",
                sublabel = obj.speedLimitEnd?.let { "bis km %.1f".format(it) },
                symbol = "La",
                color = Color(0xFFBF360C)
            )
        }
        items.add(item)
    }

    // Fahrplanhalte (die noch nicht als Streckenobjekt drin sind)
    timetable.filter { entry ->
        entry.isStop && trackObjects.none {
            it.type == TrackObjectType.STATION &&
                    kotlin.math.abs(it.km - entry.km) < 0.05
        }
    }.forEach { entry ->
        items.add(
            EbulaItem(
                km = entry.km,
                type = EbulaItemType.STATION,
                label = entry.stationName,
                sublabel = buildString {
                    if (entry.arrivalTime.isNotEmpty()) append("an: ${entry.arrivalTime}  ")
                    if (entry.departureTime.isNotEmpty()) append("ab: ${entry.departureTime}")
                }.trim(),
                symbol = "Bf",
                color = Color(0xFF1565C0)
            )
        )
    }

    return items.sortedWith(compareBy({ it.km }, { it.type.ordinal }))
}
