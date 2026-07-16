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

    if (selectedCustomer != null) {
        CustomerDetailScreen(
            customerIdentifier = selectedCustomer!!,
            logs = logs,
            onBack = { selectedCustomer = null }
        )
        return
    }

    val customers = logs
        .filter { it.type == "SALE" && (!(it.customerName ?: "").isBlank() || !(it.customerNumber ?: "").isBlank()) }
        .groupBy { 
            if (!(it.customerNumber ?: "").isBlank()) (it.customerNumber ?: "").trim() else (it.customerName ?: "").trim().lowercase() 
        }
        .map { (id, purchases) ->
            val totalSpent = purchases.sumOf { it.totalAmount }
            val lastPurchaseDate = purchases.maxByOrNull { it.timestamp }?.dateString ?: ""
            val name = purchases.firstOrNull { !(it.customerName ?: "").isBlank() }?.customerName ?: "Unknown"
            val number = purchases.firstOrNull { !(it.customerNumber ?: "").isBlank() }?.customerNumber ?: ""
            CustomerProfile(id, name, number, totalSpent, lastPurchaseDate)
        }
        .sortedByDescending { it.lastPurchaseDate }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            title = { Text("Customer Directory", color = RoyalCrimson, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

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
fun CustomerDetailScreen(customerIdentifier: String, logs: List<TransactionLog>, onBack: () -> Unit) {
    val purchases = logs.filter { it.type == "SALE" && (((it.customerNumber ?: "").trim() == customerIdentifier) || ((it.customerNumber ?: "").isBlank() && (it.customerName ?: "").trim().lowercase() == customerIdentifier)) }
        .sortedByDescending { it.timestamp }

    val name = purchases.firstOrNull { !(it.customerName ?: "").isBlank() }?.customerName ?: "Unknown"
    val number = purchases.firstOrNull { !(it.customerNumber ?: "").isBlank() }?.customerNumber ?: ""
    val totalSpent = purchases.sumOf { it.totalAmount }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            title = { Text("Customer Profile", color = RoyalCrimson, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
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
