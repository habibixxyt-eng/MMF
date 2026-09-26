package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.Item

@Composable
fun FilesScreen(
    screen: String,
    folder: String,
    records: List<Item>,
    onFolderChange: (String) -> Unit,
    onOpen: (Item) -> Unit,
    onMenu: (Item) -> Unit,
    onMergePdfs: (List<Item>) -> Unit,
    onImagesToPdf: (List<Item>) -> Unit,
    busy: Boolean
) {
    var sort by remember { mutableStateOf("Name") }
    val selected = remember { mutableStateListOf<String>() }

    LaunchedEffect(screen, folder) {
        selected.clear()
    }

    val sorted = remember(records, sort, screen) {
        when {
            screen == "Recent" -> records.sortedByDescending { it.data.optLong("opened") }
            sort == "Date" -> records.sortedByDescending { it.data.optLong("created") }
            sort == "Size" -> records.sortedByDescending { it.data.optLong("size") }
            sort == "Type" -> records.sortedBy { it.data.optString("mime") }
            else -> records.sortedBy { it.data.optString("name").lowercase() }
        }
    }

    val selectedItems = records.filter { it.id in selected }
    val allPdf = selectedItems.isNotEmpty() && selectedItems.all { it.data.optString("mime") == "application/pdf" }
    val allImages = selectedItems.isNotEmpty() && selectedItems.all { it.data.optString("mime").startsWith("image/") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("files_screen")
    ) {
        // Sort chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Name", "Date", "Size", "Type").forEach { value ->
                FilterChip(
                    selected = sort == value,
                    onClick = { sort = value },
                    label = { Text(value) },
                    modifier = Modifier.testTag("sort_$value")
                )
            }
        }

        // Folder navigation
        if (folder.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Current Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Viewing Folder",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onFolderChange("") }) {
                        Text("Back to Root")
                    }
                }
            }
        }

        // Batch action toolbar
        if (selected.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selected.size} selected",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    if (allPdf && selected.size >= 2) {
                        Button(
                            enabled = !busy,
                            onClick = { onMergePdfs(selectedItems) },
                            modifier = Modifier.testTag("merge_pdfs_btn")
                        ) {
                            Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Merge PDFs (${selected.size})")
                        }
                    }

                    if (allImages) {
                        Button(
                            enabled = !busy,
                            onClick = { onImagesToPdf(selectedItems) },
                            modifier = Modifier.testTag("images_to_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Images to PDF")
                        }
                    }

                    OutlinedButton(onClick = { selected.clear() }) {
                        Text("Deselect all")
                    }
                }
            }
        }

        Text(
            text = "${records.size} items in vault • select items in order to merge",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No files found here",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Use the '+' button to import documents, photos, or create folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("files_list"),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sorted, key = { it.id }) { item ->
                    val isFile = item.kind == "file"
                    val mime = item.data.optString("mime")
                    val isFavorite = item.data.optBoolean("favorite")
                    val sizeKb = item.data.optLong("size") / 1024

                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.data.optString("name"),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isFavorite) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Favorite",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        supportingContent = {
                            Text(
                                text = if (isFile) {
                                    "${sizeKb} KB • $mime"
                                } else {
                                    "Folder"
                                }
                            )
                        },
                        leadingContent = {
                            if (isFile) {
                                Checkbox(
                                    checked = item.id in selected,
                                    onCheckedChange = { checked ->
                                        if (checked) selected.add(item.id) else selected.remove(item.id)
                                    },
                                    modifier = Modifier.testTag("check_${item.id}")
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onMenu(item) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Item Actions")
                            }
                        },
                        modifier = Modifier
                            .clickable {
                                if (!isFile) {
                                    onFolderChange(item.id)
                                } else {
                                    onOpen(item)
                                }
                            }
                            .testTag("item_${item.id}")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}
