package com.example.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.export.ExportDialog
import com.example.utils.ExportUtils
import com.example.data.ExportedEvent
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navigateBack: () -> Unit,
    navigateToEdit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val event = uiState.event

    val graphicsLayer = rememberGraphicsLayer()

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showExportDialog by remember { mutableStateOf(false) }
    // Sekundy tykają tylko gdy ten ekran jest faktycznie na pierwszym planie (RESUMED).
    // To jedyne miejsce w appce z odświeżaniem co 1s — repeatOnLifecycle pauzuje pętlę
    // (i przestaje budzić CPU co sekundę) gdy appka trafi w tło albo ekran zgaśnie,
    // zamiast tykać bezwarunkowo tak długo, jak composable jest w kompozycji.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    if (showExportDialog && event != null) {
        val exportedEvent = ExportedEvent(
            name = event.name,
            targetTimestamp = event.targetTimestamp,
            recurrence = event.recurrence
        )
        val json = ExportUtils.encodeEventToJson(exportedEvent)
        val qrBitmap = remember { ExportUtils.generateQrCode(json) }
        val context = androidx.compose.ui.platform.LocalContext.current
        
        ExportDialog(
            qrBitmap = qrBitmap,
            onDismiss = { showExportDialog = false },
            onShareFile = {
                ExportUtils.shareEventAsFile(context, json, "${event.name}.iledni")
                showExportDialog = false
            }
        )
    }

    if (event == null) {
        // Loading or not found
        Scaffold { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val nextTimestamp = event.getNextOccurrence(currentTime)
    val diffMillis = nextTimestamp - currentTime
    val isPast = diffMillis < 0
    val absDiff = Math.abs(diffMillis)

    val daysLeft = TimeUnit.MILLISECONDS.toDays(absDiff)
    val hoursLeft = TimeUnit.MILLISECONDS.toHours(absDiff) % 24
    val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(absDiff) % 60
    val secondsLeft = TimeUnit.MILLISECONDS.toSeconds(absDiff) % 60
    // Uproszczenie: liczone względem oryginalnego targetTimestamp, nie najbliższego
    // wystąpienia cyklicznego — patrz komentarz przy Event.progressFraction().
    val progressFraction = event.progressFraction(currentTime)

    val themeConfig = com.example.ui.theme.EventThemes.getTheme(event.theme)
    val backgroundColor = themeConfig.backgroundColor
    
    val recurrenceLabel = when(event.recurrence) {
        "WEEKLY" -> "Co tydzień"
        "MONTHLY" -> "Co miesiąc"
        "YEARLY" -> "Co rok"
        else -> "Jednorazowe"
    }

    val hasImage = event.imageUri.isNotBlank()
    // Theme applies to text and accents. If there is an image, we enforce white text for visibility
    // except for Classic where we have a specific rule, but let us use the theme colors if no image.
    val textColor = if (hasImage) Color.White else themeConfig.textColor
    val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
    val labelColor = if (hasImage) Color.White.copy(alpha = 0.7f) else themeConfig.labelColor
    val dividerColor = if (hasImage) Color.White.copy(alpha = 0.3f) else themeConfig.dividerColor
    val iconTint = if (hasImage) Color.White else themeConfig.iconTint
    val cardBackgroundColor = if (hasImage) Color.Black.copy(alpha = 0.4f) else themeConfig.cardBackgroundColor
    val fontFamily = themeConfig.fontFamily
    val titleFontWeight = themeConfig.titleFontWeight


    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    IconButton(onClick = {
                        coroutineScope.launch {
                            try {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                com.example.utils.shareBitmap(context, bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Udostępnij obraz")
                    }
                    IconButton(onClick = { showExportDialog = true }, modifier = Modifier.background(com.example.ui.theme.IconButtonBg, shape = androidx.compose.foundation.shape.CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Eksportuj wydarzenie")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
                    .then(
                        if (themeConfig.hasDecorativeBorder && !hasImage) androidx.compose.ui.Modifier.border(2.dp, themeConfig.accentColor, RoundedCornerShape(36.dp)).padding(4.dp)
                        else androidx.compose.ui.Modifier
                    ),
                shape = RoundedCornerShape(36.dp),
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
                            .padding(26.dp)
                            .fillMaxWidth()
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = titleFontWeight, fontFamily = fontFamily),
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(nextTimestamp)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                                color = secondaryTextColor
                            )
                            if (event.recurrence != "NONE") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = recurrenceLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = fontFamily),
                                    color = secondaryTextColor
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { navigateToEdit(event.id) },
                                modifier = Modifier
                                    .background(cardBackgroundColor, shape = androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edytuj", tint = iconTint)
                            }
                            IconButton(
                                onClick = { 
                                    viewModel.deleteEvent()
                                    navigateBack()
                                },
                                modifier = Modifier
                                    .background(cardBackgroundColor, shape = androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Usuń", tint = iconTint)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pierścień postępu: promień 88dp / grubość 14dp / start na
                        // godzinie 12 — dokładnie wg wymiarów z pliku designu (koło
                        // 200x200, r=88, stroke-width=14, rotate(-90)).
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                            val strokeWidthPx = 14.dp.toPx()
                            val diameterPx = 176.dp.toPx()
                            val topLeft = androidx.compose.ui.geometry.Offset(
                                (size.width - diameterPx) / 2f,
                                (size.height - diameterPx) / 2f
                            )
                            val arcSize = androidx.compose.ui.geometry.Size(diameterPx, diameterPx)
                            drawArc(
                                color = dividerColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidthPx,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                ),
                                topLeft = topLeft,
                                size = arcSize
                            )
                            drawArc(
                                color = iconTint,
                                startAngle = -90f,
                                sweepAngle = 360f * progressFraction,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidthPx,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                ),
                                topLeft = topLeft,
                                size = arcSize
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = daysLeft.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = if (titleFontWeight == FontWeight.Normal) FontWeight.Medium else FontWeight.Black,
                                    fontSize = 56.sp,
                                    letterSpacing = (-2).sp,
                                    fontFamily = fontFamily
                                ),
                                color = textColor
                            )
                            Text(
                                text = if (isPast) "DNI TEMU" else "POZOSTAŁO DNI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontFamily = fontFamily
                                ),
                                color = labelColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CountdownUnit(value = hoursLeft.toString(), label = "Godzin", modifier = Modifier.weight(1f), textColor = textColor, cardBg = cardBackgroundColor, labelColor = labelColor, fontFamily = fontFamily)
                        CountdownUnit(value = minutesLeft.toString(), label = "Minut", modifier = Modifier.weight(1f), textColor = textColor, cardBg = cardBackgroundColor, labelColor = labelColor, fontFamily = fontFamily)
                        CountdownUnit(value = secondsLeft.toString(), label = "Sekund", modifier = Modifier.weight(1f), textColor = textColor, cardBg = cardBackgroundColor, labelColor = labelColor, fontFamily = fontFamily)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val hasReminder = (event.reminderDays ?: 0) > 0 || (event.reminderHours ?: 0) > 0 || (event.reminderMinutes ?: 0) > 0
                    val reminderText = if (hasReminder) {
                        val parts = mutableListOf<String>()
                        if ((event.reminderDays ?: 0) > 0) parts.add("${event.reminderDays} dni")
                        if ((event.reminderHours ?: 0) > 0) parts.add("${event.reminderHours} godz.")
                        if ((event.reminderMinutes ?: 0) > 0) parts.add("${event.reminderMinutes} min.")
                        "${parts.joinToString(", ")} przed"
                    } else {
                        "Brak przypomnienia"
                    }
                    
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .background(cardBackgroundColor, shape = RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasReminder) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Przypomnienie",
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "PRZYPOMNIENIE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = fontFamily),
                                color = labelColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = reminderText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                                color = textColor
                            )
                        }
                    }
                    
                    if (event.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardBackgroundColor, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "NOTATKA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = fontFamily),
                                color = labelColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.note,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                                color = textColor
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun CountdownUnit(value: String, label: String, modifier: Modifier = Modifier, textColor: Color = Color(0xFF001D35), cardBg: Color = Color.White.copy(alpha = 0.4f), labelColor: Color = Color(0xFF001D35).copy(alpha = 0.6f), fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.Default) {
    Column(
        modifier = modifier
            .background(cardBg, shape = RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = fontFamily),
            color = textColor
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = fontFamily),
            color = labelColor
        )
    }
}
