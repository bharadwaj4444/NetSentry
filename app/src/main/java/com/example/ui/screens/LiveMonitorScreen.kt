package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection
import com.example.ui.MainViewModel
import com.example.ui.ProbeResultState
import com.example.ui.components.ConnectionCard
import com.example.ui.components.ConnectionDetailDialog
import com.example.ui.components.LivePulseDot
import com.example.ui.components.TrafficThroughputCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBlueSecondary
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NoticeYellow
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveMonitorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
    val liveConnections by viewModel.liveConnections.collectAsStateWithLifecycle()
    val trafficSnapshot by viewModel.trafficSnapshot.collectAsStateWithLifecycle()
    val recentAlerts by viewModel.recentAlerts.collectAsStateWithLifecycle()
    val inspectedConnection by viewModel.inspectedConnection.collectAsStateWithLifecycle()
    val probeState by viewModel.probeState.collectAsStateWithLifecycle()
    val aiAssessmentState by viewModel.aiAssessmentState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showCustomProbeDialog by remember { mutableStateOf(false) }

    val filteredLiveConnections = remember(liveConnections, searchQuery) {
        if (searchQuery.isBlank()) {
            liveConnections
        } else {
            val q = searchQuery.trim().lowercase()
            liveConnections.filter {
                it.remoteAddress.lowercase().contains(q) ||
                it.remoteHost.lowercase().contains(q) ||
                it.appName.lowercase().contains(q) ||
                it.remotePort.toString().contains(q) ||
                it.protocol.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Sentry Status Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isMonitoring) CyberCyanPrimary.copy(alpha = 0.5f) else BorderSubtle
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isMonitoring) CyberCyanPrimary.copy(alpha = 0.15f) else CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Shield",
                                tint = if (isMonitoring) CyberCyanPrimary else TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LivePulseDot(isActive = isMonitoring, color = CyberCyanPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMonitoring) "ACTIVE MONITORING" else "MONITORING PAUSED",
                                    color = if (isMonitoring) SafeGreen else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "${liveConnections.size} Sockets Inspected",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Switch(
                        checked = isMonitoring,
                        onCheckedChange = { viewModel.toggleMonitoring(context) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberCyanPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceVariant
                        ),
                        modifier = Modifier.testTag("monitoring_toggle")
                    )
                }
            }
        }

        // 2. Real-time Alert Notification Banner (if recent new connections occurred)
        if (recentAlerts.isNotEmpty()) {
            item {
                val latest = recentAlerts.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.inspectConnection(latest) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (latest.alertLevel) {
                            AlertLevel.HIGH_RISK -> AlertRed.copy(alpha = 0.2f)
                            AlertLevel.SUSPICIOUS -> NoticeYellow.copy(alpha = 0.2f)
                            else -> CyberBlueSecondary.copy(alpha = 0.15f)
                        }
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            when (latest.alertLevel) {
                                AlertLevel.HIGH_RISK -> AlertRed
                                AlertLevel.SUSPICIOUS -> NoticeYellow
                                else -> CyberCyanPrimary.copy(alpha = 0.6f)
                            }
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (latest.alertLevel) {
                                AlertLevel.HIGH_RISK, AlertLevel.SUSPICIOUS -> Icons.Default.Warning
                                else -> Icons.Default.NotificationsActive
                            },
                            contentDescription = "Alert",
                            tint = when (latest.alertLevel) {
                                AlertLevel.HIGH_RISK -> AlertRed
                                AlertLevel.SUSPICIOUS -> NoticeYellow
                                else -> CyberCyanPrimary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REAL-TIME ALERT: New Connection Detected",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${latest.appName} -> ${latest.displayDestination}:${latest.remotePort} (${latest.protocol})",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { viewModel.analyzeConnectionWithAI(latest) },
                                modifier = Modifier.padding(end = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("AI Scan", color = CyberCyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "View",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Live Wi-Fi & Cellular Traffic Meter
        item {
            TrafficThroughputCard(snapshot = trafficSnapshot)
        }

        // 4. Test Connection Probe Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Test Probe",
                                tint = NoticeYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONNECTION PROBE / TEST AUDIT",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        if (probeState is ProbeResultState.Testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyberCyanPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trigger a live outbound connection to test real-time detection & alert dispatching instantly:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            ProbeChip(
                                label = "DNS (8.8.8.8:53)",
                                onClick = { viewModel.executeTestProbe("8.8.8.8", 53) }
                            )
                        }
                        item {
                            ProbeChip(
                                label = "HTTPS (1.1.1.1:443)",
                                onClick = { viewModel.executeTestProbe("1.1.1.1", 443) }
                            )
                        }
                        item {
                            ProbeChip(
                                label = "HTTP (example.com:80)",
                                onClick = { viewModel.executeTestProbe("example.com", 80) }
                            )
                        }
                        item {
                            ProbeChip(
                                label = "Custom...",
                                onClick = { showCustomProbeDialog = true }
                            )
                        }
                    }

                    // Probe feedback
                    when (val state = probeState) {
                        is ProbeResultState.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SafeGreen.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, "Success", tint = SafeGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Probe established & logged! Detected ${state.connection.remoteAddress}:${state.connection.remotePort}",
                                    color = SafeGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is ProbeResultState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AlertRed.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.Error, "Error", tint = AlertRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.message,
                                    color = AlertRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // 5. Open Sockets Section Header & Search
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPEN CONNECTIONS (${filteredLiveConnections.size})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Tap for Security Audit",
                    color = CyberCyanPrimary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter live by IP, domain, port or app...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_search_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = CyberSurfaceCard,
                    unfocusedContainerColor = CyberSurfaceCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        // 6. Live Connection Items
        if (filteredLiveConnections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield",
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No active external sockets detected" else "No matching connections found",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Probe' above to test a live connection",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredLiveConnections,
                key = { it.connectionFingerprint + it.timestamp }
            ) { connection ->
                ConnectionCard(
                    connection = connection,
                    onClick = { viewModel.inspectConnection(connection) }
                )
            }
        }
    }

    // Detail Sheet
    inspectedConnection?.let { conn ->
        ConnectionDetailDialog(
            connection = conn,
            onDismiss = { viewModel.inspectConnection(null) },
            onAnalyzeAi = { viewModel.analyzeConnectionWithAI(conn) }
        )
    }

    // Gemini AI Threat Assessment Dialog
    if (aiAssessmentState !is com.example.ui.AiThreatAssessmentState.Idle) {
        com.example.ui.components.AiThreatAssessmentDialog(
            state = aiAssessmentState,
            onDismiss = { viewModel.dismissAiAssessment() }
        )
    }

    // Custom Probe Dialog
    if (showCustomProbeDialog) {
        CustomProbeDialog(
            onDismiss = { showCustomProbeDialog = false },
            onConfirm = { host, port ->
                showCustomProbeDialog = false
                viewModel.executeTestProbe(host, port)
            }
        )
    }
}

@Composable
fun ProbeChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceVariant)
            .border(0.5.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = CyberCyanPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CustomProbeDialog(
    onDismiss: () -> Unit,
    onConfirm: (host: String, port: Int) -> Unit
) {
    var host by remember { mutableStateOf("google.com") }
    var portText by remember { mutableStateOf("443") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Custom Probe Connection", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Test connection to a specific remote host and port to verify real-time monitoring detection:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host or IP") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyanPrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyanPrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 443
                    onConfirm(host.trim(), port)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary)
            ) {
                Text("Execute Probe", color = CyberBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = CyberSurfaceCard
    )
}
