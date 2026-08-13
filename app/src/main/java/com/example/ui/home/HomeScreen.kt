package com.example.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.example.utils.ExportUtils
import com.example.data.ExportedEvent
import com.example.data.Event
import kotlinx.coroutines.launch
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.AppViewModelProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToDetail: (Int) -> Unit,
    navigateToAdd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    var showImportDialog by remember { mutableStateOf(false) }
    var eventToImport by remember { mutableStateOf<ExportedEvent?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val exported = ExportUtils.decodeEventFromJson(result.contents)
            if (exported != null) {
                eventToImport = exported
            } else {
                Toast.makeText(context, "Nieprawidłowy kod QR", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Zeskanuj kod QR wydarzenia")
            options.setBeepEnabled(false)
            scanLauncher.launch(options)
        } else {
            Toast.makeText(context, "Uprawnienie do kamery jest wymagane", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    val exported = ExportUtils.decodeEventFromJson(json)
                    if (exported != null) {
                        eventToImport = exported
                    } else {
                        Toast.makeText(context, "Nieprawidłowy plik", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Błąd odczytu pliku", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Ticker to refresh time periodically
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onScanQrClick = {
                showImportDialog = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val options = ScanOptions()
                    options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    options.setPrompt("Zeskanuj kod QR wydarzenia")
                    options.setBeepEnabled(false)
                    scanLauncher.launch(options)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onPickFileClick = {
                showImportDialog = false
                filePickerLauncher.launch("*/*")
            }
        )
    }

    eventToImport?.let { event ->
        ImportPreviewDialog(
            event = event,
            onDismiss = { eventToImport = null },
            onConfirm = {
                coroutineScope.launch {
                    val newEvent = Event(
                        name = event.name,
                        targetTimestamp = event.targetTimestamp,
                        recurrence = event.recurrence,
                        colorArgb = 0xFF6200EE.toInt(),
                        theme = "Classic"
                    )
                    viewModel.insertEvent(newEvent)
                    eventToImport = null
                    Toast.makeText(context, "Wydarzenie zaimportowane", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.background(com.example.ui.theme.IconButtonBg, CircleShape)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Download, contentDescription = "Zaimportuj wydarzenie")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },


                floatingActionButton = {
            FloatingActionButton(
                onClick = navigateToAdd,
                containerColor = com.example.ui.theme.AccentOrange,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj wydarzenie", modifier = Modifier.size(32.dp))
            }
        },
    ) { innerPadding ->
        if (homeUiState.eventList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Brak wydarzeń.",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kliknij +, aby dodać nowe odliczanie.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                homeUiState.featured?.let { featured ->
                    item(key = "featured-${featured.id}") {
                        FeaturedEventCard(
                            event = featured,
                            currentTime = currentTime,
                            onClick = { navigateToDetail(featured.id) }
                        )
                    }
                }
                if (homeUiState.laterEvents.isNotEmpty()) {
                    item(key = "later-label") {
                        Text(
                            text = "PÓŹNIEJ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = TextLight.copy(alpha = 0.45f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    item(key = "timeline") {
                        TimelineSection(
                            events = homeUiState.laterEvents,
                            currentTime = currentTime,
                            onClick = navigateToDetail
                        )
                    }
                }
            }
        }
    }
}

/**
 * Karta "najbliższe" (wariant C designu Home) — pierścień postępu z lewej (ta sama
 * krzywa Event.progressFraction() co dawny pasek/DetailScreen), nazwa/data/pigułka
 * przypomnienia z prawej. Jedyna karta na Home z pierścieniem — reszta wydarzeń
 * (TimelineEventRow) używa cieńszego paska, tak jak wcześniej EventCard.
 */
@Composable
fun FeaturedEventCard(
    event: Event,
    currentTime: Long,
    onClick: () -> Unit
) {
    val nextTimestamp = event.getNextOccurrence(currentTime)
    val diffMillis = nextTimestamp - currentTime
    val isPast = diffMillis < 0
    val absDiff = Math.abs(diffMillis)
    val daysLeft = TimeUnit.MILLISECONDS.toDays(absDiff)
    val progress = event.progressFraction(currentTime)

    val dateFormatter = remember { SimpleDateFormat("dd MMMM, HH:mm", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(nextTimestamp))

    val hasImage = event.imageUri.isNotBlank()
    val themeConfig = EventThemes.getTheme(event.theme)
    val textColor = if (hasImage) Color.White else themeConfig.textColor
    val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
    val labelColor = if (hasImage) Color.White.copy(alpha = 0.7f) else themeConfig.labelColor
    val backgroundColor = themeConfig.backgroundColor
    val fontFamily = themeConfig.fontFamily
    val titleFontWeight = themeConfig.titleFontWeight
    // Ten sam podział co pierścień na DetailScreen: dividerColor = tor, iconTint = wypełnienie.
    val trackColor = if (hasImage) Color.White.copy(alpha = 0.3f) else themeConfig.dividerColor
    val ringColor = if (hasImage) Color.White else themeConfig.iconTint
    val isDarkCard = hasImage || backgroundColor.luminance() < 0.5f
    val pillBg = if (isDarkCard) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.6f)

    val hasReminder = (event.reminderDays ?: 0) > 0 || (event.reminderHours ?: 0) > 0 || (event.reminderMinutes ?: 0) > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (themeConfig.hasDecorativeBorder && !hasImage) Modifier.border(2.dp, themeConfig.accentColor, RoundedCornerShape(32.dp))
                else Modifier
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            if (hasImage) {
                val imageModel = if (event.imageUri.startsWith("/")) java.io.File(event.imageUri) else event.imageUri
                coil.compose.AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(modifier = Modifier.matchParentSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.7f))
                    )
                ))
            }
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(108.dp)) {
                        val strokeWidthPx = 9.dp.toPx()
                        val diameterPx = 92.dp.toPx()
                        val topLeft = Offset(
                            (size.width - diameterPx) / 2f,
                            (size.height - diameterPx) / 2f
                        )
                        val arcSize = androidx.compose.ui.geometry.Size(diameterPx, diameterPx)
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                            topLeft = topLeft,
                            size = arcSize
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                            topLeft = topLeft,
                            size = arcSize
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = daysLeft.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = if (titleFontWeight == FontWeight.Normal) FontWeight.Medium else FontWeight.Black,
                                fontSize = 30.sp,
                                fontFamily = fontFamily
                            ),
                            color = textColor,
                            maxLines = 1
                        )
                        Text(
                            text = if (isPast) "DNI TEMU" else "DNI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = fontFamily
                            ),
                            color = labelColor,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NAJBLIŻSZE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontFamily = fontFamily
                        ),
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = titleFontWeight,
                            fontFamily = fontFamily,
                            fontSize = 20.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontFamily = fontFamily),
                        color = secondaryTextColor,
                        maxLines = 1
                    )
                    // Pigułka przypomnienia — tylko gdy wydarzenie faktycznie je ma
                    // (bez przypomnienia karta jest po prostu krótsza, bez pustego
                    // elementu "Brak przypomnienia").
                    if (hasReminder) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .background(pillBg, RoundedCornerShape(100))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Przypomnienie",
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = event.reminderText(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFamily = fontFamily),
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Oś czasu z pozostałymi wydarzeniami ("PÓŹNIEJ") — pionowa szyna z kolorową kropką
 * per motyw wydarzenia i ciągłą linią. Linia jest rysowana per-wiersz na pełną
 * wysokość wiersza (razem z marginesem karty), więc mimo wizualnego odstępu między
 * kartami sama linia wygląda na nieprzerwaną przez całą kolumnę.
 */
