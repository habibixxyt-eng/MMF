package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.Item
import java.time.LocalDate

@Composable
fun CalendarScreen(
    screen: String,
    records: List<Item>,
    defaultView: String,
    onEditItem: (Item) -> Unit,
    onToggleDone: (Item, Boolean) -> Unit,
    onAddActivity: (LocalDate, String) -> Unit
) {
    var day by remember { mutableStateOf(LocalDate.now()) }
    var mode by remember { mutableStateOf(defaultView) }
    var filter by remember { mutableStateOf("All") }

    val taskMode = screen == "Tasks"

    val shown = remember(records, taskMode, mode, day, filter) {
        records.filter { item ->
            val date = runCatching { LocalDate.parse(item.data.optString("date")) }.getOrNull()
            val type = item.data.optString("type")
            val completed = item.data.optBoolean("done")

            val matchesDateOrType = if (taskMode) {
                type == "Task" || type == "Attendance"
            } else {
                when (mode) {
                    "Day" -> date == day
                    "Week" -> {
                        val start = day.minusDays((day.dayOfWeek.value - 1).toLong())
                        date != null && !date.isBefore(start) && date.isBefore(start.plusDays(7))
                    }
                    else -> date?.month == day.month && date?.year == day.year
                }
            }

            val matchesFilter = when (filter) {
                "Completed" -> completed
                "Pending" -> !completed
                else -> true
            }

            matchesDateOrType && matchesFilter
        }.sortedBy { it.data.optString("date") + it.data.optString("time") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // View Mode selection
            if (!taskMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Day", "Week", "Month").forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m) }
                        )
                    }
                }

                // Date Navigator
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { day = day.minusYears(1) }) { Text("-1Y") }
                        IconButton(onClick = {
                            day = when (mode) {
                                "Day" -> day.minusDays(1)
                                "Week" -> day.minusWeeks(1)
                                else -> day.minusMonths(1)
                            }
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                        }

                        TextButton(onClick = { day = LocalDate.now() }) {
                            Text(
                                text = day.toString(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = {
                            day = when (mode) {
                                "Day" -> day.plusDays(1)
                                "Week" -> day.plusWeeks(1)
                                else -> day.plusMonths(1)
                            }
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                        TextButton(onClick = { day = day.plusYears(1) }) { Text("+1Y") }
                    }
                }

                // Month Grid
                if (mode == "Month") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Weekday headers
                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                                    Text(
                                        text = label,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val first = day.withDayOfMonth(1)
                            val offset = first.dayOfWeek.value - 1
                            val totalDays = day.lengthOfMonth()
                            val rows = (offset + totalDays + 6) / 7

                            repeat(rows) { r ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    repeat(7) { c ->
                                        val dayNumber = r * 7 + c - offset + 1
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (dayNumber in 1..totalDays) {
                                                val date = first.withDayOfMonth(dayNumber)
                                                val isToday = date == LocalDate.now()
                                                val isSelected = date == day

                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable {
                                                            day = date
                                                            mode = "Day"
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayNumber.toString(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Completed", "Pending").forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (taskMode) "All Tasks & Attendance (${shown.size})" else "Activities on $day (${shown.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        onAddActivity(day, if (taskMode) "Attendance" else "Task")
                    },
                    modifier = Modifier.testTag("add_activity_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (taskMode) "Add Attendance" else "Add Activity")
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activities recorded for this selection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(shown, key = { it.id }) { item ->
                val done = item.data.optBoolean("done")
                val name = item.data.optString("name")
                val date = item.data.optString("date")
                val time = item.data.optString("time")
                val type = item.data.optString("type")
                val notes = item.data.optString("notes")

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditItem(item) }
                        .testTag("activity_${item.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Column {
                                Text("$date $time • $type", style = MaterialTheme.typography.bodySmall)
                                if (notes.isNotBlank()) {
                                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        leadingContent = {
                            Checkbox(
                                checked = done,
                                onCheckedChange = { checked -> onToggleDone(item, checked) }
                            )
                        },
                        trailingContent = {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(type) }
                            )
                        }
                    )
                }
            }
        }
    }
}
