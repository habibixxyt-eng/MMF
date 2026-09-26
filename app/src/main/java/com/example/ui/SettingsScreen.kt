package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    theme: String,
    locked: Boolean,
    is24Hour: Boolean,
    calendarView: String,
    filesCount: Int,
    storageKb: Long,
    onThemeChange: (String) -> Unit,
    onLockToggle: (Boolean) -> Unit,
    onClock24Toggle: (Boolean) -> Unit,
    onCalendarViewChange: (String) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationChannels: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onPickAlarmSound: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 100.dp)
            .testTag("settings_screen")
    ) {
        // Appearance
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("System", "Light", "Dark").forEach { value ->
                FilterChip(
                    selected = theme == value,
                    onClick = { onThemeChange(value) },
                    label = { Text(value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security
        Text(
            text = "App Lock & Security",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Require Biometric / PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Prompt device biometrics or lock screen credential before opening vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = locked,
                        onCheckedChange = onLockToggle,
                        modifier = Modifier.testTag("lock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FLAG_SECURE is active: screenshots and task-switcher previews are permanently blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Default Calendar View
        Text(
            text = "Calendar Preferences",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Default calendar view mode:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Day", "Week", "Month").forEach { view ->
                FilterChip(
                    selected = calendarView == view,
                    onClick = { onCalendarViewChange(view) },
                    label = { Text(view) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Alarm & Notifications
        Text(
            text = "Alarms & Reminders",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "24-Hour Time Format",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = is24Hour, onCheckedChange = onClock24Toggle)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRequestNotifications,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Notification Permission")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onRequestExactAlarm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exact-Alarm Permission Settings")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onPickAlarmSound,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Default Alarm Ringtone")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("App Battery & Background Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Storage & Privacy
        Text(
            text = "Storage & Privacy",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Vault Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$filesCount files in sandbox • $storageKb KB stored locally",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 100% offline: no internet permission is requested or used.\n" +
                           "• Cloud backup is disabled to prevent leakage of private vault files.\n" +
                           "• Export documents individually at any time via Files → More → Export a copy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About
        Text(
            text = "About MMF Formula",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "MMF Formula v1.0 • Native Kotlin / Jetpack Compose • High-performance on-device private productivity suite.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
