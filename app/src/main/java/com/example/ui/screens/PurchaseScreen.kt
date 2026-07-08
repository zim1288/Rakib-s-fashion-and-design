package com.example.ui.screens

import com.example.ui.TallyViewModel

import androidx.compose.animation.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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
import com.example.db.*
import com.example.ui.theme.*
import com.example.utils.ImageHelper
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
// ---------------- 3. PURCHASE PRODUCT (Incoming Cart Staging) ----------------
@Composable
fun PurchaseScreen(viewModel: TallyViewModel) {
    val cartList = viewModel.purchaseCart

    var sareeInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Rakib Silk") } // Rakib Silk / Rakib Fashion
    var costInput by remember { mutableStateOf("") }
    var qtyInput by remember { mutableStateOf("") }
    var skuInput by remember { mutableStateOf("") }
    var colorInput by remember { mutableStateOf("") }
    var fabricTypeInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val savedUri = ImageHelper.copyUriToInternalStorage(context, uri)
            if (savedUri != null) {
                imageUrlInput = savedUri
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val savedUri = ImageHelper.saveBitmapToInternalStorage(context, it)
            if (savedUri != null) {
                imageUrlInput = savedUri
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Stage Incoming Cargo Shipments", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RoyalCrimson)
        Text("Build bulk purchase cart, then lock stock on commitment", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        // Input Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = sareeInput,
                    onValueChange = { sareeInput = it },
                    label = { Text("Model Saree Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = skuInput,
                    onValueChange = { skuInput = it },
                    label = { Text("SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colorInput,
                        onValueChange = { colorInput = it },
                        label = { Text("Color") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fabricTypeInput,
                        onValueChange = { fabricTypeInput = it },
                        label = { Text("Fabric Type") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Class Brand Category:", style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.LightGray.copy(alpha = 0.3f))) {
                            listOf("Rakib Silk", "Rakib Fashion").forEach { brand ->
                                val active = brand == categoryInput
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { categoryInput = brand }
                                        .background(if (active) RoyalCrimson else Color.Transparent)
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(brand.substringAfter(" "), fontSize = 11.sp, color = if (active) Color.White else SlateDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = it },
                        label = { Text("Unit Cost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )

                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        label = { Text("Pieces") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val cost = costInput.toDoubleOrNull() ?: 0.0
                        val qty = qtyInput.toIntOrNull() ?: 0
                        if (sareeInput.isNotBlank() && cost > 0 && qty > 0) {
                            viewModel.addToCart(sareeInput, categoryInput, cost, qty, imageUrlInput, skuInput, colorInput, fabricTypeInput)
                            // Clear inputs
                            sareeInput = ""
                            skuInput = ""
                            colorInput = ""
                            fabricTypeInput = ""
                            costInput = ""
                            qtyInput = ""
                            imageUrlInput = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_to_cart_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("STAGE ITEM INTO SHIPMENT CART")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Staged cart ledger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bulk Cargo Staging Cart (${cartList.size} items)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            if (cartList.isNotEmpty()) {
                Text(
                    text = "Clear Cart",
                    color = CardinalRed,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable { viewModel.clearCart() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cartList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Your bulk shipment shipment log is empty.", color = Color.Gray)
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("purchase_cart"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(cartList) { idx, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, RoyalCrimson.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!item.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = "Selected Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.LightGray.copy(alpha = 0.3f))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column {
                                        Text(item.modelName, fontWeight = FontWeight.Bold)
                                        Row {
                                            Text(
                                                text = item.brandCategory,
                                                color = if (item.brandCategory == "Rakib Silk") GoldAccent else RoyalCrimson,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Cost: ৳${formatCurrency(item.unitCost)} x ${item.quantity} pieces", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("৳${formatCurrency(item.unitCost * item.quantity)}", fontWeight = FontWeight.Bold, color = RoyalCrimson, modifier = Modifier.padding(end = 8.dp))
                                    IconButton(onClick = { viewModel.removeFromCart(idx) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint = CardinalRed)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val cartTotal = cartList.sumOf { it.unitCost * it.quantity }

                Button(
                    onClick = { viewModel.commitCartToStock() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("commit_stock_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text(
                        "COMMIT ৳${formatCurrency(cartTotal)} TO HOUSE STOCK",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlateDark
                    )
                }
            }
        }
    }
}

