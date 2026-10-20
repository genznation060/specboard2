package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ClipboardItem
import com.example.parser.TsvParser
import com.example.ui.SpecViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClipboardScreen(
    viewModel: SpecViewModel,
    onOpenImportWithText: (String) -> Unit
) {
    val clipboardItems by viewModel.allClipboard.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var filterMode by remember { mutableStateOf("ALL") } // ALL, TSV, FAVORITES
    var clipSearchQuery by remember { mutableStateOf("") }
    var itemToView by remember { mutableStateOf<ClipboardItem?>(null) }
    var showConfirmClearClipboard by remember { mutableStateOf(false) }

    fun readCurrentSystemClipboard() {
        val clip = clipboardManager.getText()?.text.orEmpty()
        if (clip.isNotBlank()) {
            val isTsv = TsvParser.isLikelyTsv(clip)
            viewModel.addClipboardText(clip)
            if (isTsv) {
                Toast.makeText(context, "Copied TSV detected and added to history!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Copied text added to clipboard history", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "System clipboard is currently empty", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredList = remember(clipboardItems, filterMode, clipSearchQuery) {
        clipboardItems.filter { item ->
            val matchesFilter = when (filterMode) {
                "TSV" -> item.isTsv
                "FAVORITES" -> item.isFavorite
                else -> true
            }
            val matchesSearch = if (clipSearchQuery.isBlank()) true else {
                item.content.contains(clipSearchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Action Card: "Read System Clipboard"
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clipboard Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auto-detects smartphone TSV from Excel, Sheets & Web",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { readCurrentSystemClipboard() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_paste_from_clipboard")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Clip", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = clipSearchQuery,
                        onValueChange = { clipSearchQuery = it },
                        placeholder = { Text("Search clipboard history...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (clipSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { clipSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = filterMode == "ALL",
                                    onClick = { filterMode = "ALL" },
                                    label = { Text("All (${clipboardItems.size})", fontSize = 11.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterMode == "TSV",
                                    onClick = { filterMode = "TSV" },
                                    label = {
                                        Text(
                                            "TSV Specs (${clipboardItems.count { it.isTsv }})",
                                            fontSize = 11.sp,
                                            fontWeight = if (filterMode == "TSV") FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = filterMode == "FAVORITES",
                                    onClick = { filterMode = "FAVORITES" },
                                    label = { Text("Favorites", fontSize = 11.sp) }
                                )
                            }
                        }

                        if (clipboardItems.isNotEmpty()) {
                            TextButton(onClick = { showConfirmClearClipboard = true }) {
                                Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Clipboard History List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (clipSearchQuery.isNotEmpty()) "No matching clipboard items" else "No clipboard history yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Copy smartphone TSV or text from any app, then tap 'Paste Clip'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        ClipboardItemCard(
                            item = item,
                            onClick = { itemToView = item },
                            onImportTsv = {
                                onOpenImportWithText(item.content)
                            },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(item.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, item.content)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Clipboard Item"))
                            },
                            onToggleFavorite = {
                                viewModel.toggleClipboardFavorite(item.id, !item.isFavorite)
                            },
                            onTogglePinned = {
                                viewModel.toggleClipboardPinned(item.id, !item.isPinned)
                            },
                            onDelete = {
                                viewModel.deleteClipboardItem(item.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail / Preview Dialog
    itemToView?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToView = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isTsv) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Smartphone TSV Item")
                    } else {
                        Text("Clipboard Item")
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(item.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = item.content,
                            fontFamily = if (item.isTsv) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (item.isTsv) {
                    Button(
                        onClick = {
                            val text = item.content
                            itemToView = null
                            onOpenImportWithText(text)
                        }
                    ) {
                        Text("Parse & Add to Phone DB")
                    }
                } else {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(item.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            itemToView = null
                        }
                    ) {
                        Text("Copy")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToView = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Confirm Clear Clipboard Dialog
    if (showConfirmClearClipboard) {
        AlertDialog(
            onDismissRequest = { showConfirmClearClipboard = false },
            title = { Text("Clear Clipboard History") },
            text = { Text("Are you sure you want to clear all recorded clipboard items? This does not delete any phone database records.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearClipboard()
                        showConfirmClearClipboard = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearClipboard = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ClipboardItemCard(
    item: ClipboardItem,
    onClick: () -> Unit,
    onImportTsv: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.createdAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.createdAt))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isTsv) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isTsv) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Text(
                                text = "TSV DETECTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (item.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onTogglePinned, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (item.isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.content.trim(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (item.isTsv) FontFamily.Monospace else FontFamily.Default
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isTsv) {
                    Button(
                        onClick = onImportTsv,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Import into Phone DB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
