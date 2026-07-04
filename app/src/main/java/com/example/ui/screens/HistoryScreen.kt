package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
// ---------------- 5. TRANSACTION HISTORY & ANALYTICS LEDGER ----------------
@Composable
fun HistoryScreen(viewModel: TallyViewModel) {
    val logs by viewModel.transactionLogs.collectAsStateWithLifecycle()
    val monthFilter by viewModel.selectedMonthFilter.collectAsStateWithLifecycle()
    val yearFilter by viewModel.selectedYearFilter.collectAsStateWithLifecycle()

    val availableMonths = listOf("All", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val availableYears = listOf("All", "2026", "2027", "2028")

    // Filter logs in Memory matching selected month / year
    val filteredLogs = remember(logs, monthFilter, yearFilter) {
        logs.filter { log ->
            val matchYear = if (yearFilter == "All") true else log.dateString.startsWith(yearFilter)
            val matchMonth = if (monthFilter == "All") true else {
                val monthIndex = availableMonths.indexOf(monthFilter) // 1 for Jan, 2 for Feb...
                if (monthIndex > 0) {
                    val expectedSub = if (monthIndex < 10) "-0$monthIndex-" else "-$monthIndex-"
                    log.dateString.contains(expectedSub)
                } else true
            }
            matchYear && matchMonth
        }
    }

    val totalSpent = filteredLogs.filter { it.type == "EXPENSE" }.sumOf { it.totalAmount }
    val totalEarned = filteredLogs.filter { it.type == "SALE" }.sumOf { it.totalAmount }
    val netProfitLoss = totalEarned - totalSpent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Central Ledger & Analytics", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))

        // Timeframe Dropdown Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Select Month Filter", style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .clickable { /* Simulate simpler dropdown cycles for faster UX */
                            val nextIdx = (availableMonths.indexOf(monthFilter) + 1) % availableMonths.size
                            viewModel.setMonthFilter(availableMonths[nextIdx])
                        }
                        .padding(12.dp)
                        .testTag("month_filter_trigger"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(monthFilter, fontWeight = FontWeight.Bold, color = RoyalCrimson)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Select Calendar Year", style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .clickable {
                            val nextIdx = (availableYears.indexOf(yearFilter) + 1) % availableYears.size
                            viewModel.setYearFilter(availableYears[nextIdx])
                        }
                        .padding(12.dp)
                        .testTag("year_filter_trigger"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(yearFilter, fontWeight = FontWeight.Bold, color = RoyalCrimson)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Financial status boxes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL SPENT (PURCHASES)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = SoftEggshell.copy(alpha = 0.6f), maxLines = 1)
                        Text("৳ ${formatCurrency(totalSpent)}", style = MaterialTheme.typography.titleMedium, color = CardinalRed, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOTAL EARNED (SALES)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = SoftEggshell.copy(alpha = 0.6f), maxLines = 1)
                        Text("৳ ${formatCurrency(totalEarned)}", style = MaterialTheme.typography.titleMedium, color = Color.Green, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = GoldAccent.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                val isProfit = netProfitLoss >= 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TALLY NET P&L STATUS", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = GoldAccent)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isProfit) Color(0xFF198754) else CardinalRed)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${if (isProfit) "PROFIT: +" else "LOSS: "}৳${formatCurrency(netProfitLoss)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List logs
        Text("Chronological History Logs (${filteredLogs.size})", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No ledger activities captured for given timeline filters.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("ledger_logs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredLogs) { _, log ->
                    val isIncome = log.type == "SALE"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.modelName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (log.type == "EXPENSE") "EXPENSE" else if (log.type == "SALE") "RETAIL SALES" else "SYSTEM LOG",
                                        color = if (isIncome) Color(0xFF198754) else if (log.type == "EXPENSE") CardinalRed else Color.Blue,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = log.dateString,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isIncome) "+৳${formatCurrency(log.totalAmount)}" else if (log.type == "EXPENSE") "-৳${formatCurrency(log.totalAmount)}" else "Update",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isIncome) Color(0xFF198754) else if (log.type == "EXPENSE") CardinalRed else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${log.quantity} pcs @ ৳${formatCurrency(log.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
}
