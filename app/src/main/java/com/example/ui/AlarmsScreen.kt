package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.Alarms
import com.example.Item
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AlarmsScreen(
    alarms: List<Item>,
    isExactEnabled: Boolean,
    is24Hour: Boolean,
    onRequestExact: () -> Unit,
    onAddAlarm: () -> Unit,
    onToggleAlarm: (Item, Boolean) -> Unit,
    onEditAlarm: (Item) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("alarms_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            if (!isExactEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exact Alarms Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Exact alarm permissions are required by Android for punctual ringing and alarm audio playback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestExact,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("enable_exact_alarm_btn")
                        ) {
                            Text("Grant exact alarm permission")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Local Offline Engine",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Alarms run entirely on your phone via AlarmManager. Restored automatically upon reboot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = "Configured Alarms (${alarms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onAddAlarm,
                    modifier = Modifier.testTag("add_alarm_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Alarm")
                }
            }
        }

        if (alarms.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No alarms configured yet. Tap 'Add Alarm' to schedule one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(alarms, key = { it.id }) { item ->
                val next = Alarms.next(item.data)
                val format = if (is24Hour) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd h:mm a"
                val whenText = next?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(format))
                } ?: "No upcoming trigger"

                val name = item.data.optString("name", "Alarm")
                val enabled = item.data.optBoolean("enabled", true)
                val days = item.data.optString("days")
                val interval = item.data.optInt("interval", 0)

                val repeatInfo = when {
                    days.isNotBlank() -> "Days: $days"
                    interval > 0 -> "Every $interval day(s)"
                    else -> "One-time"
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditAlarm(item) }
                        .testTag("alarm_${item.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = "Next: $whenText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = repeatInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enabled,
                                onCheckedChange = { checked -> onToggleAlarm(item, checked) },
                                modifier = Modifier.testTag("switch_${item.id}")
                            )
                        }
                    )
                }
            }
        }
    }
}
