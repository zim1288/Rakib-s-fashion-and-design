package com.example.ui.screens

import com.example.ui.TallyViewModel

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
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalAnimationApi::class)
// ---------------- 1. STOCK IN HOUSE (Brand Switch) ----------------
@Composable
fun StockHouseScreen(viewModel: TallyViewModel) {
    val sarees by viewModel.sareeItems.collectAsStateWithLifecycle()
    val lowStockThreshold by viewModel.lowStockThreshold.collectAsStateWithLifecycle()
    var selectedBrand by remember { mutableStateOf("Rakib Silk") } // Rakib Silk / Rakib Fashion

    val valuation = sarees.filter { it.brandCategory == selectedBrand }.sumOf { it.totalValue }

    var itemToEdit by remember { mutableStateOf<SareeItem?>(null) }
    var isAdjustingThreshold by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAdjustingThreshold = true },
                containerColor = RoyalCrimson,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Adjust Threshold")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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
                    Text(
                        text = "৳ ${formatCurrency(valuation)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
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

            var searchQuery by remember { mutableStateOf("") }
            var fabricFilter by remember { mutableStateOf("All Fabrics") }
            var colorFilter by remember { mutableStateOf("All Colors") }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search items by name or SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fabricFilter,
                    onValueChange = { fabricFilter = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Fabric") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = colorFilter,
                    onValueChange = { colorFilter = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Color") },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filteredList = sarees.filter { it.brandCategory == selectedBrand }.filter {
                (searchQuery.isBlank() || it.modelName.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true)) &&
                        (fabricFilter == "All Fabrics" || fabricFilter.isBlank() || it.fabricType.contains(fabricFilter, ignoreCase = true)) &&
                        (colorFilter == "All Colors" || colorFilter.isBlank() || it.color.contains(colorFilter, ignoreCase = true))
            }

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
                        Text("No stock items found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
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
                        val isLowStock = item.pieceCount < lowStockThreshold
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.modelName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isLowStock) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                Text("Low Stock", color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(horizontal = 4.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "SKU: ${item.sku.ifBlank { "N/A" }} | ${item.color} | ${item.fabricType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Unit: ৳${formatCurrency(item.unitPrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.pieceCount} pcs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = stockTextColor,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Interactive Total Value label and actions
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "৳ ${formatCurrency(item.totalValue)}",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = RoyalCrimson,
                                        textAlign = TextAlign.End,
                                        maxLines = 1
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
                                        Spacer(modifier = Modifier.width(4.dp))
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
    }

    // Modal Dialog to edit saree details inline (Optimistic instantly!)
    itemToEdit?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { itemToEdit = null }
        ) { name, sku, color, fabricType, category, price, count, imageUrl ->
            viewModel.updateStockItemDetails(item.id, name, sku, color, fabricType, category, price, count, imageUrl)
            itemToEdit = null
        }
    }

    if (isAdjustingThreshold) {
        Dialog(onDismissRequest = { isAdjustingThreshold = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Low Stock", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Low Stock Threshold", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = lowStockThreshold.toFloat(),
                            onValueChange = { viewModel.setLowStockThreshold(it.toInt()) },
                            valueRange = 0f..50f,
                            steps = 49,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "$lowStockThreshold pcs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Items with quantity below this threshold will be highlighted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { isAdjustingThreshold = false },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson, contentColor = Color.White)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun EditItemDialog(
    item: SareeItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Double, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf(item.modelName) }
    var sku by remember { mutableStateOf(item.sku) }
    var color by remember { mutableStateOf(item.color) }
    var fabricType by remember { mutableStateOf(item.fabricType) }
    var priceStr by remember { mutableStateOf(item.unitPrice.toString()) }
    var countStr by remember { mutableStateOf(item.pieceCount.toString()) }
    var brandCategory by remember { mutableStateOf(item.brandCategory) }
    var imageUrl by remember { mutableStateOf(item.imageUrl) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            try {
                val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                it.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                imageUrl = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text("Color") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fabricType,
                        onValueChange = { fabricType = it },
                        label = { Text("Fabric Type") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
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
                                containerColor = if (active) RoyalCrimson else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
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
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gallery")
                        }
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Camera")
                        }
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
                            onConfirm(name, sku, color, fabricType, brandCategory, p, count, imageUrl)
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

