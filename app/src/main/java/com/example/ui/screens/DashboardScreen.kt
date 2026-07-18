package com.example.ui.screens

import com.example.ui.TallyViewModel

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
// ---------------- DASHBOARD (5 Pillars grid) ----------------
@Composable
fun DashboardScreen(viewModel: TallyViewModel) {
    val sarees by viewModel.sareeItems.collectAsStateWithLifecycle()
    val productions by viewModel.productionItems.collectAsStateWithLifecycle()

    val totalValuation = sarees.sumOf { it.totalValue }
    val activeWeavingCount = productions.filter { it.status == "In Progress" }.sumOf { it.quantity }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-level valuation overview card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("valuation_hero_card"),
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, GoldAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "TOTAL STOCK VALUATION",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${formatCurrency(totalValuation)}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Active Models", style = MaterialTheme.typography.bodySmall, color = SoftEggshell.copy(alpha = 0.6f), maxLines = 1)
                            Text(
                                sarees.size.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = SoftEggshell,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Total Weaving", style = MaterialTheme.typography.bodySmall, color = SoftEggshell.copy(alpha = 0.6f), maxLines = 1)
                            Text("$activeWeavingCount pcs", style = MaterialTheme.typography.titleMedium, color = SoftEggshell, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "CORE PILLARS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Start
            )
        }

        // Implementation of the 5 Core Pillar Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillarNavigationRow(
                    title1 = "Stock in House",
                    desc1 = "In-hand saree catalog",
                    icon1 = Icons.Default.Home,
                    tag1 = "pillar_stock_house",
                    onClick1 = { viewModel.navigateTo("STOCK_HOUSE") },
                    title2 = "Stock in Production",
                    desc2 = "Track manufacturing",
                    icon2 = Icons.Default.Build,
                    tag2 = "pillar_production",
                    onClick2 = { viewModel.navigateTo("STOCK_PRODUCTION") }
                )

                PillarNavigationRow(
                    title1 = "Purchase Product",
                    desc1 = "Stage shipping cart",
                    icon1 = Icons.Default.ShoppingCart,
                    tag1 = "pillar_purchase",
                    onClick1 = { viewModel.navigateTo("PURCHASE") },
                    title2 = "Sell Product",
                    desc2 = "Log customer client sales",
                    icon2 = Icons.AutoMirrored.Filled.Send,
                    tag2 = "pillar_sell",
                    onClick2 = { viewModel.navigateTo("SELL") },
                    containerColor1 = SageGreen,
                    borderColor1 = Color(0xFFBFCDBB),
                    contentColor1 = Color(0xFF2D332C),
                    containerColor2 = TerracottaPink,
                    borderColor2 = Color(0xFFEAC9BC),
                    contentColor2 = Color(0xFF3D2C26)
                )
                PillarNavigationRow(
                    title1 = "Customer Directory",
                    desc1 = "Client profiles & logs",
                    icon1 = Icons.Default.Person,
                    tag1 = "pillar_customers",
                    onClick1 = { viewModel.navigateTo("CUSTOMERS") },
                    title2 = "Transaction History",
                    desc2 = "Compile analytics live",
                    icon2 = Icons.AutoMirrored.Filled.List,
                    tag2 = "pillar_history",
                    onClick2 = { viewModel.navigateTo("HISTORY") }
                )
            }
        }
    }
}

@Composable
fun PillarNavigationRow(
    title1: String, desc1: String, icon1: ImageVector, tag1: String, onClick1: () -> Unit,
    title2: String, desc2: String, icon2: ImageVector, tag2: String, onClick2: () -> Unit,
    containerColor1: Color = MaterialTheme.colorScheme.surface,
    borderColor1: Color = AntiqueCream,
    contentColor1: Color = MaterialTheme.colorScheme.onSurface,
    containerColor2: Color = MaterialTheme.colorScheme.surface,
    borderColor2: Color = AntiqueCream,
    contentColor2: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick1)
                .testTag(tag1),
            colors = CardDefaults.cardColors(containerColor = containerColor1),
            border = BorderStroke(1.dp, borderColor1),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(RoyalCrimson),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon1, contentDescription = title1, tint = SoftEggshell, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title1,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = contentColor1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = desc1,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = contentColor1.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick2)
                .testTag(tag2),
            colors = CardDefaults.cardColors(containerColor = containerColor2),
            border = BorderStroke(1.dp, borderColor2),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(RoyalCrimson),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon2, contentDescription = title2, tint = SoftEggshell, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title2,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = contentColor2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = desc2,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = contentColor2.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

