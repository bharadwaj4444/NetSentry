package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection
import com.example.data.model.TrafficSnapshot
import com.example.ui.ExportPreviewData
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderCyanGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBlueSecondary
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.InfoSky
import com.example.ui.theme.NoticeYellow
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LivePulseDot(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    color: Color = CyberCyanPrimary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alphaAnim * 0.4f))
            )
        }
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isActive) color else TextMuted)
        )
    }
}

@Composable
fun ProtocolBadge(protocol: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (protocol.uppercase(Locale.ROOT)) {
        "TCP" -> Pair(CyberBlueSecondary.copy(alpha = 0.2f), CyberBlueSecondary)
        "UDP" -> Pair(CyberCyanPrimary.copy(alpha = 0.2f), CyberCyanPrimary)
        "ICMP" -> Pair(NoticeYellow.copy(alpha = 0.2f), NoticeYellow)
        else -> Pair(CyberSurfaceVariant, TextSecondary)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(0.5.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = protocol,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AlertLevelBadge(level: AlertLevel, modifier: Modifier = Modifier) {
    val (color, label) = when (level) {
        AlertLevel.NORMAL -> Pair(SafeGreen, "SECURE")
        AlertLevel.NOTICE -> Pair(InfoSky, "NOTICE")
        AlertLevel.SUSPICIOUS -> Pair(NoticeYellow, "SUSPICIOUS")
        AlertLevel.HIGH_RISK -> Pair(AlertRed, "HIGH RISK")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StateBadge(state: String, modifier: Modifier = Modifier) {
    val color = when (state.uppercase(Locale.ROOT)) {
        "ESTABLISHED" -> SafeGreen
        "LISTEN" -> InfoSky
        "TIME_WAIT", "SYN_SENT" -> NoticeYellow
        "CLOSED" -> TextMuted
        else -> TextSecondary
    }

    Text(
        text = state,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}

@Composable
fun ConnectionCard(
    connection: NetworkConnection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val isRisk = connection.alertLevel == AlertLevel.SUSPICIOUS || connection.alertLevel == AlertLevel.HIGH_RISK

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_card_${connection.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRisk) CyberSurfaceCard.copy(alpha = 0.95f) else CyberSurfaceCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (connection.isNew) CyberCyanPrimary.copy(alpha = 0.7f)
                else if (isRisk) AlertRed.copy(alpha = 0.5f)
                else BorderSubtle
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Protocol, App Name, Network Type, Alert Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProtocolBadge(protocol = connection.protocol)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = connection.appName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (connection.isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberCyanPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("NEW", color = CyberCyanPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    AlertLevelBadge(level = connection.alertLevel)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Destination Host & Port
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "-> ",
                    color = CyberCyanPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${connection.displayDestination}:${connection.remotePort}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                StateBadge(state = connection.state)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sub info row: Service, Interface & Transport, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${connection.serviceName} • ${connection.networkType}",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Text(
                    text = timeFormat.format(Date(connection.timestamp)),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TrafficThroughputCard(
    snapshot: TrafficSnapshot,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    LivePulseDot(isActive = true, color = CyberCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE NETWORK TRAFFIC",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Active: ${snapshot.activeTransport}",
                    color = CyberCyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Wi-Fi column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi",
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wi-Fi", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download",
                            tint = SafeGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = TrafficSnapshot.formatSpeed(snapshot.wifiRxRate),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload",
                            tint = CyberBlueSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = TrafficSnapshot.formatSpeed(snapshot.wifiTxRate),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Cellular column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = "Cellular",
                            tint = NoticeYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cellular", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download",
                            tint = SafeGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = TrafficSnapshot.formatSpeed(snapshot.mobileRxRate),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload",
                            tint = CyberBlueSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = TrafficSnapshot.formatSpeed(snapshot.mobileTxRate),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionDetailDialog(
    connection: NetworkConnection,
    onDismiss: () -> Unit,
    onAnalyzeAi: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Audit",
                        tint = CyberCyanPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connection Audit",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                AlertLevelBadge(level = connection.alertLevel)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailRow("Remote Host", connection.remoteHost.ifBlank { "Unresolved" })
                DetailRow("Remote IP:Port", "${connection.remoteAddress}:${connection.remotePort}")
                DetailRow("Standard Service", connection.serviceName)
                DetailRow("Protocol", connection.protocol)
                DetailRow("Local Endpoint", "${connection.localAddress}:${connection.localPort}")
                DetailRow("Socket State", connection.state)
                DetailRow("Network Transport", "${connection.networkType} (${connection.interfaceName})")
                DetailRow("Application", connection.appName)
                DetailRow("Package Name", connection.packageName)
                DetailRow("Process UID", connection.uid.toString())
                DetailRow("Observed At", dateFormat.format(Date(connection.timestamp)))

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "HEURISTIC ASSESSMENT",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = connection.riskDetails,
                    color = if (connection.alertLevel == AlertLevel.NORMAL) SafeGreen else NoticeYellow,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close", color = TextSecondary)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onAnalyzeAi()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("ai_assess_single_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "AI",
                        tint = CyberBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Assessment", color = CyberBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = CyberSurfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AiThreatAssessmentDialog(
    state: com.example.ui.AiThreatAssessmentState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyberCyanPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Gemini AI",
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini Threat Intel",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state is com.example.ui.AiThreatAssessmentState.Success) {
                    val threatLevel = state.assessment.threatLevel.uppercase(Locale.ROOT)
                    val color = when (threatLevel) {
                        "CLEAN" -> SafeGreen
                        "LOW" -> InfoSky
                        "MEDIUM" -> NoticeYellow
                        "HIGH", "CRITICAL" -> AlertRed
                        else -> TextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.2f))
                            .border(0.5.dp, color, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = threatLevel,
                            color = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            when (state) {
                is com.example.ui.AiThreatAssessmentState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = CyberCyanPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Consulting Gemini Cyber Intelligence...",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.contextMessage,
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                is com.example.ui.AiThreatAssessmentState.Success -> {
                    val assessment = state.assessment
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = state.title,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Headline Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = assessment.headline,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Summary
                        Text(
                            text = "SECURITY ANALYSIS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = assessment.summary,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        // Threat Vectors
                        if (assessment.threatVectors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "IDENTIFIED THREAT VECTORS",
                                color = AlertRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            assessment.threatVectors.forEach { vector ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Threat",
                                        tint = AlertRed,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = vector,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Recommendations
                        if (assessment.recommendations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "RECOMMENDED MITIGATION STEPS",
                                color = SafeGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            assessment.recommendations.forEach { rec ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Recommendation",
                                        tint = SafeGreen,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rec,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                is com.example.ui.AiThreatAssessmentState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Assessment Failed",
                                color = AlertRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tip: Make sure GEMINI_API_KEY is configured in the Secrets panel in AI Studio.",
                            color = CyberCyanPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                else -> {}
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state is com.example.ui.AiThreatAssessmentState.Success) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = "NetSentry Gemini AI Threat Report:\nHeadline: ${state.assessment.headline}\nThreat Level: ${state.assessment.threatLevel}\nSummary: ${state.assessment.summary}\nThreat Vectors: ${state.assessment.threatVectors.joinToString()}\nRecommendations:\n${state.assessment.recommendations.joinToString("\n- ")}"
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Threat Assessment", clipText))
                            Toast.makeText(context, "Assessment copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary)
                ) {
                    Text("Close", color = CyberBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = CyberSurfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun ExportPreviewDialog(
    data: ExportPreviewData,
    onDismiss: () -> Unit,
    onShare: (format: String, content: String) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Export ${data.format} (${data.itemCount} logs)",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Security Audit Export ready for external inspection, compliance analysis, or SIEM ingestion.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = data.content,
                        color = CyberCyanPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("NetSentry Audit", data.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }

                Button(
                    onClick = { onShare(data.format, data.content) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = CyberBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = CyberBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = CyberSurfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}
