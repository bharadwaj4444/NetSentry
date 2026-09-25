package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.ConnectionCard
import com.example.ui.components.ConnectionDetailDialog
import com.example.ui.components.ExportPreviewDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NoticeYellow
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuditLogsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredLogs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val totalCount by viewModel.logCount.collectAsStateWithLifecycle()
    val suspiciousCount by viewModel.suspiciousCount.collectAsStateWithLifecycle()
    val inspectedConnection by viewModel.inspectedConnection.collectAsStateWithLifecycle()
    val exportPreview by viewModel.exportPreview.collectAsStateWithLifecycle()
    val aiAssessmentState by viewModel.aiAssessmentState.collectAsStateWithLifecycle()

    val selectedProtocol by viewModel.selectedProtocol.collectAsStateWithLifecycle()
    val selectedNetworkType by viewModel.selectedNetworkType.collectAsStateWithLifecycle()
    val selectedAlertLevel by viewModel.selectedAlertLevel.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val hasActiveFilters = selectedProtocol != "ALL" ||
            selectedNetworkType != "ALL" ||
            selectedAlertLevel != "ALL" ||
            searchQuery.isNotBlank()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Audit Stats & Export Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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
                        Column {
                            Text(
                                text = "SECURITY AUDIT LOGS",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$totalCount Events",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (suspiciousCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AlertRed.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$suspiciousCount Alerts",
                                            color = AlertRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear logs",
                                tint = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Export Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.prepareExportCsv() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "CSV",
                                tint = CyberBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", color = CyberBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.prepareExportJson() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "JSON",
                                tint = CyberCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", color = CyberCyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gemini AI Threat Assessment Button
                    Button(
                        onClick = { viewModel.analyzeLogsBatchWithAI() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_audit_batch_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.CyberSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyanPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "AI Audit",
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini AI Threat Assessment on Logs",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Granular Filtering Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = CyberCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GRANULAR FILTERS",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        if (hasActiveFilters) {
                            Text(
                                text = "Reset All",
                                color = CyberCyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.resetFilters() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Destination Address / Host / IP Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Filter by destination IP, host, port or app...", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audit_search_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyanPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = CyberSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = CyberSurfaceVariant.copy(alpha = 0.5f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Protocol Filter Chips
                    Text("Protocol:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val protocols = listOf("ALL", "TCP", "UDP", "ICMP")
                        items(protocols) { proto ->
                            FilterChip(
                                selected = selectedProtocol == proto,
                                onClick = { viewModel.selectedProtocol.value = proto },
                                label = { Text(proto, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyanPrimary,
                                    selectedLabelColor = CyberBackground,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Network Transport Filter Chips
                    Text("Network Transport:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val transports = listOf("ALL", "Wi-Fi", "Cellular")
                        items(transports) { trans ->
                            FilterChip(
                                selected = selectedNetworkType == trans,
                                onClick = { viewModel.selectedNetworkType.value = trans },
                                label = { Text(trans, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyanPrimary,
                                    selectedLabelColor = CyberBackground,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Risk Level Filter Chips
                    Text("Risk Severity:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val levels = listOf("ALL", "NORMAL", "NOTICE", "SUSPICIOUS", "HIGH_RISK")
                        items(levels) { lvl ->
                            FilterChip(
                                selected = selectedAlertLevel == lvl,
                                onClick = { viewModel.selectedAlertLevel.value = lvl },
                                label = {
                                    Text(
                                        when (lvl) {
                                            "NORMAL" -> "Secure"
                                            "NOTICE" -> "Notice"
                                            "SUSPICIOUS" -> "Suspicious"
                                            "HIGH_RISK" -> "High Risk"
                                            else -> "All Levels"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (lvl == "HIGH_RISK") AlertRed else if (lvl == "SUSPICIOUS") NoticeYellow else CyberCyanPrimary,
                                    selectedLabelColor = CyberBackground,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Log Items List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUDIT RECORDS (${filteredLogs.size})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // 4. Log Items
        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "No logs",
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (hasActiveFilters) "No connections match your filters" else "No connections logged yet",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        if (hasActiveFilters) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(onClick = { viewModel.resetFilters() }) {
                                Text("Clear Filters", color = CyberCyanPrimary)
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredLogs,
                key = { it.id }
            ) { connection ->
                ConnectionCard(
                    connection = connection,
                    onClick = { viewModel.inspectConnection(connection) }
                )
            }
        }
    }

    // Detail Dialog
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

    // Export Preview & Share Dialog
    exportPreview?.let { data ->
        ExportPreviewDialog(
            data = data,
            onDismiss = { viewModel.dismissExportPreview() },
            onShare = { format, content ->
                viewModel.shareExport(context, format, content)
            }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Audit Logs?", color = TextPrimary) },
            text = { Text("This will permanently erase all logged connection records from the device database.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllLogs()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Clear All", color = TextPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CyberSurfaceCard
        )
    }
}
