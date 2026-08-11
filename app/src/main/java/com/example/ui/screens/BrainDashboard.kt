package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BrainDashboard(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val memoryCount by viewModel.memoryCount.collectAsState()
    val researchCount by viewModel.researchCount.collectAsState()
    val aliasesCount by viewModel.aliasesCount.collectAsState()
    val preferencesCount by viewModel.preferencesCount.collectAsState()
    val correctionsCount by viewModel.correctionsCount.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "JARVIS BRAIN DASHBOARD",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyanGlow,
            letterSpacing = 1.5.sp
        )

        // Local Model Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Model",
                        tint = CyanGlow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE AI MODEL",
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "JARVIS Local Rule & Context Engine",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = "• 100% On-Device Execution\n• Zero External APIs / Keys\n• Multi-Turn Context Resolution\n• Automatic Memory Persistence",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }
        }

        Text(
            text = "PERSISTENT KNOWLEDGE METRICS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        // REAL Statistics Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    title = "Memories",
                    count = memoryCount.toString(),
                    subtitle = "Stored items",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    title = "Preferences",
                    count = preferencesCount.toString(),
                    subtitle = "User habits",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    title = "Aliases",
                    count = aliasesCount.toString(),
                    subtitle = "Custom mappings",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    title = "Corrections",
                    count = correctionsCount.toString(),
                    subtitle = "Learned fixes",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    title = "Research Topics",
                    count = researchCount.toString(),
                    subtitle = "Cached facts",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    title = "Learned Concepts",
                    count = (memoryCount + researchCount).toString(),
                    subtitle = "Total knowledge",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "BRAIN MANAGEMENT & BACKUP",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val json = viewModel.exportBrainJson()
                    clipboardManager.setText(AnnotatedString(json))
                    Toast.makeText(context, "Brain exported to clipboard!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = CyanGlow)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Brain", color = TextPrimary, fontSize = 12.sp)
            }

            Button(
                onClick = { showImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = AmberGlow)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import Brain", color = TextPrimary, fontSize = 12.sp)
            }
        }

        OutlinedButton(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Learned Data")
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Learned Data?") },
            text = { Text("This will permanently remove all learned preferences, aliases, corrections, and research memories. Core JARVIS capabilities will remain intact.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetLearnedData()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Brain JSON") },
            text = {
                Column {
                    Text("Paste your exported JARVIS Brain JSON structure below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            viewModel.importBrainJson(importText)
                            importText = ""
                            showImportDialog = false
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatBox(
    title: String,
    count: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = JarvisSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, JarvisCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontSize = 24.sp, color = CyanGlow, fontWeight = FontWeight.ExtraBold)
            Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}
