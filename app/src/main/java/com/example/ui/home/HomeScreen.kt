package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color
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
                items(homeUiState.eventList) { event ->
                    EventCard(
                        event = event,
                        currentTime = currentTime,
                        onClick = { navigateToDetail(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun EventCard(
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

    // Bez roku — design karty listy pokazuje "12 sierpnia, 18:00" (rok jest tylko w
    // widoku szczegółów, gdzie miejsca jest więcej).
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, HH:mm", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(nextTimestamp))

    val hasImage = event.imageUri.isNotBlank()
    val themeConfig = com.example.ui.theme.EventThemes.getTheme(event.theme)
    val textColor = if (hasImage) Color.White else themeConfig.textColor
    val secondaryTextColor = if (hasImage) Color.White.copy(alpha = 0.8f) else themeConfig.secondaryTextColor
    val backgroundColor = themeConfig.backgroundColor
    val fontFamily = themeConfig.fontFamily
    val titleFontWeight = themeConfig.titleFontWeight
    val pillTextColor = if (hasImage) Color.White.copy(alpha = 0.85f) else themeConfig.textColor.copy(alpha = 0.6f)
    val progressTrackColor = if (hasImage) Color.White.copy(alpha = 0.25f) else themeConfig.accentColor.copy(alpha = 0.15f)
    val progressFillColor = if (hasImage) Color.White else themeConfig.accentColor
    // Pigułka z liczbą dni miała ZAWSZE białe półprzezroczyste tło, ale tekst na niej
    // jest jasny dla ciemnych kart (motyw "Nocny", zdjęcie w tle) — jasny na jasnym,
    // nieczytelne. luminance() zamiast sprawdzania konkretnej nazwy motywu ("Night"),
    // żeby to samo zadziałało poprawnie dla każdego przyszłego ciemnego motywu.
    val isDarkCard = hasImage || backgroundColor.luminance() < 0.5f
    val pillBg = if (isDarkCard) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (themeConfig.hasDecorativeBorder && !hasImage) androidx.compose.ui.Modifier.border(2.dp, themeConfig.accentColor, RoundedCornerShape(32.dp))
                else androidx.compose.ui.Modifier
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
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
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = titleFontWeight,
                            fontFamily = fontFamily,
                            fontSize = 17.sp
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(pillBg, RoundedCornerShape(100))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isPast) "$daysLeft dni temu" else "$daysLeft dni",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = fontFamily,
                                fontSize = 17.sp
                            ),
                            color = pillTextColor,
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
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
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
