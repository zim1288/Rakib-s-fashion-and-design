package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.db.*
import com.example.ui.TallyViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
// ---------------- 1. STOCK IN HOUSE (Brand Switch) ----------------
@Composable
fun StockHouseScreen(viewModel: TallyViewModel) {
    val sarees by viewModel.sareeItems.collectAsStateWithLifecycle()
    var selectedBrand by remember { mutableStateOf("Rakib Silk") } // Rakib Silk / Rakib Fashion

    val valuation = sarees.filter { it.brandCategory == selectedBrand }.sumOf { it.totalValue }

    var itemToEdit by remember { mutableStateOf<SareeItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Instant Live Valuation banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            border = BorderStroke(1.dp, GoldAccent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("$selectedBrand Total Valuation", style = MaterialTheme.typography.bodyMedium, color = GoldAccent)
                Text("৳ ${formatCurrency(valuation)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Brand division controller
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Rakib Silk", "Rakib Fashion").forEach { brand ->
                val isSelected = selectedBrand == brand
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedBrand = brand }
                        .background(if (isSelected) RoyalCrimson else Color.Transparent)
                        .padding(vertical = 12.dp)
                        .testTag("brand_tab_${brand.replace(" ", "_")}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = brand.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredList = sarees.filter { it.brandCategory == selectedBrand }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = "Empty", tint = RoyalCrimson.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No stock items found under $selectedBrand", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("stock_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredList) { index, item ->
                    val isLowStock = item.pieceCount < 10
                    val cardBackgroundColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    val cardBorderColor = if (isLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    val stockTextColor = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stock_item_$index"),
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        border = BorderStroke(if (isLowStock) 1.dp else 0.5.dp, cardBorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = "Saree Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray.copy(alpha = 0.3f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.modelName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Unit Cost: ৳${formatCurrency(item.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("Stock: ${item.pieceCount} pcs", style = MaterialTheme.typography.bodySmall, color = stockTextColor, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Interactive Total Value label and actions
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "৳ ${formatCurrency(item.totalValue)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalCrimson
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row {
                                    IconButton(
                                        onClick = { itemToEdit = item },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("edit_item_${item.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GoldAccent, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteStockItem(item) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_item_${item.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CardinalRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to edit saree details inline (Optimistic instantly!)
    itemToEdit?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { itemToEdit = null },
            onConfirm = { name, category, price, count, imageUrl ->
                viewModel.updateStockItemDetails(item.id, name, category, price, count, imageUrl)
                itemToEdit = null
            }
        )
    }
}

@Composable
fun EditItemDialog(
    item: SareeItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf(item.modelName) }
    var priceStr by remember { mutableStateOf(item.unitPrice.toString()) }
    var countStr by remember { mutableStateOf(item.pieceCount.toString()) }
    var brandCategory by remember { mutableStateOf(item.brandCategory) }
    var imageUrl by remember { mutableStateOf(item.imageUrl) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text("Edit Saree Details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Model Saree Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Select Brand
                Text("Brand Category:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Rakib Silk", "Rakib Fashion").forEach { brand ->
                        val active = brand == brandCategory
                        Button(
                            onClick = { brandCategory = brand },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) TobaccoSaddle else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(brand)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Unit Retail Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = countStr,
                    onValueChange = { countStr = it },
                    label = { Text("Piece Stock Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Selected Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
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
                        Text(if (imageUrl.isNullOrBlank()) "Add Picture" else "Change Picture")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val p = priceStr.toDoubleOrNull() ?: item.unitPrice
                            val count = countStr.toIntOrNull() ?: item.pieceCount
                            onConfirm(name, brandCategory, p, count, imageUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson)
                    ) {
                        Text("SAVE CHANGES", color = Color.White)
                    }
                }
            }
        }
    }
}

