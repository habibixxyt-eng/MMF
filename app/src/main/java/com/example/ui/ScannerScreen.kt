package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScannerScreen(
    scansCount: Int,
    busy: Boolean,
    onCapture: () -> Unit,
    onSavePdf: () -> Unit,
    onClearSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("scanner_screen")
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Document Scanner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Capture multi-page documents privately using your camera and compile directly into PDF format.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCapture,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("capture_page_btn")
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Capture document page")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Scan Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$scansCount page(s) captured in this session. All originals remain in your private vault.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSavePdf,
            enabled = scansCount > 0 && !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_scans_pdf_btn")
        ) {
            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save captured pages as PDF")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onClearSession,
            enabled = scansCount > 0 && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start new scan session")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Workflow Tips:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "• Capture pages sequentially in portrait or landscape.\n" +
                   "• When finished, tap 'Save captured pages as PDF' to generate an optimized PDF file in your vault.\n" +
                   "• You can also reorder, rotate, or delete pages anytime using the PDF tools in Files.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
