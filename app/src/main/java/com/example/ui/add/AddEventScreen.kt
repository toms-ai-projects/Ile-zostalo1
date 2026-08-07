package com.example.ui.add
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import android.os.Build
import android.content.Intent
import android.provider.Settings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.text.input.KeyboardType
import android.app.AlarmManager


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppViewModelProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEventViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.eventUiState

    val title = if (uiState.eventDetails.id == 0) "Dodaj wydarzenie" else "Edytuj wydarzenie"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { innerPadding ->
        AddEventBody(
            eventDetails = uiState.eventDetails,
            onValueChange = viewModel::updateUiState,
            onSaveClick = {
                coroutineScope.launch {
                    viewModel.saveEvent()
                    navigateBack()
                }
            },
            isEntryValid = uiState.isEntryValid,
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddEventBody(
    eventDetails: com.example.ui.add.EventDetails,
    onValueChange: (com.example.ui.add.EventDetails) -> Unit,
    onSaveClick: () -> Unit,
    isEntryValid: Boolean,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val calendar = remember(eventDetails.targetTimestamp) {
        Calendar.getInstance().apply {
            if (eventDetails.targetTimestamp > 0) {
                timeInMillis = eventDetails.targetTimestamp
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (eventDetails.targetTimestamp > 0) eventDetails.targetTimestamp else System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    // DatePicker zawsze zwraca północ UTC dla wybranego dnia — trzeba
                    // odczytać rok/miesiąc/dzień przez kalendarz UTC, inaczej w strefach
                    // z ujemnym przesunięciem względem UTC (np. USA) wybrany dzień
                    // potrafi się cofnąć o jeden.
                    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                    calendar.set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
                    calendar.set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
                    calendar.set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
                    onValueChange(eventDetails.copy(targetTimestamp = calendar.timeInMillis))
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Anuluj")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    onValueChange(eventDetails.copy(targetTimestamp = calendar.timeInMillis))
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Anuluj")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    val dateString = if (eventDetails.targetTimestamp > 0) dateFormatter.format(Date(eventDetails.targetTimestamp)) else "Wybierz datę"
    val timeString = if (eventDetails.targetTimestamp > 0) timeFormatter.format(Date(eventDetails.targetTimestamp)) else "Wybierz godzinę"

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Zdjęcie z Photo Pickera trzeba skopiować do pliku aplikacji: surowy
                // content:// URI ma tylko tymczasowe uprawnienia (mogą wygasnąć np. po
                // restarcie) i nie da się go odczytać przez BitmapFactory.decodeFile,
                // z którego korzysta widget na ekranie głównym.
                val savedPath = com.example.utils.ImageUtils.saveAndScaleImage(context, uri)
                if (savedPath != null) {
                    onValueChange(eventDetails.copy(imageUri = savedPath))
                } else {
                    // Fallback, gdyby kopiowanie się nie powiodło — spróbuj przynajmniej
                    // utrwalić dostęp do oryginalnego URI (widget nadal go nie pokaże).
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        onValueChange(eventDetails.copy(imageUri = uri.toString()))
                    } catch (e2: Exception) {
                        onValueChange(eventDetails.copy(imageUri = uri.toString()))
                    }
                }
            }
        }
    )

    var notificationPermissionState: com.google.accompanist.permissions.PermissionState? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionState = com.google.accompanist.permissions.rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    var showExactAlarmDialog by remember { mutableStateOf(false) }
    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("Wymagane uprawnienie") },
            text = { Text("Aby przypomnienia przychodziły dokładnie o czasie, musisz zezwolić na dokładne alarmy w ustawieniach.") },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                }) {
                    Text("Przejdź do ustawień")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) { Text("Anuluj") }
            }
        )
    }

    val inputShape = RoundedCornerShape(18.dp)
    val inputColors = TextFieldDefaults.colors(
        focusedContainerColor = com.example.ui.theme.FormFieldBg,
        unfocusedContainerColor = com.example.ui.theme.FormFieldBg,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedTextColor = com.example.ui.theme.TextLight,
        unfocusedTextColor = com.example.ui.theme.TextLight,
        focusedPlaceholderColor = com.example.ui.theme.FormFieldPlaceholder,
        unfocusedPlaceholderColor = com.example.ui.theme.FormFieldPlaceholder
    )
    val labelStyle = TextStyle(
        fontFamily = com.example.ui.theme.QuicksandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = Color(0x732B241D)
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 100.dp), // Extra padding at bottom for the button
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("NAZWA", style = labelStyle)
            TextField(
                value = eventDetails.name,
                onValueChange = { onValueChange(eventDetails.copy(name = it)) },
                placeholder = { Text("Np. Urodziny", style = TextStyle(color = com.example.ui.theme.FormFieldPlaceholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                colors = inputColors,
                shape = inputShape
            )

            Text("KIEDY", style = labelStyle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = inputShape,
                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.FormFieldBg),
                    
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(dateString, color = if (eventDetails.targetTimestamp > 0) com.example.ui.theme.TextLight else com.example.ui.theme.FormFieldPlaceholder)
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = com.example.ui.theme.TextLight)
                    }
                }
                
                Card(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = inputShape,
                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.FormFieldBg),
                    
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(timeString, color = if (eventDetails.targetTimestamp > 0) com.example.ui.theme.TextLight else com.example.ui.theme.FormFieldPlaceholder)
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = com.example.ui.theme.TextLight) // Or a clock icon
                    }
                }
            }

            Text("CYKLICZNOŚĆ", style = labelStyle)
            var expanded by remember { mutableStateOf(false) }
            val options = listOf("NONE" to "Brak", "WEEKLY" to "Co tydzień", "MONTHLY" to "Co miesiąc", "YEARLY" to "Co rok")
            val selectedOption = options.find { it.first == eventDetails.recurrence }?.second ?: "Brak"

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    readOnly = true,
                    value = selectedOption,
                    onValueChange = { },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = inputColors,
                    shape = inputShape,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onValueChange(eventDetails.copy(recurrence = type))
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Text("PRZYPOMNIENIE", style = labelStyle)
            var reminderUnit by remember { 
                mutableStateOf(
                    if (eventDetails.reminderDays?.isNotEmpty() == true) "Dni" 
                    else if (eventDetails.reminderHours?.isNotEmpty() == true) "Godziny"
                    else if (eventDetails.reminderMinutes?.isNotEmpty() == true) "Minuty"
                    else "Dni"
                )
            }
            
            val currentReminderValue = when(reminderUnit) {
                "Dni" -> eventDetails.reminderDays ?: ""
                "Godziny" -> eventDetails.reminderHours ?: ""
                else -> eventDetails.reminderMinutes ?: ""
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = currentReminderValue,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        when (reminderUnit) {
                            "Dni" -> {
                                var newDays = filtered
                                if (newDays.isNotEmpty()) {
                                    val intVal = newDays.toIntOrNull() ?: 0
                                    newDays = if (intVal > 365) "365" else intVal.toString()
                                }
                                onValueChange(eventDetails.copy(reminderDays = newDays, reminderHours = "", reminderMinutes = ""))
                            }
                            "Godziny" -> {
                                var newHours = filtered
                                if (newHours.isNotEmpty()) {
                                    val intVal = newHours.toIntOrNull() ?: 0
                                    newHours = if (intVal > 24) "24" else intVal.toString()
                                }
                                onValueChange(eventDetails.copy(reminderDays = "", reminderHours = newHours, reminderMinutes = ""))
                            }
                            "Minuty" -> {
                                var newMinutes = filtered
                                if (newMinutes.isNotEmpty()) {
                                    val intVal = newMinutes.toIntOrNull() ?: 0
                                    newMinutes = if (intVal > 60) "60" else intVal.toString()
                                }
                                onValueChange(eventDetails.copy(reminderDays = "", reminderHours = "", reminderMinutes = newMinutes))
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            showExactAlarmDialog = true
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = inputColors,
                    shape = inputShape,
                    placeholder = { Text("0", color = com.example.ui.theme.FormFieldPlaceholder) }
                )
                
                // Pill segment
                Row(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(50))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    val units = listOf("Dni", "Godz.", "Min.")
                    val activeUnit = if (reminderUnit == "Godziny") "Godz." else if (reminderUnit == "Minuty") "Min." else "Dni"
                    
                    units.forEach { unit ->
                        val isSelected = activeUnit == unit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) com.example.ui.theme.AccentOrange else Color.Transparent)
                                .clickable { 
                                    if (unit == "Dni") reminderUnit = "Dni"
                                    else if (unit == "Godz.") reminderUnit = "Godziny"
                                    else if (unit == "Min.") reminderUnit = "Minuty"
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unit,
                                color = if (isSelected) Color.White else com.example.ui.theme.TextLight,
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = com.example.ui.theme.QuicksandFontFamily)
                            )
                        }
                    }
                }
            }

            if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
                Text(
                    "Brak uprawnień do powiadomień. Przypomnienia nie będą działać, dopóki nie wyrazisz zgody w ustawieniach.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text("NOTATKA", style = labelStyle)
            TextField(
                value = eventDetails.note,
                onValueChange = { onValueChange(eventDetails.copy(note = it)) },
                placeholder = { Text("Notatka (opcjonalnie)", color = com.example.ui.theme.FormFieldPlaceholder) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                colors = inputColors,
                shape = inputShape,
                maxLines = 5
            )

            Text("TŁO WYDARZENIA", style = labelStyle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AccentOrange)
                ) {
                    Text(if (eventDetails.imageUri.isNotBlank()) "Zmień zdjęcie" else "Wybierz zdjęcie")
                }
                if (eventDetails.imageUri.isNotBlank()) {
                    TextButton(onClick = { onValueChange(eventDetails.copy(imageUri = "")) }) {
                        Text("Usuń", color = com.example.ui.theme.TextLight)
                    }
                }
            }
            if (eventDetails.imageUri.isNotBlank()) {
                val imageModel = if (eventDetails.imageUri.startsWith("/")) java.io.File(eventDetails.imageUri) else eventDetails.imageUri
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Wybrane zdjęcie",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text("WYBIERZ MOTYW", style = labelStyle)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(com.example.ui.theme.EventThemes.themes) { themeConfig ->
                    val isSelected = eventDetails.theme == themeConfig.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onValueChange(eventDetails.copy(theme = themeConfig.name)) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp, 96.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeConfig.backgroundColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) com.example.ui.theme.AccentOrange else Color(0x33000000),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("12", color = themeConfig.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = themeConfig.fontFamily)
                                Text("DNI", color = themeConfig.labelColor, fontSize = 8.sp, fontFamily = themeConfig.fontFamily)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when(themeConfig.name) {
                                "Classic" -> "Klasyczny"
                                "Elegant" -> "Elegancki"
                                "Warm" -> "Ciepły"
                                else -> "Minimalistyczny"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) com.example.ui.theme.AccentOrange else com.example.ui.theme.TextLight
                        )
                    }
                }
            }
        }
        
        // Pinned Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, com.example.ui.theme.BgLight.copy(alpha=0.9f), com.example.ui.theme.BgLight),
                        startY = 0f
                    )
                )
                .padding(16.dp)
        ) {
            Button(
                onClick = onSaveClick,
                enabled = isEntryValid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.ui.theme.AccentOrange,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "ZAPISZ WYDARZENIE", 
                    style = TextStyle(
                        fontWeight = FontWeight.Bold, 
                        letterSpacing = 1.sp,
                        fontFamily = com.example.ui.theme.QuicksandFontFamily
                    )
                )
            }
        }
    }
}
