package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ModuleItem(
    val title: String,
    val subtitle: String,
    val destination: String,
    val icon: ImageVector
)

@Composable
fun HomeScreen(
    fileCount: Int,
    activityCount: Int,
    alarmCount: Int,
    onNavigate: (String) -> Unit
) {
    val modules = listOf(
        ModuleItem("PDF & Documents", "Manage, preview and manipulate PDFs", "Files", Icons.Default.Description),
        ModuleItem("Photos & Screenshots", "Organized images and screenshots", "Photos", Icons.Default.PhotoLibrary),
        ModuleItem("Document Scanner", "Multi-page camera scan to PDF", "Scanner", Icons.Default.DocumentScanner),
        ModuleItem("Calculator", "Decimal128 precision math engine", "Calculator", Icons.Default.Calculate),
        ModuleItem("Calendar", "Schedule, agenda & appointments", "Calendar", Icons.Default.CalendarMonth),
        ModuleItem("Attendance & Tasks", "Daily attendance, tasks & completion", "Tasks", Icons.Default.CheckCircle),
        ModuleItem("Alarm & Reminders", "Exact offline alarms with snooze", "Alarms", Icons.Default.Alarm),
        ModuleItem("Favorites", "Quick-starred important items", "Favorites", Icons.Default.Star),
        ModuleItem("Folders", "Directory organization structure", "Folders", Icons.Default.Folder),
        ModuleItem("Recent Files", "Recently opened documents", "Recent", Icons.Default.History),
        ModuleItem("Settings & Security", "Biometrics, theme & storage", "Settings", Icons.Default.Settings)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            // Hero card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Shield",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "OFFLINE VAULT ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Private. Offline. Yours.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your private document safe, high-precision tools, calendar, and offline alarms all completely on-device.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadge(label = "Files", value = "$fileCount")
                        StatBadge(label = "Activities", value = "$activityCount")
                        StatBadge(label = "Alarms", value = "$alarmCount")
                    }
                }
            }
        }

        item {
            Text(
                text = "Workspace Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        items(modules.size) { index ->
            val mod = modules[index]
            ElevatedCard(
                onClick = { onNavigate(mod.destination) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("module_${mod.destination.lowercase()}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = mod.icon,
                            contentDescription = mod.title,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mod.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = mod.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
