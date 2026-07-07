package com.example.ui.screens

import com.example.ui.TallyViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

@Composable
fun StockProductionScreen(viewModel: TallyViewModel) {
    val items by viewModel.productionItems.collectAsStateWithLifecycle()
    val darkTheme = isSystemInDarkTheme()

    var showAddNewPanel by remember { mutableStateOf(false) }
    var newModel by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("") }
    val defaultDate = "2026-06-30"
    var newDate by remember { mutableStateOf(defaultDate) }
    var imageUrlInput by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUrlInput = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Weaving Loom Production", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            Button(
                onClick = { showAddNewPanel = !showAddNewPanel },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("view_toggle_add_production")
            ) {
                Text(if (showAddNewPanel) "CLOSE" else "+ SPIN NEW")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showAddNewPanel) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("add_production_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Spin Fresh Loom Production Lot", style = MaterialTheme.typography.titleSmall, color = RoyalCrimson)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newModel,
                        onValueChange = { newModel = it },
                        label = { Text("Saree Model Detail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newQty,
                            onValueChange = { newQty = it },
                            label = { Text("Loom Pieces") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            label = { Text("Estimated Date") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!imageUrlInput.isNullOrBlank()) {
                            AsyncImage(
                                model = imageUrlInput,
                                contentDescription = "Selected Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Pick Image", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (imageUrlInput.isNullOrBlank()) "Add Picture" else "Change Picture")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val qtyInt = newQty.toIntOrNull() ?: 10
                            if (newModel.isNotBlank() && qtyInt > 0) {
                                viewModel.addNewProductionItem(newModel, qtyInt, newDate, imageUrlInput)
                                newModel = ""
                                newQty = ""
                                newDate = defaultDate
                                imageUrlInput = null
                                showAddNewPanel = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("commit_production_button")
                    ) {
                        Text("INITIATE WEAVING LOOM", color = Color.White)
                    }
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No looming products active.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("production_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    val isCompleted = item.status == "Completed"
                    val completedBgColor = if (darkTheme) Color(0xFF1B3B2B) else Color(0xFFEFFDF5)
                    val statusColor = if (isCompleted) {
                        if (darkTheme) Color(0xFF81C784) else Color(0xFF0F5132)
                    } else {
                        if (darkTheme) Color(0xFFFFB74D) else Color(0xFF664D03)
                    }
                    val labelColor = if (darkTheme) Color.LightGray else Color.DarkGray

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("production_item_$index"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted) completedBgColor else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isCompleted) Color.Green.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = "Production Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray.copy(alpha = 0.3f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isCompleted) Color.Green else Color(0xFFFFA500))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.status,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                }
                                Text(
                                    item.modelName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("Quantity Target: ${item.quantity} pieces", style = MaterialTheme.typography.bodySmall)
                                Text("Weaver Completion: ${item.estimatedCompletionDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            // Interactive toggle switch
                            Column(horizontalAlignment = Alignment.End) {
                                Switch(
                                    checked = isCompleted,
                                    onCheckedChange = { viewModel.toggleProductionStatus(item) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color.Green,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.LightGray
                                    ),
                                    modifier = Modifier.testTag("toggle_production_${item.id}")
                                )
                                Text(
                                    text = if (isCompleted) "Finished Stock!" else "Active Loom",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = labelColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

