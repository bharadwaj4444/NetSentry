package com.example.data.model

data class TrafficSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val totalRxBytes: Long = 0,
    val totalTxBytes: Long = 0,
    val mobileRxBytes: Long = 0,
    val mobileTxBytes: Long = 0,
    val wifiRxBytes: Long = 0,
    val wifiTxBytes: Long = 0,
    // Speed rates in bytes per second
    val totalRxRate: Long = 0,
    val totalTxRate: Long = 0,
    val mobileRxRate: Long = 0,
    val mobileTxRate: Long = 0,
    val wifiRxRate: Long = 0,
    val wifiTxRate: Long = 0,
    val activeTransport: String = "Wi-Fi",
    val activeInterface: String = "wlan0"
) {
    companion object {
        fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
                bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
                else -> "$bytesPerSec B/s"
            }
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}

data class NetworkInterfaceInfo(
    val name: String,
    val displayName: String,
    val ipAddresses: List<String>,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val isWifi: Boolean,
    val isCellular: Boolean,
    val mtu: Int,
    val hardwareAddress: String?
)
