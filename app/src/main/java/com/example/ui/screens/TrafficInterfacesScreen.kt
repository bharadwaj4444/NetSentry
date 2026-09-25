package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NetworkInterfaceInfo
import com.example.data.model.TrafficSnapshot
import com.example.ui.MainViewModel
import com.example.ui.components.DetailRow
import com.example.ui.components.LivePulseDot
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBlueSecondary
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NoticeYellow
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TrafficInterfacesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trafficSnapshot by viewModel.trafficSnapshot.collectAsStateWithLifecycle()
    val interfacesList by viewModel.interfacesList.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dual Traffic Overview Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRAFFIC MONITORING OVERVIEW",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePulseDot(isActive = true, color = CyberCyanPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live",
                                color = SafeGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Downloaded", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = TrafficSnapshot.formatBytes(trafficSnapshot.totalRxBytes),
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Uploaded", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = TrafficSnapshot.formatBytes(trafficSnapshot.totalTxBytes),
                                color = CyberBlueSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 2. Wi-Fi Breakdown Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wifi_traffic_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CyberCyanPrimary.copy(alpha = 0.3f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyanPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Wi-Fi",
                                    tint = CyberCyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Wi-Fi Traffic", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("WLAN Interfaces (wlan0)", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        if (trafficSnapshot.activeTransport == "Wi-Fi") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SafeGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CONNECTED", color = SafeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TrafficStatPill(
                            label = "Rx Speed",
                            value = TrafficSnapshot.formatSpeed(trafficSnapshot.wifiRxRate),
                            icon = Icons.Default.ArrowDownward,
                            color = SafeGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TrafficStatPill(
                            label = "Tx Speed",
                            value = TrafficSnapshot.formatSpeed(trafficSnapshot.wifiTxRate),
                            icon = Icons.Default.ArrowUpward,
                            color = CyberBlueSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TrafficStatPill(
                            label = "Total Usage",
                            value = TrafficSnapshot.formatBytes(trafficSnapshot.wifiRxBytes + trafficSnapshot.wifiTxBytes),
                            icon = Icons.Default.Devices,
                            color = CyberCyanPrimary
                        )
                    }
                }
            }
        }

        // 3. Cellular Breakdown Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cellular_traffic_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NoticeYellow.copy(alpha = 0.3f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NoticeYellow.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CellTower,
                                    contentDescription = "Cellular",
                                    tint = NoticeYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Cellular Mobile Traffic", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("WWAN / Modem (rmnet, ccmni)", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        if (trafficSnapshot.activeTransport == "Cellular") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SafeGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CONNECTED", color = SafeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TrafficStatPill(
                            label = "Rx Speed",
                            value = TrafficSnapshot.formatSpeed(trafficSnapshot.mobileRxRate),
                            icon = Icons.Default.ArrowDownward,
                            color = SafeGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TrafficStatPill(
                            label = "Tx Speed",
                            value = TrafficSnapshot.formatSpeed(trafficSnapshot.mobileTxRate),
                            icon = Icons.Default.ArrowUpward,
                            color = CyberBlueSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TrafficStatPill(
                            label = "Total Usage",
                            value = TrafficSnapshot.formatBytes(trafficSnapshot.mobileRxBytes + trafficSnapshot.mobileTxBytes),
                            icon = Icons.Default.Devices,
                            color = NoticeYellow
                        )
                    }
                }
            }
        }

        // 4. Network Interfaces List Header
        item {
            Text(
                text = "NETWORK INTERFACES (${interfacesList.size})",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // 5. Interface Items
        items(interfacesList) { intf ->
            InterfaceItemCard(intf = intf)
        }
    }
}

@Composable
fun TrafficStatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = TextMuted, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun InterfaceItemCard(intf: NetworkInterfaceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (intf.isWifi) Icons.Default.Wifi else if (intf.isCellular) Icons.Default.CellTower else Icons.Default.Router,
                        contentDescription = intf.name,
                        tint = if (intf.isUp) CyberCyanPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = intf.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (intf.isUp) SafeGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (intf.isUp) "UP" else "DOWN",
                        color = if (intf.isUp) SafeGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (intf.ipAddresses.isNotEmpty()) {
                Text(
                    text = "IP: " + intf.ipAddresses.joinToString(", "),
                    color = CyberCyanPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(text = "No IP assigned", color = TextMuted, fontSize = 11.sp)
            }

            intf.hardwareAddress?.let { mac ->
                Text(
                    text = "MAC: $mac • MTU: ${intf.mtu}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
