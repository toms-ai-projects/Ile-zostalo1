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
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    eventDetails: EventDetails,
    onValueChange: (EventDetails) -> Unit,
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
    
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val savedPath = com.example.utils.ImageUtils.saveAndScaleImage(context, uri)
                if (savedPath != null) {
                    onValueChange(eventDetails.copy(imageUri = savedPath))
                } else {
                    // Fallback to URI if copy fails
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
    
    val timePickerState = rememberTimePickerState(
        initialHour = if (eventDetails.targetTimestamp > 0) calendar.get(Calendar.HOUR_OF_DAY) else 12,
        initialMinute = if (eventDetails.targetTimestamp > 0) calendar.get(Calendar.MINUTE) else 0,
        is24Hour = true
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TextField(
            value = eventDetails.name,
            onValueChange = { onValueChange(eventDetails.copy(name = it)) },
            placeholder = { Text("Nazwa wydarzenia") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Date selection
        val dateString = if (eventDetails.targetTimestamp > 0) {
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(eventDetails.targetTimestamp))
        } else {
            "Wybierz datę"
        }

        TextField(
            value = dateString,
            onValueChange = { },
            placeholder = { Text("Data wydarzenia") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Wybierz datę")
                }
            },
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                showDatePicker = true
                            }
                        }
                    }
                },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { utcDateMillis ->
                            val localCalendar = Calendar.getInstance()
                            if (eventDetails.targetTimestamp > 0) {
                                localCalendar.timeInMillis = eventDetails.targetTimestamp
                            }
                            val hour = localCalendar.get(Calendar.HOUR_OF_DAY)
                            val minute = localCalendar.get(Calendar.MINUTE)
                            
                            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { 
                                timeInMillis = utcDateMillis 
                            }
                            val year = utcCalendar.get(Calendar.YEAR)
                            val month = utcCalendar.get(Calendar.MONTH)
                            val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
                            
                            localCalendar.set(year, month, day, hour, minute, 0)
                            onValueChange(eventDetails.copy(targetTimestamp = localCalendar.timeInMillis))
                        }
                        showDatePicker = false
                    }) {
                        Text("Zapisz")
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

        // Time selection
        val timeString = if (eventDetails.targetTimestamp > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(eventDetails.targetTimestamp))
        } else {
            "Wybierz czas"
        }

        TextField(
            value = timeString,
            onValueChange = { },
            placeholder = { Text("Czas wydarzenia") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Filled.Schedule, contentDescription = "Wybierz czas")
                }
            },
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                showTimePicker = true
                            }
                        }
                    }
                },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Wybierz czas") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val localCalendar = Calendar.getInstance()
                        if (eventDetails.targetTimestamp > 0) {
                            localCalendar.timeInMillis = eventDetails.targetTimestamp
                        }
                        localCalendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        localCalendar.set(Calendar.MINUTE, timePickerState.minute)
                        localCalendar.set(Calendar.SECOND, 0)
                        onValueChange(eventDetails.copy(targetTimestamp = localCalendar.timeInMillis))
                        showTimePicker = false
                    }) {
                        Text("Zapisz")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }

        // Recurrence selection
        var recurrenceExpanded by remember { mutableStateOf(false) }
        val recurrenceOptions = listOf(
            com.example.data.RecurrenceType.NONE to "Jednorazowe",
            com.example.data.RecurrenceType.WEEKLY to "Co tydzień",
            com.example.data.RecurrenceType.MONTHLY to "Co miesiąc",
            com.example.data.RecurrenceType.YEARLY to "Co rok"
        )
        val selectedRecurrenceLabel = recurrenceOptions.find { it.first.name == eventDetails.recurrence }?.second ?: "Jednorazowe"

        ExposedDropdownMenuBox(
            expanded = recurrenceExpanded,
            onExpandedChange = { recurrenceExpanded = !recurrenceExpanded }
        ) {
            TextField(
                value = selectedRecurrenceLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Powtarzalność") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = recurrenceExpanded,
                onDismissRequest = { recurrenceExpanded = false }
            ) {
                recurrenceOptions.forEach { (type, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onValueChange(eventDetails.copy(recurrence = type.name))
                            recurrenceExpanded = false
                        }
                    )
                }
            }
        }

        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Przypomnienie", style = MaterialTheme.typography.titleMedium)
        
        val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
        } else null
        
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
        
        var reminderUnit by remember(eventDetails.id) { 
            mutableStateOf(
                when {
                    eventDetails.reminderMinutes.isNotEmpty() -> "Minuty"
                    eventDetails.reminderHours.isNotEmpty() -> "Godziny"
                    else -> "Dni"
                }
            )
        }
        val currentReminderValue = when (reminderUnit) {
            "Minuty" -> eventDetails.reminderMinutes
            "Godziny" -> eventDetails.reminderHours
            else -> eventDetails.reminderDays
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = currentReminderValue,
                onValueChange = { 
                    var filteredValue = it.filter { char -> char.isDigit() }
                    
                    if (filteredValue.isNotEmpty()) {
                        val intVal = filteredValue.toIntOrNull() ?: 0
                        filteredValue = when (reminderUnit) {
                            "Dni" -> if (intVal > 365) "365" else intVal.toString()
                            "Godziny" -> if (intVal > 24) "24" else intVal.toString()
                            "Minuty" -> if (intVal > 60) "60" else intVal.toString()
                            else -> filteredValue
                        }
                    }

                    val newDays = if (reminderUnit == "Dni") filteredValue else ""
                    val newHours = if (reminderUnit == "Godziny") filteredValue else ""
                    val newMinutes = if (reminderUnit == "Minuty") filteredValue else ""
                    onValueChange(eventDetails.copy(reminderDays = newDays, reminderHours = newHours, reminderMinutes = newMinutes)) 
                    notificationPermissionState?.launchPermissionRequest()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        showExactAlarmDialog = true
                    }
                },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(80.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            )
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.weight(1f)
            ) {
                SegmentedButton(
                    selected = reminderUnit == "Dni",
                    onClick = { 
                        reminderUnit = "Dni"
                        var newDays = currentReminderValue
                        if (newDays.isNotEmpty()) {
                            val intVal = newDays.toIntOrNull() ?: 0
                            newDays = if (intVal > 365) "365" else intVal.toString()
                        }
                        onValueChange(eventDetails.copy(reminderDays = newDays, reminderHours = "", reminderMinutes = ""))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Dni")
                }
                SegmentedButton(
                    selected = reminderUnit == "Godziny",
                    onClick = { 
                        reminderUnit = "Godziny"
                        var newHours = currentReminderValue
                        if (newHours.isNotEmpty()) {
                            val intVal = newHours.toIntOrNull() ?: 0
                            newHours = if (intVal > 24) "24" else intVal.toString()
                        }
                        onValueChange(eventDetails.copy(reminderDays = "", reminderHours = newHours, reminderMinutes = ""))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Godz.")
                }
                SegmentedButton(
                    selected = reminderUnit == "Minuty",
                    onClick = { 
                        reminderUnit = "Minuty"
                        var newMinutes = currentReminderValue
                        if (newMinutes.isNotEmpty()) {
                            val intVal = newMinutes.toIntOrNull() ?: 0
                            newMinutes = if (intVal > 60) "60" else intVal.toString()
                        }
                        onValueChange(eventDetails.copy(reminderDays = "", reminderHours = "", reminderMinutes = newMinutes))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Min.")
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
        
TextField(
            value = eventDetails.note,
            onValueChange = { onValueChange(eventDetails.copy(note = it)) },
            placeholder = { Text("Notatka (opcjonalnie)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Tło wydarzenia", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text(if (eventDetails.imageUri.isNotBlank()) "Zmień zdjęcie" else "Wybierz zdjęcie")
            }
            if (eventDetails.imageUri.isNotBlank()) {
                TextButton(onClick = {
                    onValueChange(eventDetails.copy(imageUri = ""))
                }) {
                    Text("Usuń")
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

        Spacer(modifier = Modifier.height(8.dp))

        // Color selection
        Text("Wybierz motyw", style = MaterialTheme.typography.titleMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(com.example.ui.theme.EventThemes.themes) { themeConfig ->
                val isSelected = eventDetails.theme == themeConfig.name
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onValueChange(eventDetails.copy(theme = themeConfig.name))
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp, 96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (themeConfig.name == "Classic") Color(eventDetails.colorArgb) else themeConfig.backgroundColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .then(
                                if (themeConfig.hasDecorativeBorder) Modifier.border(1.dp, themeConfig.accentColor, RoundedCornerShape(6.dp)).padding(2.dp) else Modifier
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("12", color = themeConfig.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = themeConfig.fontFamily)
                            Text("DNI", color = themeConfig.labelColor, fontSize = 8.sp, fontFamily = themeConfig.fontFamily)
                        }
                        if (themeConfig.hasCornerIcon) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = themeConfig.accentColor,
                                modifier = Modifier.size(12.dp).align(Alignment.TopEnd)
                            )
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSaveClick,
            enabled = isEntryValid,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("ZAPISZ WYDARZENIE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
        }
    }
}
