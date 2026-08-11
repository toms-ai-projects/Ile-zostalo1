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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Image
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
                title = {
                    Text(
                        title,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = com.example.ui.theme.QuicksandFontFamily,
                            color = com.example.ui.theme.TextLight
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack,
                        modifier = Modifier.background(com.example.ui.theme.IconButtonBg, CircleShape)
                    ) {
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
            // 22px między sekcjami (NAZWA/KIEDY/PRZYPOMNIENIE/NOTATKA/WYGLĄD) — luźniej niż
            // odstęp etykieta-do-pola wewnątrz każdej z nich (8dp, patrz Column w środku).
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NAZWA", style = labelStyle)
                TextField(
                    value = eventDetails.name,
                    onValueChange = { onValueChange(eventDetails.copy(name = it)) },
                    placeholder = { Text("np. Urodziny Mamy", style = TextStyle(color = com.example.ui.theme.FormFieldPlaceholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    colors = inputColors,
                    shape = inputShape
                )
            }

            // KIEDY: data, godzina I cykliczność razem pod jedną etykietą — w designie
            // "Powtarzalność" jest trzecim polem tej samej sekcji, nie osobną sekcją.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("KIEDY", style = labelStyle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        onClick = { showDatePicker = true },
                        // 1.3:1 względem pola czasu, jak w designie — pełna data z nazwą
                        // miesiąca ("13 sierpnia 2026") inaczej zawija się do dwóch linii.
                        modifier = Modifier.weight(1.3f),
                        shape = inputShape,
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.FormFieldBg),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                dateString,
                                style = TextStyle(fontSize = 15.sp),
                                color = if (eventDetails.targetTimestamp > 0) com.example.ui.theme.TextLight else com.example.ui.theme.FormFieldPlaceholder,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                // weight(1f) samo (bez drugiego wagowego elementu obok) zajmuje
                                // CAŁĄ przestrzeń do lewej od ikony — ikona zawsze ląduje tuż
                                // przy prawej krawędzi karty, a tekst obcina się wielokropkiem
                                // tylko gdy faktycznie nie mieści się w tej (szerokiej) przestrzeni.
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.DateRange, contentDescription = null, tint = com.example.ui.theme.AccentRust, modifier = Modifier.size(18.dp))
                        }
                    }

                    Card(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = inputShape,
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.FormFieldBg),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                timeString,
                                style = TextStyle(fontSize = 15.sp),
                                color = if (eventDetails.targetTimestamp > 0) com.example.ui.theme.TextLight else com.example.ui.theme.FormFieldPlaceholder,
                                // "Wybierz godzinę" (placeholder) zawijał się do 2 linii w węższej
                                // (weight 1f) karcie i wypychał ikonę poza jej granice, gdzie
                                // była przycinana przez zaokrąglone rogi Card — stąd wcześniej
                                // wyglądało to jak brakująca ikona.
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                // Patrz komentarz przy analogicznym Text w polu daty — weight(1f)
                                // samo (bez konkurującego wagowego Spacera) daje ikonie stałą
                                // pozycję przy krawędzi bez przedwczesnego obcinania tekstu.
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Zegar, nie kalendarz — Schedule był zaimportowany, ale nieużywany.
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = com.example.ui.theme.AccentRust, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                var expanded by remember { mutableStateOf(false) }
                // "Jednorazowe" (nie "Brak") — dokładne słowo z designu/DetailScreen.kt.
                val options = listOf("NONE" to "Jednorazowe", "WEEKLY" to "Co tydzień", "MONTHLY" to "Co miesiąc", "YEARLY" to "Co rok")
                val selectedOption = options.find { it.first == eventDetails.recurrence }?.second ?: "Jednorazowe"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        readOnly = true,
                        // Pole zamknięte pokazuje "Powtarzalność: <wartość>" (dokładnie jak w
                        // designzie) — lista rozwijana niżej pokazuje same wartości bez prefiksu.
                        value = "Powtarzalność: $selectedOption",
                        onValueChange = { },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
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
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    // Wymusza tę samą wysokość na polu liczby i kontrolce Dni/Godz./Min. —
                    // domyślna wysokość TextField (56dp) i "wyliczona z paddingu" wysokość
                    // pigułek różniły się, więc obie kolumny rozciągają się teraz do
                    // wysokości najwyższego elementu zamiast zgadywać dokładny padding.
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
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
                        modifier = Modifier.width(64.dp).fillMaxHeight(),
                        textStyle = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = inputColors,
                        shape = inputShape,
                        placeholder = {
                            Text(
                                "0",
                                color = com.example.ui.theme.FormFieldPlaceholder,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )

                    // Kontrolka Dni/Godz./Min. — tło i zaokrąglenie takie samo jak reszta pól
                    // formularza (FormFieldBg, 18dp), NIE biała pigułka z szarą obwódką jak
                    // poprzednio; flex:1 na kontenerze i każdym segmencie, jak w designie.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(com.example.ui.theme.FormFieldBg, inputShape)
                            .padding(3.dp)
                    ) {
                        val units = listOf("Dni", "Godz.", "Min.")
                        val activeUnit = if (reminderUnit == "Godziny") "Godz." else if (reminderUnit == "Minuty") "Min." else "Dni"

                        units.forEach { unit ->
                            val isSelected = activeUnit == unit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(if (isSelected) com.example.ui.theme.AccentOrange else Color.Transparent)
                                    .clickable {
                                        if (unit == "Dni") reminderUnit = "Dni"
                                        else if (unit == "Godz.") reminderUnit = "Godziny"
                                        else if (unit == "Min.") reminderUnit = "Minuty"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit,
                                    color = if (isSelected) Color.White else com.example.ui.theme.OnSurfaceVariant,
                                    style = TextStyle(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        fontFamily = com.example.ui.theme.QuicksandFontFamily
                                    )
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
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NOTATKA", style = labelStyle)
                TextField(
                    value = eventDetails.note,
                    onValueChange = { onValueChange(eventDetails.copy(note = it)) },
                    placeholder = { Text("Notatka (opcjonalnie)", color = com.example.ui.theme.FormFieldPlaceholder) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    colors = inputColors,
                    shape = inputShape,
                    maxLines = 5
                )
            }

            // WYGLĄD: połączone dawne "TŁO WYDARZENIA" + "WYBIERZ MOTYW" w jedną sekcję,
            // jak w designie — jedna etykieta, podtytuł wyjaśniający, obrysowany (nie
            // wypełniony) przycisk zdjęcia z ikoną, potem pasek motywów.
            Column {
                Text("WYGLĄD", style = labelStyle)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, com.example.ui.theme.AccentRust.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.AccentRust),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (eventDetails.imageUri.isNotBlank()) "Zmień zdjęcie" else "Wybierz zdjęcie",
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                fontFamily = com.example.ui.theme.QuicksandFontFamily
                            )
                        )
                    }
                    if (eventDetails.imageUri.isNotBlank()) {
                        TextButton(onClick = { onValueChange(eventDetails.copy(imageUri = "")) }) {
                            Text("Usuń", color = com.example.ui.theme.AccentRust)
                        }
                    }
                }

                if (eventDetails.imageUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(14.dp))

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
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp, 78.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(themeConfig.backgroundColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) com.example.ui.theme.AccentOrange else Color(0x33000000),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("12", color = themeConfig.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = themeConfig.fontFamily)
                                        Text("DNI", color = themeConfig.labelColor, fontSize = 8.sp, fontFamily = themeConfig.fontFamily)
                                    }
                                }
                                // Odznaka wyboru — mały pomarańczowy krążek z ptaszkiem w rogu,
                                // dokładnie jak w designie (zamiast/obok samej obwódki).
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(com.example.ui.theme.AccentOrange),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when(themeConfig.name) {
                                    "Classic" -> "Klasyczny"
                                    "Elegant" -> "Elegancki"
                                    "Warm" -> "Ciepły"
                                    "Night" -> "Nocny"
                                    else -> "Minimalistyczny"
                                },
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    fontFamily = com.example.ui.theme.QuicksandFontFamily
                                ),
                                color = if (isSelected) com.example.ui.theme.AccentOrange else com.example.ui.theme.TextLight
                            )
                        }
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
                // Pełna pigułka jak w designie (border-radius:100) — RoundedCornerShape(50)
                // liczy promień jako % krótszego wymiaru, więc daje stadion, nie elipsę
                // (to dałby CircleShape na szerokim, niskim przycisku).
                shape = RoundedCornerShape(50),
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
                        letterSpacing = 0.5.sp,
                        fontFamily = com.example.ui.theme.QuicksandFontFamily
                    )
                )
            }
        }
    }
}
