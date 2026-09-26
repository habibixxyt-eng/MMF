package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.Alarms
import com.example.Item
import com.example.PdfTools
import com.example.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun EditDialog(
    item: Item,
    store: Store,
    defaultSound: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit
) {
    val context = LocalContext.current
    var name by remember(item) { mutableStateOf(item.data.optString("name")) }
    var notes by remember(item) { mutableStateOf(item.data.optString("notes")) }

    val initialTime = Instant.ofEpochMilli(
        item.data.optLong("when", System.currentTimeMillis() + 3600000L)
    ).atZone(ZoneId.systemDefault())

    var date by remember(item) {
        mutableStateOf(item.data.optString("date", initialTime.toLocalDate().toString()))
    }
    var time by remember(item) {
        mutableStateOf(item.data.optString("time", initialTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))))
    }
    var days by remember(item) { mutableStateOf(item.data.optString("days")) }
    var interval by remember(item) { mutableStateOf(item.data.optInt("interval", 0).toString()) }
    var minutes by remember(item) { mutableStateOf(item.data.optString("reminder", "")) }
    var type by remember(item) { mutableStateOf(item.data.optString("type", "Task")) }
    var destination by remember(item) { mutableStateOf(item.data.optString("folder")) }

    var folders by remember { mutableStateOf(emptyList<Item>()) }
    var selectedSound by remember(item) {
        mutableStateOf(item.data.optString("sound", defaultSound))
    }
    var sounds by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var showSounds by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val scheduled = item.kind in listOf("activity", "alarm")

    LaunchedEffect(item) {
        folders = withContext(Dispatchers.IO) { store.all("folder") }
        if (item.kind == "alarm") {
            sounds = withContext(Dispatchers.IO) {
                val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
                val result = mutableListOf<Pair<String, String>>()
                manager.cursor.use { c ->
                    while (c.moveToNext()) {
                        result.add(
                            c.getString(RingtoneManager.TITLE_COLUMN_INDEX) to manager.getRingtoneUri(c.position).toString()
                        )
                    }
                }
                result
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(
                if (item.id.isEmpty()) "New ${item.kind.replaceFirstChar { it.uppercase() }}"
                else "Edit ${item.kind.replaceFirstChar { it.uppercase() }}"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (item.kind == "file") {
                    Text("Folder Location:", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { destination = "" }) {
                        Text(if (destination.isEmpty()) "✓ Root Vault" else "Root Vault")
                    }
                    folders.forEach { f ->
                        TextButton(onClick = { destination = f.id }) {
                            Text((if (destination == f.id) "✓ " else "") + f.data.optString("name"))
                        }
                    }
                }

                if (scheduled) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:mm 24-hr)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes & Details") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (item.kind == "activity") {
                        Text("Activity Type:", style = MaterialTheme.typography.labelLarge)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("Task", "Attendance", "Event", "Appointment", "Note").forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    label = { Text(t) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = { minutes = it },
                            label = { Text("Reminder offset (minutes before, blank = none)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "Repeat days (1=Mon .. 7=Sun). e.g. 1,2,3,4,5 for weekdays:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = days,
                            onValueChange = { days = it },
                            label = { Text("Weekdays (e.g. 1,3,5)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = interval,
                            onValueChange = { interval = it },
                            label = { Text("Repeat every N days (0 = none)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextButton(onClick = { showSounds = !showSounds }) {
                            Text(if (showSounds) "Hide ringtones" else "Choose ringtone")
                        }
                        if (showSounds) {
                            Card(shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    TextButton(onClick = { selectedSound = ""; showSounds = false }) {
                                        Text("System Default Alarm Sound")
                                    }
                                    sounds.forEach { (title, uri) ->
                                        TextButton(onClick = { selectedSound = uri; showSounds = false }) {
                                            Text(title)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (error.isNotEmpty()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }

                if (item.id.isNotEmpty()) {
                    TextButton(
                        enabled = !busy,
                        onClick = { onDelete(item) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete permanently")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    try {
                        require(name.isNotBlank()) { "Name is required" }
                        require(name.length <= 250) { "Use 250 characters or fewer" }
                        val data = JSONObject(item.data.toString())
                            .put("name", name.trim())
                            .put("notes", notes)

                        if (item.kind == "file") {
                            data.put("folder", destination)
                        }

                        if (scheduled) {
                            val parsedDate = LocalDate.parse(date)
                            val parsedTime = LocalTime.parse(time)
                            val at = parsedDate.atTime(parsedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                            data.put("date", date)
                                .put("time", time)
                                .put("enabled", item.data.optBoolean("enabled", true))

                            if (item.kind == "alarm") {
                                val repeats = interval.toIntOrNull() ?: 0
                                require(repeats in 0..3650) { "Repeat interval must be 0–3650 days" }
                                val weekdays = if (days.isBlank()) emptyList() else days.split(',').map { it.trim().toInt() }
                                require(weekdays.all { it in 1..7 }) { "Weekdays must be 1–7" }
                                require(weekdays.isEmpty() || repeats == 0) { "Choose weekdays OR a day interval" }
                                data.put("when", at)
                                    .put("days", weekdays.distinct().joinToString(","))
                                    .put("interval", repeats)
                                    .put("sound", selectedSound)
                                require(!data.optBoolean("enabled") || Alarms.next(data) != null) {
                                    "Choose a future time for the alarm"
                                }
                            } else {
                                data.put("type", type)
                                    .put("reminder", minutes)
                                    .put("when", 0L)
                                if (minutes.isNotBlank()) {
                                    val before = minutes.toLong()
                                    require(before in 0..525600) { "Reminder must be between 0 and 525600 minutes" }
                                    val trigger = at - before * 60000L
                                    require(trigger > System.currentTimeMillis()) { "Reminder time must be in the future" }
                                    data.put("when", trigger)
                                }
                            }
                        }

                        onSave(Item(item.id.ifBlank { UUID.randomUUID().toString() }, item.kind, data))
                    } catch (e: Exception) {
                        error = e.message ?: "Please verify your input"
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PreviewDialog(
    item: Item,
    store: Store,
    onDismiss: () -> Unit,
    onOpenExternal: (Item) -> Unit
) {
    val mime = item.data.optString("mime")
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var text by remember(item.id) { mutableStateOf("") }
    var error by remember(item.id) { mutableStateOf("") }
    var page by remember(item.id) { mutableIntStateOf(0) }
    var count by remember(item.id) { mutableIntStateOf(1) }

    LaunchedEffect(item.id, page) {
        try {
            when {
                mime == "application/pdf" -> {
                    val result = withContext(Dispatchers.IO) {
                        PdfTools.count(store.file(item)) to PdfTools.preview(store.file(item), page)
                    }
                    count = result.first
                    bitmap = result.second
                }
                mime.startsWith("image/") -> {
                    bitmap = withContext(Dispatchers.IO) {
                        val file = store.file(item)
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.path, opts)
                        var sample = 1
                        while (maxOf(opts.outWidth, opts.outHeight) / sample > 1800) sample *= 2
                        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                    }
                }
                mime.startsWith("text/") -> {
                    text = withContext(Dispatchers.IO) {
                        store.file(item).bufferedReader().use { reader ->
                            val chars = CharArray(100000)
                            val n = reader.read(chars)
                            if (n < 0) "" else String(chars, 0, n)
                        }
                    }
                }
                else -> {
                    text = "This format requires an installed document viewer. Opening externally allows viewing in a dedicated application."
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Cannot load preview"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.data.optString("name"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Document preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    )
                }

                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = if (mime.startsWith("text/")) FontFamily.Monospace else FontFamily.Default,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error.isNotEmpty()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }

                if (mime == "application/pdf") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(enabled = page > 0, onClick = { page-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                        }
                        Text("Page ${page + 1} of $count", style = MaterialTheme.typography.labelLarge)
                        IconButton(enabled = page < count - 1, onClick = { page++ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = { onOpenExternal(item) }) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open externally")
            }
        }
    )
}

@Composable
fun PdfDialog(
    item: Item,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApply: (operation: String, pages: String, text: String, x: Float, y: Float, outputName: String) -> Unit
) {
    var operation by remember { mutableStateOf("Extract / reorder") }
    var pages by remember { mutableStateOf("1") }
    var text by remember { mutableStateOf("") }
    var x by remember { mutableStateOf("40") }
    var y by remember { mutableStateOf("700") }
    var outputName by remember { mutableStateOf("Edited - " + item.data.optString("name")) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("PDF Operations") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "A new copy will be generated in your vault; your original document remains preserved.",
                    style = MaterialTheme.typography.bodySmall
                )

                listOf(
                    "Extract / reorder",
                    "Rotate 90°",
                    "Delete pages",
                    "Split into individual pages",
                    "Add text"
                ).forEach { op ->
                    FilterChip(
                        selected = operation == op,
                        onClick = { operation = op },
                        label = { Text(op) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (operation != "Split into individual pages") {
                    OutlinedTextField(
                        value = pages,
                        onValueChange = { pages = it },
                        label = { Text(if (operation == "Add text") "Page Number" else "Pages (e.g. 1, 3, 5-7)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (operation == "Add text") {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Text string to add") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = x,
                        onValueChange = { x = it },
                        label = { Text("X position (PDF points from left)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = y,
                        onValueChange = { y = it },
                        label = { Text("Y position (PDF points from bottom)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Output PDF filename") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    try {
                        require(outputName.isNotBlank()) { "Output filename required" }
                        val px = x.toFloatOrNull() ?: 40f
                        val py = y.toFloatOrNull() ?: 700f
                        onApply(operation, pages, text, px, py, outputName)
                    } catch (e: Exception) {
                        error = e.message ?: "Operation failed"
                    }
                }
            ) {
                Text("Process & Save PDF")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SearchScreen(
    query: String,
    store: Store,
    onOpenItem: (Item) -> Unit
) {
    var records by remember { mutableStateOf(emptyList<Item>()) }

    LaunchedEffect(query) {
        records = withContext(Dispatchers.IO) {
            store.all().filter { item ->
                item.kind in listOf("file", "folder", "activity", "alarm") &&
                    (item.data.optString("name") + " " + item.data.optString("notes")).contains(query, true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_results"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "${records.size} match(es) found for '$query'",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(records, key = { it.id }) { item ->
            ListItem(
                headlineContent = { Text(item.data.optString("name"), fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("${item.kind.replaceFirstChar { it.uppercase() }} • ${item.data.optString("notes")}") },
                leadingContent = {
                    Icon(
                        imageVector = when (item.kind) {
                            "file" -> Icons.Default.Description
                            "folder" -> Icons.Default.Folder
                            "activity" -> Icons.Default.Event
                            "alarm" -> Icons.Default.Alarm
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { onOpenItem(item) }
            )
            HorizontalDivider()
        }
    }
}