@Composable
fun TimelineSection(
    events: List<Event>,
    currentTime: Long,
    onClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        events.forEach { event ->
            TimelineEventRow(
                event = event,
                currentTime = currentTime,
                onClick = { onClick(event.id) }
            )
        }
    }
}

@Composable
private fun TimelineEventRow(
    event: Event,
    currentTime: Long,
    onClick: () -> Unit
) {
    val nextTimestamp = event.getNextOccurrence(currentTime)
    val diffMillis = nextTimestamp - currentTime
    val isPast = diffMillis < 0
    val absDiff = Math.abs(diffMillis)
    val daysLeft = TimeUnit.MILLISECONDS.toDays(absDiff)
    val progress = event.progressFraction(currentTime)

    val dateFormatter = remember { SimpleDateFormat("d MMMM", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(nextTimestamp))

    val hasImage = event.imageUri.isNotBlank()
    val themeConfig = EventThemes.getTheme(event.theme)
    val textColor = if (hasImage) Color.White else themeConfig.textColor
    val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
    val backgroundColor = themeConfig.backgroundColor
    val fontFamily = themeConfig.fontFamily
    val titleFontWeight = themeConfig.titleFontWeight
    val progressTrackColor = if (hasImage) Color.White.copy(alpha = 0.25f) else themeConfig.accentColor.copy(alpha = 0.15f)
    val progressFillColor = if (hasImage) Color.White else themeConfig.accentColor
    val dotColor = themeConfig.accentColor
    // Linia szyny jest neutralna (nie zależy od motywu) — kolor "niesie" kropka.
    val lineColor = TextLight.copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val centerX = size.width / 2f
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = dotColor,
                    radius = 5.dp.toPx(),
                    center = Offset(centerX, size.height / 2f)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .then(
                    if (themeConfig.hasDecorativeBorder && !hasImage) Modifier.border(2.dp, themeConfig.accentColor, RoundedCornerShape(24.dp))
                    else Modifier
                )
                .clickable { onClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box {
                if (hasImage) {
                    val imageModel = if (event.imageUri.startsWith("/")) java.io.File(event.imageUri) else event.imageUri
                    coil.compose.AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Box(modifier = Modifier.matchParentSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.7f))
                        )
                    ))
                }
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = titleFontWeight,
                                fontFamily = fontFamily,
                                fontSize = 15.sp
                            ),
                            color = textColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = daysLeft.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = QuicksandFontFamily,
                                    fontSize = 18.sp
                                ),
                                color = textColor,
                                maxLines = 1
                            )
                            Text(
                                text = if (isPast) "DNI TEMU" else "DNI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = QuicksandFontFamily,
                                    fontSize = 10.sp
                                ),
                                color = secondaryTextColor,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontFamily = fontFamily),
                        color = secondaryTextColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(progressTrackColor, RoundedCornerShape(100))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progress)
                                .fillMaxHeight()
                                .background(progressFillColor, RoundedCornerShape(100))
                        )
                    }
                }
            }
        }
    }
}
