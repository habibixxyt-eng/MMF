package com.example

import android.Manifest
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ui.*
import com.example.ui.theme.MMFTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MainActivity : FragmentActivity() {
    private lateinit var store: Store
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    private var version by mutableIntStateOf(0)
    private var busy by mutableStateOf(false)
    private var unlocked by mutableStateOf(false)
    private var screen by mutableStateOf("Home")
    private var theme by mutableStateOf("System")
    private var query by mutableStateOf("")
    private var folder by mutableStateOf("")
    private var editing by mutableStateOf<Item?>(null)
    private var preview by mutableStateOf<Item?>(null)
    private var pdfEditing by mutableStateOf<Item?>(null)
    private var deleting by mutableStateOf<Item?>(null)
    private var fileMenu by mutableStateOf<Item?>(null)
    private var quick by mutableStateOf(false)
    private var exporting by mutableStateOf<Item?>(null)
    private var captureFile: File? = null
    private var captureFolder = ""
    private val scans = mutableStateListOf<String>()

    private val noticeLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message(if (granted) "Notifications enabled" else "Notifications disabled; reminders may not trigger alerts")
    }

    private val importerLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val destination = folder
        work {
            uris.forEach { store.importUri(it, destination) }
        }
    }

    private val exporterLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val item = exporting
        if (uri != null && item != null) {
            work {
                val out = contentResolver.openOutputStream(uri) ?: error("Cannot write to destination")
                out.use { dst ->
                    store.file(item).inputStream().use { it.copyTo(dst) }
                }
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = captureFile
        if (ok && file != null) {
            lifecycleScope.launch {
                busy = true
                try {
                    val id = withContext(Dispatchers.IO) {
                        store.addFile(
                            file,
                            "Scan ${LocalDateTime.now().toString().replace(':', '-')}.jpg",
                            "image/jpeg",
                            captureFolder
                        )
                    }
                    scans.add(id)
                    version++
                } catch (e: Exception) {
                    message(e.message ?: "Capture failed")
                } finally {
                    file.delete()
                    captureFile = null
                    prefs.edit().remove("capture").apply()
                    busy = false
                }
            }
        } else {
            file?.delete()
            captureFile = null
            prefs.edit().remove("capture").apply()
        }
    }

    private val soundPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        @Suppress("DEPRECATION")
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            prefs.edit().putString("sound", uri.toString()).apply()
            version++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        store = Store(this)
        PDFBoxResourceLoader.init(applicationContext)
        Alarms.channels(this)

        theme = prefs.getString("theme", "System") ?: "System"
        prefs.getString("capture", null)?.let { captureFile = File(it) }
        captureFolder = prefs.getString("captureFolder", "") ?: ""

        setContent {
            val isDark = theme == "Dark" || (theme == "System" && isSystemInDarkTheme())
            MMFTheme(darkTheme = isDark) {
                AppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        unlocked = !prefs.getBoolean("lock", false)
    }

    override fun onStop() {
        if (prefs.getBoolean("lock", false)) unlocked = false
        super.onStop()
    }

    private fun message(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    private fun work(block: () -> Unit) {
        lifecycleScope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) { block() }
                version++
            } catch (e: Exception) {
                message(e.message ?: "Operation failed")
            } finally {
                busy = false
            }
        }
    }

    private fun auth(enableLockOnSuccess: Boolean = false) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            message("Set a secure screen lock or enroll biometrics in Android settings first")
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (enableLockOnSuccess) {
                        prefs.edit().putBoolean("lock", true).apply()
                    }
                    unlocked = true
                    version++
                }

                override fun onAuthenticationError(code: Int, text: CharSequence) {
                    message(text.toString())
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock MMF Formula Vault")
                .setSubtitle("Confirm your biometric or device credentials")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    private fun startCameraCapture() {
        val dir = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "capture-${UUID.randomUUID()}.jpg")
        captureFile = file
        captureFolder = folder
        prefs.edit().putString("capture", file.path).putString("captureFolder", folder).apply()

        runCatching {
            cameraLauncher.launch(FileProvider.getUriForFile(this, "$packageName.files", file))
        }.onFailure {
            message("No compatible camera app available")
        }
    }

    private fun requestExactAlarms() {
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }.onFailure {
                message("Open Android Settings → Apps → Special access → Alarms & reminders")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppRoot() {
        if (!unlocked) {
            // Lock Screen
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "MMF Formula",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your private offline workspace",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { auth() },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(50.dp)
                            .testTag("unlock_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock Vault")
                    }
                }
            }
            return
        }

        // Vault main content
        var allItems by remember { mutableStateOf(emptyList<Item>()) }
        LaunchedEffect(version) {
            allItems = withContext(Dispatchers.IO) { store.all() }
        }

        val fileCount = allItems.count { it.kind == "file" }
        val activityCount = allItems.count { it.kind == "activity" }
        val alarmCount = allItems.count { it.kind == "alarm" }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (screen == "Home") "MMF Formula" else "MMF • $screen",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (screen != "Home") {
                            IconButton(onClick = { screen = "Home"; folder = "" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { screen = "Settings" }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { quick = true },
                    modifier = Modifier.testTag("fab_quick_action")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Quick Add")
                }
            },
            bottomBar = {
                NavigationBar {
                    listOf(
                        NavigationTab("Home", Icons.Default.Home),
                        NavigationTab("Files", Icons.Default.Folder),
                        NavigationTab("Calendar", Icons.Default.CalendarMonth),
                        NavigationTab("Tasks", Icons.Default.CheckCircle)
                    ).forEach { tab ->
                        NavigationBarItem(
                            selected = screen == tab.label,
                            onClick = {
                                screen = tab.label
                                folder = ""
                                query = ""
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("nav_${tab.label.lowercase()}")
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search files, folders, activities, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("search_field"),
                    singleLine = true
                )

                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isNotBlank()) {
                        SearchScreen(
                            query = query,
                            store = store,
                            onOpenItem = { item ->
                                when (item.kind) {
                                    "file" -> {
                                        work { store.save(item.kind, item.data.put("opened", System.currentTimeMillis()), item.id) }
                                        preview = item
                                    }
                                    "folder" -> {
                                        query = ""
                                        screen = "Files"
                                        folder = item.id
                                    }
                                    else -> editing = item
                                }
                            }
                        )
                    } else {
                        when (screen) {
                            "Home" -> HomeScreen(
                                fileCount = fileCount,
                                activityCount = activityCount,
                                alarmCount = alarmCount,
                                onNavigate = { destination ->
                                    screen = destination
                                    folder = ""
                                }
                            )

                            "Files", "Photos", "Favorites", "Recent", "Folders" -> {
                                val currentFiles = allItems.filter {
                                    if (screen == "Folders") it.kind == "folder"
                                    else it.kind == "file" && when (screen) {
                                        "Favorites" -> it.data.optBoolean("favorite")
                                        "Recent" -> it.data.optLong("opened") > 0
                                        "Photos" -> it.data.optString("mime").startsWith("image/")
                                        else -> it.data.optString("folder") == folder
                                    }
                                }
                                FilesScreen(
                                    screen = screen,
                                    folder = folder,
                                    records = currentFiles,
                                    onFolderChange = { newFolder -> folder = newFolder },
                                    onOpen = { item ->
                                        work { store.save(item.kind, item.data.put("opened", System.currentTimeMillis()), item.id) }
                                        preview = item
                                    },
                                    onMenu = { item -> fileMenu = item },
                                    onMergePdfs = { selectedList ->
                                        work {
                                            require(selectedList.all { it.data.optString("mime") == "application/pdf" }) { "Select PDFs only" }
                                            val tmp = File.createTempFile("merged", ".pdf", cacheDir)
                                            try {
                                                PdfTools.merge(selectedList.map { store.file(it) }, tmp)
                                                store.addFile(tmp, "Merged_${System.currentTimeMillis()}.pdf", "application/pdf", folder)
                                            } finally {
                                                tmp.delete()
                                            }
                                        }
                                    },
                                    onImagesToPdf = { selectedImages ->
                                        work {
                                            require(selectedImages.all { it.data.optString("mime").startsWith("image/") }) { "Select images only" }
                                            val tmp = File.createTempFile("images", ".pdf", cacheDir)
                                            try {
                                                PdfTools.images(selectedImages.map { store.file(it) }, tmp)
                                                store.addFile(tmp, "Images_${System.currentTimeMillis()}.pdf", "application/pdf", folder)
                                            } finally {
                                                tmp.delete()
                                            }
                                        }
                                    },
                                    busy = busy
                                )
                            }

                            "Scanner" -> ScannerScreen(
                                scansCount = scans.size,
                                busy = busy,
                                onCapture = { startCameraCapture() },
                                onSavePdf = {
                                    val ids = scans.toList()
                                    work {
                                        val tmp = File.createTempFile("scan", ".pdf", cacheDir)
                                        try {
                                            PdfTools.images(ids.map { store.file(store.get(it) ?: error("Missing scan page")) }, tmp)
                                            store.addFile(tmp, "Scanned_${LocalDateTime.now().toString().replace(':', '-')}.pdf", "application/pdf", folder)
                                        } finally {
                                            tmp.delete()
                                        }
                                    }
                                },
                                onClearSession = { scans.clear() }
                            )

                            "Calendar", "Tasks" -> {
                                val activities = allItems.filter { it.kind == "activity" }
                                val defaultView = prefs.getString("calendarView", "Month") ?: "Month"
                                CalendarScreen(
                                    screen = screen,
                                    records = activities,
                                    defaultView = defaultView,
                                    onEditItem = { item -> editing = item },
                                    onToggleDone = { item, checked ->
                                        work {
                                            store.save(item.kind, item.data.put("done", checked), item.id)
                                        }
                                    },
                                    onAddActivity = { selectedDate, defaultType ->
                                        editing = Item(
                                            "",
                                            "activity",
                                            JSONObject()
                                                .put("date", selectedDate.toString())
                                                .put("type", defaultType)
                                        )
                                    }
                                )
                            }

                            "Alarms" -> {
                                val alarms = allItems.filter { it.kind == "alarm" }
                                val exactEnabled = Alarms.exact(this@MainActivity)
                                val is24 = prefs.getBoolean("24hour", true)
                                AlarmsScreen(
                                    alarms = alarms,
                                    isExactEnabled = exactEnabled,
                                    is24Hour = is24,
                                    onRequestExact = { requestExactAlarms() },
                                    onAddAlarm = {
                                        editing = Item("", "alarm", JSONObject())
                                    },
                                    onToggleAlarm = { item, enabled ->
                                        work {
                                            store.save(item.kind, item.data.put("enabled", enabled), item.id)
                                            Alarms.schedule(this@MainActivity, item)
                                        }
                                    },
                                    onEditAlarm = { item -> editing = item }
                                )
                            }

                            "Calculator" -> {
                                val history = allItems.filter { it.kind == "calc" }.sortedByDescending { it.data.optLong("created") }
                                CalculatorScreen(
                                    history = history,
                                    onSaveCalculation = { expr, ans ->
                                        work {
                                            store.save("calc", JSONObject().put("name", "$expr = $ans").put("created", System.currentTimeMillis()))
                                        }
                                    },
                                    onError = { message(it) }
                                )
                            }

                            "Settings" -> {
                                val is24 = prefs.getBoolean("24hour", true)
                                val lockedState = prefs.getBoolean("lock", false)
                                val calView = prefs.getString("calendarView", "Month") ?: "Month"
                                val totalStorageKb = allItems.filter { it.kind == "file" }.sumOf { store.file(it).length() } / 1024

                                SettingsScreen(
                                    theme = theme,
                                    locked = lockedState,
                                    is24Hour = is24,
                                    calendarView = calView,
                                    filesCount = fileCount,
                                    storageKb = totalStorageKb,
                                    onThemeChange = { newTheme ->
                                        theme = newTheme
                                        prefs.edit().putString("theme", newTheme).apply()
                                    },
                                    onLockToggle = { enable ->
                                        if (enable) auth(enableLockOnSuccess = true)
                                        else {
                                            prefs.edit().putBoolean("lock", false).apply()
                                            version++
                                        }
                                    },
                                    onClock24Toggle = { enabled ->
                                        prefs.edit().putBoolean("24hour", enabled).apply()
                                        version++
                                    },
                                    onCalendarViewChange = { v ->
                                        prefs.edit().putString("calendarView", v).apply()
                                        message("Default calendar view: $v")
                                        version++
                                    },
                                    onRequestNotifications = {
                                        if (Build.VERSION.SDK_INT >= 33) {
                                            noticeLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                                        }
                                    },
                                    onOpenNotificationChannels = {
                                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                                    },
                                    onRequestExactAlarm = { requestExactAlarms() },
                                    onPickAlarmSound = {
                                        soundPickerLauncher.launch(
                                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                        )
                                    },
                                    onOpenAppSettings = {
                                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Add Action Dialog
            if (quick) {
                AlertDialog(
                    onDismissRequest = { quick = false },
                    title = { Text("Quick Action") },
                    text = {
                        Column {
                            listOf(
                                "Import files" to Icons.Default.UploadFile,
                                "Capture document" to Icons.Default.CameraAlt,
                                "New folder" to Icons.Default.CreateNewFolder,
                                "New activity" to Icons.Default.Event,
                                "New alarm" to Icons.Default.Alarm
                            ).forEach { (name, icon) ->
                                TextButton(
                                    onClick = {
                                        quick = false
                                        when (name) {
                                            "Import files" -> importerLauncher.launch(
                                                arrayOf(
                                                    "application/pdf",
                                                    "image/*",
                                                    "text/*",
                                                    "application/msword",
                                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                                )
                                            )
                                            "Capture document" -> {
                                                screen = "Scanner"
                                                startCameraCapture()
                                            }
                                            "New folder" -> editing = Item("", "folder", JSONObject())
                                            "New activity" -> editing = Item(
                                                "",
                                                "activity",
                                                JSONObject().put("date", LocalDate.now().toString()).put("type", "Task")
                                            )
                                            "New alarm" -> editing = Item("", "alarm", JSONObject())
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, contentDescription = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(name)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { quick = false }) { Text("Close") }
                    }
                )
            }

            // File Item Menu
            fileMenu?.let { item ->
                AlertDialog(
                    onDismissRequest = { fileMenu = null },
                    title = { Text(item.data.optString("name")) },
                    text = {
                        Column {
                            TextButton(
                                onClick = {
                                    fileMenu = null
                                    editing = item
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (item.kind == "file") "Rename / Move to Folder" else "Rename Folder")
                            }

                            if (item.kind == "file") {
                                TextButton(
                                    onClick = {
                                        fileMenu = null
                                        work {
                                            store.save("file", item.data.put("favorite", !item.data.optBoolean("favorite")), item.id)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (item.data.optBoolean("favorite")) "Remove from favorites" else "Add to favorites")
                                }

                                TextButton(
                                    onClick = {
                                        fileMenu = null
                                        work {
                                            store.addFile(
                                                store.file(item),
                                                "Copy - " + item.data.optString("name"),
                                                item.data.optString("mime"),
                                                item.data.optString("folder")
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Duplicate / Copy")
                                }

                                TextButton(
                                    onClick = {
                                        fileMenu = null
                                        exporting = item
                                        exporterLauncher.launch(item.data.optString("name"))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Export a copy")
                                }

                                if (item.data.optString("mime") == "application/pdf") {
                                    TextButton(
                                        onClick = {
                                            fileMenu = null
                                            pdfEditing = item
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("PDF operations (Split, Merge, Rotate...)")
                                    }
                                }
                            }

                            TextButton(
                                onClick = {
                                    fileMenu = null
                                    deleting = item
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete permanently")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { fileMenu = null }) { Text("Close") }
                    }
                )
            }

            // Edit Dialog
            editing?.let { item ->
                EditDialog(
                    item = item,
                    store = store,
                    defaultSound = prefs.getString("sound", "") ?: "",
                    busy = busy,
                    onDismiss = { editing = null },
                    onSave = { savedItem ->
                        work {
                            val id = store.save(savedItem.kind, savedItem.data, savedItem.id)
                            if (savedItem.kind in listOf("alarm", "activity")) {
                                Alarms.schedule(this@MainActivity, Item(id, savedItem.kind, savedItem.data))
                            }
                        }
                        editing = null
                    },
                    onDelete = { itemToDelete ->
                        editing = null
                        deleting = itemToDelete
                    }
                )
            }

            // Preview Dialog
            preview?.let { item ->
                PreviewDialog(
                    item = item,
                    store = store,
                    onDismiss = { preview = null },
                    onOpenExternal = { itemToOpen ->
                        val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", store.file(itemToOpen))
                        runCatching {
                            startActivity(
                                Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(uri, itemToOpen.data.optString("mime"))
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            )
                        }.onFailure {
                            message("No installed app supports this file type")
                        }
                    }
                )
            }

            // PDF Operations Dialog
            pdfEditing?.let { item ->
                PdfDialog(
                    item = item,
                    busy = busy,
                    onDismiss = { pdfEditing = null },
                    onApply = { operation, pages, text, x, y, outputName ->
                        work {
                            val source = store.file(item)
                            val destination = item.data.optString("folder")
                            val temp = File.createTempFile("edited", ".pdf", cacheDir)
                            try {
                                if (operation == "Split into individual pages") {
                                    val pageTotal = PdfTools.count(source)
                                    for (n in 1..pageTotal) {
                                        PdfTools.select(source, temp, n.toString())
                                        store.addFile(temp, "${outputName.removeSuffix(".pdf")} - $n.pdf", "application/pdf", destination)
                                    }
                                } else {
                                    when (operation) {
                                        "Extract / reorder" -> PdfTools.select(source, temp, pages)
                                        "Rotate 90°" -> PdfTools.rotate(source, temp, pages)
                                        "Delete pages" -> PdfTools.delete(source, temp, pages)
                                        "Add text" -> PdfTools.text(source, temp, pages.toInt(), text, x, y)
                                    }
                                    store.addFile(
                                        temp,
                                        if (outputName.endsWith(".pdf", true)) outputName else "$outputName.pdf",
                                        "application/pdf",
                                        destination
                                    )
                                }
                            } finally {
                                temp.delete()
                            }
                        }
                        pdfEditing = null
                    }
                )
            }

            // Delete Dialog
            deleting?.let { item ->
                AlertDialog(
                    onDismissRequest = { deleting = null },
                    title = { Text("Delete permanently?") },
                    text = { Text("Are you sure you want to delete '${item.data.optString("name")}'? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                deleting = null
                                work {
                                    if (item.kind == "folder") {
                                        store.all("file").filter { it.data.optString("folder") == item.id }.forEach { child ->
                                            store.save(child.kind, child.data.put("folder", ""), child.id)
                                        }
                                    }
                                    Alarms.cancel(this@MainActivity, item.id)
                                    store.delete(item)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleting = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }

    private data class NavigationTab(val label: String, val icon: ImageVector)
}
