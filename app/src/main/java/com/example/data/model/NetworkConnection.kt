package com.example.data.model

data class NetworkConnection(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val protocol: String = "TCP", // TCP, UDP, ICMP, etc.
    val localAddress: String = "0.0.0.0",
    val localPort: Int = 0,
    val remoteAddress: String = "0.0.0.0",
    val remotePort: Int = 0,
    val remoteHost: String = "",
    val networkType: String = "Wi-Fi", // Wi-Fi, Cellular, Ethernet, Loopback
    val interfaceName: String = "wlan0",
    val state: String = "ESTABLISHED", // ESTABLISHED, LISTEN, SYN_SENT, TIME_WAIT, CLOSED
    val uid: Int = -1,
    val appName: String = "System",
    val packageName: String = "android",
    val alertLevel: AlertLevel = AlertLevel.NORMAL,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val isOutbound: Boolean = true,
    val isNew: Boolean = false,
    val riskDetails: String = "Standard verified connection",
    val serviceName: String = getStandardServiceName(remotePort)
) {
    val connectionFingerprint: String
        get() = "$protocol:$localPort->$remoteAddress:$remotePort"

    val displayDestination: String
        get() = if (remoteHost.isNotBlank() && remoteHost != remoteAddress) {
            "$remoteHost ($remoteAddress)"
        } else {
            remoteAddress
        }

    companion object {
        fun getStandardServiceName(port: Int): String {
            return when (port) {
                80 -> "HTTP (Plaintext)"
                443 -> "HTTPS (TLS/SSL)"
                53 -> "DNS Query"
                853 -> "DNS-over-TLS"
                22 -> "SSH Secure Shell"
                21 -> "FTP File Transfer"
                25 -> "SMTP Mail"
                110 -> "POP3 Mail"
                143 -> "IMAP Mail"
                993 -> "IMAPS Secure Mail"
                123 -> "NTP Network Time"
                8080 -> "HTTP-Proxy / Alt"
                8443 -> "HTTPS-Alt"
                5228, 5229, 5230 -> "Google Play FCM"
                4433, 4434 -> "QUIC / HTTP3"
                else -> if (port > 1024) "Dynamic / Private Port" else "Port $port"
            }
        }
    }
}
