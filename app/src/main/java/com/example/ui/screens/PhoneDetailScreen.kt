package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.data.entity.PhoneField
import com.example.model.SmartphoneSpecDef
import com.example.queue.PasteQueueManager
import com.example.ui.SpecViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneDetailScreen(
    phoneId: Long,
    viewModel: SpecViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val phone by viewModel.getPhoneDetails(phoneId).collectAsState(initial = null)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("ALL") }
    var showRawTsvDialog by remember { mutableStateOf(false) }
    var fieldToEdit by remember { mutableStateOf<PhoneField?>(null) }

    val fields by if (selectedCategory == "ALL") {
        viewModel.getFieldsForPhone(phoneId).collectAsState(initial = emptyList())
    } else {
        viewModel.getFieldsByCategory(phoneId, selectedCategory).collectAsState(initial = emptyList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = phone?.model ?: "Phone Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = phone?.brand.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val p = phone ?: return@IconButton
                            viewModel.togglePhoneFavorite(p.id, !p.isFavorite)
                        }
                    ) {
                        Icon(
                            imageVector = if (phone?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (phone?.isFavorite == true) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { showRawTsvDialog = true }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "View Raw TSV")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val curPhone = phone
            if (curPhone == null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Smartphone record not found or was deleted.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Back to Phones Database")
                        }
                    }
                }
                return@Scaffold
            }

            // Overview Header Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = curPhone.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Brand: ${curPhone.brand} • Model: ${curPhone.model}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(curPhone.originalTsv))
                                    Toast.makeText(context, "Full TSV row copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy TSV", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val fieldPairs = fields.map { it.fieldName to it.fieldValue }
                                    PasteQueueManager.addMultiple(fieldPairs, sourcePhone = curPhone.model)
                                    Toast.makeText(context, "Added ${fields.size} specs to Keyboard Paste Queue", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Queue All", fontSize = 12.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ready in IME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Category Tab Row
            ScrollableTabRow(
                selectedTabIndex = SmartphoneSpecDef.CATEGORIES.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                divider = {}
            ) {
                SmartphoneSpecDef.CATEGORIES.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Fields List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(fields, key = { it.id }) { field ->
                    SpecFieldItemCard(
                        field = field,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(field.fieldValue))
                            Toast.makeText(context, "Copied \"${field.fieldValue}\"", Toast.LENGTH_SHORT).show()
                        },
                        onAddToQueue = {
                            PasteQueueManager.addItem(
                                value = field.fieldValue,
                                label = field.fieldName,
                                sourcePhone = curPhone.model
                            )
                            Toast.makeText(context, "Added to Keyboard Paste Queue", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = { fieldToEdit = field },
                        onToggleFavorite = { viewModel.toggleFieldFavorite(field.id, !field.isFavorite) }
                    )
                }
            }
        }
    }

    // Edit Field Dialog
    fieldToEdit?.let { field ->
        var editVal by remember { mutableStateOf(field.fieldValue) }
        AlertDialog(
            onDismissRequest = { fieldToEdit = null },
            title = { Text("Edit ${field.fieldName}") },
            text = {
                OutlinedTextField(
                    value = editVal,
                    onValueChange = { editVal = it },
                    label = { Text(field.fieldName) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateField(field.copy(fieldValue = editVal))
                        fieldToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { fieldToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Raw TSV Dialog
    if (showRawTsvDialog && phone != null) {
        val rawTsv = phone!!.originalTsv
        AlertDialog(
            onDismissRequest = { showRawTsvDialog = false },
            title = { Text("Original TSV") },
            text = {
                Column {
                    Text("Preserved verbatim representation:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = rawTsv.ifBlank { "No raw TSV recorded for this phone." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(rawTsv))
                        Toast.makeText(context, "Copied original TSV", Toast.LENGTH_SHORT).show()
                        showRawTsvDialog = false
                    }
                ) {
                    Text("Copy Raw TSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRawTsvDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SpecFieldItemCard(
    field: PhoneField,
    onCopy: () -> Unit,
    onAddToQueue: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = field.fieldName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(SmartphoneSpecDef.getCategoryColor(field.category))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(SmartphoneSpecDef.getCategoryColor(field.category)).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = field.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(SmartphoneSpecDef.getCategoryColor(field.category)),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = field.fieldValue.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite Toggle
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (field.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Spec",
                        tint = if (field.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add to Paste Queue
                IconButton(onClick = onAddToQueue, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Queue Spec",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Edit Button
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Value",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Copy Button
                Button(
                    onClick = onCopy,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
