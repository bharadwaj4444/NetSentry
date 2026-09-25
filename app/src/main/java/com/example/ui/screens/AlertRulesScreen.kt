package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
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
fun AlertRulesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alertOnNewOutbound by viewModel.alertOnNewOutbound.collectAsStateWithLifecycle()
    val alertOnInsecureHttp by viewModel.alertOnInsecureHttp.collectAsStateWithLifecycle()
    val alertOnSuspiciousPorts by viewModel.alertOnSuspiciousPorts.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val hapticAlertEnabled by viewModel.hapticAlertEnabled.collectAsStateWithLifecycle()
    val scanIntervalSeconds by viewModel.scanIntervalSeconds.collectAsStateWithLifecycle()
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Real-Time Detection & Alerts Header
        item {
            Text(
                text = "REAL-TIME ALERT RULES",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // 2. Alert Rules Toggles Card
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
                    RuleToggleRow(
                        title = "Alert on New Outbound Connection",
                        description = "Immediate notification whenever an app or service initiates a new socket connection to an external remote address.",
                        icon = Icons.Default.Security,
                        iconTint = CyberCyanPrimary,
                        checked = alertOnNewOutbound,
                        onCheckedChange = { viewModel.alertOnNewOutbound.value = it },
                        tag = "rule_new_outbound"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderSubtle)

                    RuleToggleRow(
                        title = "Alert on Insecure / Plaintext HTTP (Port 80)",
                        description = "Flag unencrypted traffic transmitting credentials or data without TLS/SSL encryption.",
                        icon = Icons.Default.Warning,
                        iconTint = NoticeYellow,
                        checked = alertOnInsecureHttp,
                        onCheckedChange = { viewModel.alertOnInsecureHttp.value = it },
                        tag = "rule_insecure_http"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderSubtle)

                    RuleToggleRow(
                        title = "Alert on High-Risk & Suspicious Ports",
                        description = "Highlight connections to unusual ports, debug daemons (e.g. 5555), IRC (6667), or raw backdoors.",
                        icon = Icons.Default.Lock,
                        iconTint = AlertRed,
                        checked = alertOnSuspiciousPorts,
                        onCheckedChange = { viewModel.alertOnSuspiciousPorts.value = it },
                        tag = "rule_suspicious_ports"
                    )
                }
            }
        }

        // 3. Notification & Haptic Delivery Section
        item {
            Text(
                text = "ALERT NOTIFICATION DELIVERY",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

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
                    RuleToggleRow(
                        title = "System Push Notifications",
                        description = "Post high-priority heads-up notifications with remote destination details and app name.",
                        icon = Icons.Default.Notifications,
                        iconTint = CyberCyanPrimary,
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.notificationsEnabled.value = it },
                        tag = "rule_system_notifications"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderSubtle)

                    RuleToggleRow(
                        title = "Haptic Vibration Alert",
                        description = "Vibrate device with distinctive pattern when a new connection is detected.",
                        icon = Icons.Default.Vibration,
                        iconTint = SafeGreen,
                        checked = hapticAlertEnabled,
                        onCheckedChange = { viewModel.hapticAlertEnabled.value = it },
                        tag = "rule_haptic"
                    )
                }
            }
        }

        // 4. Background Performance & Scanning Section
        item {
            Text(
                text = "BACKGROUND EFFICIENCY & SCANNING",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Scan Rate",
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Monitoring Scan Interval", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Controls inspection cycle frequency and battery consumption:", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1 to "1s (Real-Time)",
                            2 to "2s (Balanced)",
                            5 to "5s (Battery Saver)"
                        ).forEach { (seconds, label) ->
                            FilterChip(
                                selected = scanIntervalSeconds == seconds,
                                onClick = { viewModel.scanIntervalSeconds.value = seconds },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyanPrimary,
                                    selectedLabelColor = CyberBackground,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    RuleToggleRow(
                        title = "Foreground Background Service",
                        description = "Keeps network sentry running continuously even when the app is minimized or device is locked.",
                        icon = Icons.Default.BatteryChargingFull,
                        iconTint = if (isMonitoring) SafeGreen else TextMuted,
                        checked = isMonitoring,
                        onCheckedChange = { viewModel.toggleMonitoring(context) },
                        tag = "rule_background_service"
                    )
                }
            }
        }

        // 5. Security & Privacy Assurance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant.copy(alpha = 0.4f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Privacy Info",
                        tint = CyberCyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Zero Cloud Leakage Guarantee",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "NetSentry inspects local Linux kernel socket descriptors (/proc/net/tcp, /proc/net/udp) and Android NetworkInterface tables strictly on-device. All audit logs are stored securely in local SQLite Room database and are never uploaded to any remote server.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberBackground,
                checkedTrackColor = CyberCyanPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CyberSurfaceVariant
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
