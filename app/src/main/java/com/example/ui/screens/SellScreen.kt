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
import com.example.ui.TallyViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
// ---------------- 4. SELL PRODUCT (Deduct Ledger) ----------------
@Composable
fun SellScreen(viewModel: TallyViewModel) {
    val sarees by viewModel.sareeItems.collectAsStateWithLifecycle()
    var selectedItemIndex by remember { mutableIntStateOf(-1) }
    var qtyToSellStr by remember { mutableStateOf("") }
    var retailPriceStr by remember { mutableStateOf("") }
    var customerNameStr by remember { mutableStateOf("") }
    var customerNumberStr by remember { mutableStateOf("") }
    
    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Record Retail Client Sale", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RoyalCrimson)
        Text("Select an existing catalog saree in stock to subtract and log", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(16.dp))

        if (sarees.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Cannot record sales. Active house inventory is currently empty.")
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales_form_card"),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Retail Saree Entry Selection:", style = MaterialTheme.typography.labelSmall)
                    
                    // Simple Dropdown simulation
                    var expanded by remember { mutableStateOf(value = false) }
                    var searchDropdownQuery by remember { mutableStateOf("") }
                    val currentSelectionName = if (selectedItemIndex in sarees.indices) {
                        "${sarees[selectedItemIndex].modelName} (${sarees[selectedItemIndex].brandCategory}) - Available: ${sarees[selectedItemIndex].pieceCount} pcs"
                    } else {
                        "SELECT A SAREE MODEL FROM HOUSE STOCK"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray.copy(alpha = 0.2f))
                            .clickable { expanded = !expanded }
                            .padding(14.dp)
                            .testTag("saree_selector")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentSelectionName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown"
                            )
                        }
                    }

                    if (expanded) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            border = BorderStroke(0.5.dp, Color.LightGray)
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = searchDropdownQuery,
                                    onValueChange = { searchDropdownQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    placeholder = { Text("Search by name or SKU...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    singleLine = true
                                )
                                val filteredSarees = sarees.filter {
                                    searchDropdownQuery.isBlank() || 
                                    it.modelName.contains(searchDropdownQuery, ignoreCase = true) || 
                                    it.sku.contains(searchDropdownQuery, ignoreCase = true)
                                }
                                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                    items(filteredSarees.size) { i ->
                                        val item = filteredSarees[i]
                                        val idx = sarees.indexOf(item)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedItemIndex = idx
                                                retailPriceStr = item.unitPrice.toString() // Prepopulate
                                                expanded = false
                                            }
                                            .padding(12.dp)
                                            .background(if (idx == selectedItemIndex) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    ) {
                                        Column {
                                            Text(
                                                text = item.modelName,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row {
                                                Text(
                                                    text = item.brandCategory,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Avail: ${item.pieceCount} | P: ৳${formatCurrency(item.unitPrice)}",
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = qtyToSellStr,
                        onValueChange = { qtyToSellStr = it },
                        label = { Text("Log Sold Pieces") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sold_quantity_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = retailPriceStr,
                        onValueChange = { retailPriceStr = it },
                        label = { Text("Client Retail Price (Per piece)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sold_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customerNameStr,
                        onValueChange = { customerNameStr = it },
                        label = { Text("Customer Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customerNumberStr,
                        onValueChange = { customerNumberStr = it },
                        label = { Text("Customer Number (Optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_number_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    feedbackMsg?.let { errorMsg ->
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    
                    successMsg?.let { successText ->
                        Text(successText, color = Color(0xFF0F5132), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    Button(
                        onClick = {
                            feedbackMsg = null
                            successMsg = null
                            focusManager.clearFocus()

                            if (selectedItemIndex in sarees.indices) {
                                val selectedSaree = sarees[selectedItemIndex]
                                val pieces = qtyToSellStr.toIntOrNull() ?: 0
                                val price = retailPriceStr.toDoubleOrNull() ?: 0.0

                                viewModel.logClientSale(
                                    sareeItem = selectedSaree,
                                    quantityToSell = pieces,
                                    salePricePerPiece = price,
                                    customerName = customerNameStr,
                                    customerNumber = customerNumberStr,
                                    onError = { feedbackMsg = it }
                                ) {
                                    successMsg = "Sale Logged Instantly! Deducted $pieces from available stock."
                                    qtyToSellStr = ""
                                    retailPriceStr = ""
                                    customerNameStr = ""
                                    customerNumberStr = ""
                                    selectedItemIndex = -1
                                }
                            } else {
                                feedbackMsg = "Please choose a saree model from current inventory list first."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_sale_button")
                    ) {
                        Text("DEDUCT INVENTORY AND LOG SALES", color = Color.White)
                    }
                }
            }
        }
    }
}