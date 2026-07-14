package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(viewModel: TallyViewModel) {
    var activeSubScreen by remember { mutableStateOf("MAIN") }

    if (activeSubScreen == "DATA_MANAGEMENT") {
        DataManagementScreen(viewModel, onBack = { activeSubScreen = "MAIN" })
    } else {
        MainSettingsScreen(viewModel, onNavigateToDataManagement = { activeSubScreen = "DATA_MANAGEMENT" })
    }
}

@Composable
fun MainSettingsScreen(viewModel: TallyViewModel, onNavigateToDataManagement: () -> Unit) {
    val email by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AntiqueCream),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Account Integration", style = MaterialTheme.typography.labelMedium, color = CardinalRed)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Logged in as", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = CardinalRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("settings_logout_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log Out")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Log Out")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AntiqueCream),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.labelMedium, color = CardinalRed)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode == true) Icons.Default.Star else Icons.Default.Info,
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Dark Mode Theme", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(
                        checked = isDarkMode == true,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Text(
                    text = "If disabled, the application respects the default system visual settings. Toggle it to force dark visuals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToDataManagement() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AntiqueCream),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Data Management", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go to Data Management", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

enum class ExportDateRange(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom Range")
}

enum class TransactionExportType(val displayName: String, val typeCode: String) {
    ALL("Total Transactions", "ALL"),
    SALE("Sales Only", "SALE"),
    PURCHASE("Purchases Only", "PURCHASE"),
    PRODUCTION("Production Only", "PRODUCTION")
}

fun getTimestampsForRange(range: ExportDateRange): Pair<Long?, Long?> {
    if (range == ExportDateRange.ALL_TIME) return Pair(null, null)

    val calendar = java.util.Calendar.getInstance()

    if (range == ExportDateRange.THIS_MONTH) {
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        return Pair(start, System.currentTimeMillis())
    }

    if (range == ExportDateRange.LAST_MONTH) {
        calendar.add(java.util.Calendar.MONTH, -1)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    return Pair(null, null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(viewModel: TallyViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val dateFormat = remember { java.text.SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()) }

    var selectedRange by remember { mutableStateOf(ExportDateRange.ALL_TIME) }
    var showRangeDropdown by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()

    var selectedTransactionType by remember { mutableStateOf(TransactionExportType.ALL) }
    var showTransactionTypeDropdown by remember { mutableStateOf(false) }

    val inventoryExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { resolvedUri -> viewModel.exportInventory(context, resolvedUri) }
    }

    val transactionExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { resolvedUri ->
            val timestamps = if (selectedRange == ExportDateRange.CUSTOM) {
                Pair(datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis?.let { it + 86399999L }) // add almost 1 day to include the end date fully
            } else {
                getTimestampsForRange(selectedRange)
            }
            viewModel.exportTransactions(context, resolvedUri, timestamps.first, timestamps.second, selectedTransactionType.typeCode)
        }
    }

    val inventoryPdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { resolvedUri -> viewModel.exportInventoryPdf(context, resolvedUri) }
    }

    val transactionPdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { resolvedUri ->
            val timestamps = if (selectedRange == ExportDateRange.CUSTOM) {
                Pair(datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis?.let { it + 86399999L }) // add almost 1 day to include the end date fully
            } else {
                getTimestampsForRange(selectedRange)
            }
            viewModel.exportTransactionsPdf(context, resolvedUri, timestamps.first, timestamps.second, selectedTransactionType.typeCode)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AntiqueCream),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Export Options", style = MaterialTheme.typography.labelMedium, color = CardinalRed)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { inventoryExportLauncher.launch("inventory_export_${dateFormat.format(java.util.Date())}.csv") },
                    colors = ButtonDefaults.buttonColors(containerColor = TobaccoSaddle, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...")
                    } else {
                        Icon(Icons.Default.Share, contentDescription = "Export Inventory")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Inventory to CSV")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { inventoryPdfExportLauncher.launch("inventory_export_${dateFormat.format(java.util.Date())}.pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = TobaccoSaddle, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...")
                    } else {
                        Icon(Icons.Default.Share, contentDescription = "Export Inventory PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Inventory to PDF")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Transaction Filters", style = MaterialTheme.typography.labelMedium, color = CardinalRed)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transaction Type", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))

                        Box {
                            OutlinedButton(
                                onClick = { showTransactionTypeDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                border = BorderStroke(1.dp, WarmBeige),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(selectedTransactionType.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Type")
                            }

                            DropdownMenu(
                                expanded = showTransactionTypeDropdown,
                                onDismissRequest = { showTransactionTypeDropdown = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, WarmBeige)
                            ) {
                                TransactionExportType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.displayName) },
                                        onClick = {
                                            selectedTransactionType = type
                                            showTransactionTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date Range", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))

                        Box {
                            OutlinedButton(
                                onClick = { showRangeDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                border = BorderStroke(1.dp, WarmBeige),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                if (selectedRange == ExportDateRange.CUSTOM && datePickerState.selectedStartDateMillis != null && datePickerState.selectedEndDateMillis != null) {
                                    val start = java.text.SimpleDateFormat("MM/dd", Locale.getDefault()).format(java.util.Date(datePickerState.selectedStartDateMillis!!))
                                    val end = java.text.SimpleDateFormat("MM/dd", Locale.getDefault()).format(java.util.Date(datePickerState.selectedEndDateMillis!!))
                                    Text("$start-$end", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else {
                                    Text(selectedRange.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Range")
                            }

                            DropdownMenu(
                                expanded = showRangeDropdown,
                                onDismissRequest = { showRangeDropdown = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, WarmBeige)
                            ) {
                                ExportDateRange.entries.forEach { range ->
                                    DropdownMenuItem(
                                        text = { Text(range.displayName) },
                                        onClick = {
                                            selectedRange = range
                                            showRangeDropdown = false
                                            if (range == ExportDateRange.CUSTOM) {
                                                showDatePickerDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showDatePickerDialog) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = { showDatePickerDialog = false }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDatePickerDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DateRangePicker(
                            state = datePickerState,
                            modifier = Modifier.weight(1f) // Ensure it fits the dialog
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { transactionExportLauncher.launch("transactions_export_${dateFormat.format(java.util.Date())}.csv") },
                    colors = ButtonDefaults.buttonColors(containerColor = TobaccoSaddle, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...")
                    } else {
                        Icon(Icons.Default.Share, contentDescription = "Export Transactions")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export ${selectedTransactionType.displayName} to CSV")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { transactionPdfExportLauncher.launch("transactions_export_${dateFormat.format(java.util.Date())}.pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = TobaccoSaddle, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...")
                    } else {
                        Icon(Icons.Default.Share, contentDescription = "Export Transactions PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export ${selectedTransactionType.displayName} to PDF")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.triggerManualBackup(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Manual Backup")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Database Backup")
                }
            }
        }
    }
}
