package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.ui.TallyViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthorizedLayout(viewModel: TallyViewModel, currentScreen: String, syncState: SyncState) {
    val email by viewModel.currentUserEmail.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header Banner
        TopBanner(
            userEmail = email,
            syncState = syncState,
            onSettings = { viewModel.navigateTo("SETTINGS") },
            onBackToDashboard = { viewModel.navigateTo("DASHBOARD") },
            showBackButton = currentScreen != "DASHBOARD",
        ) {
            viewModel.forceSync()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (currentScreen) {
                "DASHBOARD" -> DashboardScreen(viewModel)
                "STOCK_HOUSE" -> StockHouseScreen(viewModel)
                "STOCK_PRODUCTION" -> StockProductionScreen(viewModel)
                "PURCHASE" -> PurchaseScreen(viewModel)
                "SELL" -> SellScreen(viewModel)
                "HISTORY" -> HistoryScreen(viewModel)
                "SETTINGS" -> SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
fun TopBanner(
    userEmail: String,
    syncState: SyncState,
    onSettings: () -> Unit,
    onBackToDashboard: () -> Unit,
    showBackButton: Boolean,
    onSync: () -> Unit,
) {
    Surface(
        color = SlateDark,
        contentColor = SoftEggshell,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBackToDashboard,
                            modifier = Modifier.testTag("action_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldAccent)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = "Rakib Silk & Fashion",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = GoldAccent
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onSync() }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                        ) {
                            // Sync state chip
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (syncState) {
                                            is SyncState.SYNCING -> Color.Yellow
                                            is SyncState.SUCCESS -> Color.Green
                                            is SyncState.IDLE -> Color.DarkGray
                                            else -> Color.Red
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (syncState) {
                                    is SyncState.SYNCING -> "MongoDB Syncing..."
                                    is SyncState.SUCCESS -> "MongoDB Connected"
                                    is SyncState.IDLE -> "Offline-Ready"
                                    is SyncState.ERROR -> "Local Save (Unsynced)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = SoftEggshell.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Manager Logged:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = SoftEggshell.copy(alpha = 0.6f)
                        )
                        Text(
                            text = userEmail.substringBefore("@"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SoftEggshell,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GoldAccent)
                    }
                }
            }
        }
    }
}
