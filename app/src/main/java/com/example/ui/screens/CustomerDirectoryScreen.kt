package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.TransactionLog
import com.example.ui.TallyViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDirectoryScreen(viewModel: TallyViewModel, onBack: () -> Unit) {
    val logs by viewModel.transactionLogs.collectAsStateWithLifecycle()

    var selectedCustomer by remember { mutableStateOf<String?>(null) }
    var nameFilter by remember { mutableStateOf("") }
    var numberFilter by remember { mutableStateOf("") }

    if (selectedCustomer != null) {
        DisposableEffect(selectedCustomer) {
            viewModel.setCustomBackAction { selectedCustomer = null }
            onDispose { viewModel.setCustomBackAction(null) }
        }

        CustomerDetailScreen(
            customerIdentifier = selectedCustomer!!,
            logs = logs,
            viewModel = viewModel,
            onBack = { selectedCustomer = null },
            onIdentifierChanged = { selectedCustomer = it }
        )
        return
    }

    val customers = logs
        .filter { it.type == "SALE" && (it.customerName.isNotBlank() || it.customerNumber.isNotBlank()) }
        .groupBy {
            if (it.customerNumber.isNotBlank()) it.customerNumber.trim() else it.customerName.trim().lowercase()
        }
        .map { (id, purchases) ->
            val totalSpent = purchases.sumOf { it.totalAmount }
            val lastPurchaseDate = purchases.maxByOrNull { it.timestamp }?.dateString ?: ""
            val name = purchases.firstOrNull { it.customerName.isNotBlank() }?.customerName ?: "Unknown"
            val number = purchases.firstOrNull { it.customerNumber.isNotBlank() }?.customerNumber ?: ""
            CustomerProfile(id, name, number, totalSpent, lastPurchaseDate)
        }
        .filter { profile ->
            (nameFilter.isBlank() || profile.name.contains(nameFilter, ignoreCase = true)) &&
                    (numberFilter.isBlank() || profile.number.contains(numberFilter, ignoreCase = true))
        }
        .sortedByDescending { it.lastPurchaseDate }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            title = { Text("Customer Directory", color = RoyalCrimson, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nameFilter,
                onValueChange = { nameFilter = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Name", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AntiqueCream,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AntiqueCream
                ),
                shape = RoundedCornerShape(4.dp)
            )
            OutlinedTextField(
                value = numberFilter,
                onValueChange = { numberFilter = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Number", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AntiqueCream,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AntiqueCream
                ),
                shape = RoundedCornerShape(4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (customers.isEmpty()) {
                item {
                    Text("No customer profiles found. When you record a sale with a name or number, they will appear here.", modifier = Modifier.padding(16.dp), color = Color.White)
                }
            } else {
                items(customers) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCustomer = customer.id },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, AntiqueCream),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SageGreen, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                if (customer.number.isNotBlank()) {
                                    Text(customer.number, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                }
                                Text("Last active: ${customer.lastPurchaseDate}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                Text("৳ ${formatCurrency(customer.totalSpent)}", style = MaterialTheme.typography.titleMedium, color = RoyalCrimson, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CustomerProfile(
    val id: String,
    val name: String,
    val number: String,
    val totalSpent: Double,
    val lastPurchaseDate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(customerIdentifier: String, logs: List<TransactionLog>, viewModel: TallyViewModel, onBack: () -> Unit, onIdentifierChanged: (String) -> Unit) {
    val currentPurchases = logs.filter { it.type == "SALE" && ((it.customerNumber.trim() == customerIdentifier) || (it.customerNumber.isBlank() && it.customerName.trim().lowercase() == customerIdentifier)) }
        .sortedByDescending { it.timestamp }

    var purchases by remember { mutableStateOf(currentPurchases) }

    LaunchedEffect(currentPurchases) {
        if (currentPurchases.isNotEmpty()) {
            purchases = currentPurchases
        }
    }

    val name = purchases.firstOrNull { it.customerName.isNotBlank() }?.customerName ?: "Unknown"
    val number = purchases.firstOrNull { it.customerNumber.isNotBlank() }?.customerNumber ?: ""
    val totalSpent = purchases.sumOf { it.totalAmount }

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(name) }
        var editNumber by remember { mutableStateOf(number) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Customer Profile", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editNumber,
                        onValueChange = { editNumber = it },
                        label = { Text("Customer Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newIdentifier = if (editNumber.isNotBlank()) editNumber.trim() else editName.trim().lowercase()
                        viewModel.updateCustomerProfile(name, number, editName.trim(), editNumber.trim()) {
                            onIdentifierChanged(newIdentifier)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save", color = RoyalCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            title = { Text("Customer Profile", color = RoyalCrimson, fontWeight = FontWeight.Bold) },
            actions = {
                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.padding(end = 8.dp).height(36.dp)
                ) {
                    Text("EDIT", color = SlateDark, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(GoldAccent, RoundedCornerShape(40.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        if (number.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = SoftEggshell, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(number, style = MaterialTheme.typography.bodyMedium, color = SoftEggshell)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lifetime Value", style = MaterialTheme.typography.labelSmall, color = SoftEggshell.copy(alpha = 0.7f))
                        Text("৳ ${formatCurrency(totalSpent)}", style = MaterialTheme.typography.headlineMedium, color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text("Purchase History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }

            items(purchases) { sale ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, AntiqueCream),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sale.modelName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("৳ ${formatCurrency(sale.totalAmount)}", style = MaterialTheme.typography.titleMedium, color = SageGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${sale.quantity} pcs @ ৳${formatCurrency(sale.unitPrice)}/pc", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            Text("${sale.dateString} ${sale.timeString}", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}
