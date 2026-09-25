package com.example.monitor

import android.content.Context
import android.content.pm.PackageManager
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetAddress
import java.util.Locale

object ProcNetParser {

    private val uidAppCache = mutableMapOf<Int, Pair<String, String>>()

    fun parseActiveSockets(context: Context): List<NetworkConnection> {
        val connections = mutableListOf<NetworkConnection>()

        // Scan TCP, TCP6, UDP, UDP6
        connections.addAll(parseProcFile(context, "/proc/net/tcp", "TCP"))
        connections.addAll(parseProcFile(context, "/proc/net/tcp6", "TCP"))
        connections.addAll(parseProcFile(context, "/proc/net/udp", "UDP"))
        connections.addAll(parseProcFile(context, "/proc/net/udp6", "UDP"))

        return connections
    }

    private fun parseProcFile(context: Context, filePath: String, protocol: String): List<NetworkConnection> {
        val results = mutableListOf<NetworkConnection>()
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return results

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line = reader.readLine() // skip header line
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line?.trim()?.split("\\s+".toRegex()) ?: continue
                    if (tokens.size < 10) continue

                    val localRaw = tokens[1]
                    val remoteRaw = tokens[2]
                    val stateHex = tokens[3]
                    val uid = tokens[7].toIntOrNull() ?: -1

                    val (localIp, localPort) = parseSocketAddress(localRaw)
                    val (remoteIp, remotePort) = parseSocketAddress(remoteRaw)
                    val state = decodeSocketState(stateHex)

                    // Determine app name
                    val (appName, pkgName) = resolveAppInfo(context, uid)

                    // Analyze risk level
                    val (alertLevel, riskDetails) = assessConnectionRisk(protocol, remoteIp, remotePort, state)

                    val connection = NetworkConnection(
                        timestamp = System.currentTimeMillis(),
                        protocol = protocol,
                        localAddress = localIp,
                        localPort = localPort,
                        remoteAddress = remoteIp,
                        remotePort = remotePort,
                        remoteHost = remoteIp, // Resolved asynchronously
                        networkType = if (localIp.startsWith("127.") || localIp == "::1") "Loopback" else "Active",
                        interfaceName = if (localIp.startsWith("127.")) "lo" else "net0",
                        state = state,
                        uid = uid,
                        appName = appName,
                        packageName = pkgName,
                        alertLevel = alertLevel,
                        isOutbound = remotePort != 0 && remoteIp != "0.0.0.0" && remoteIp != "::",
                        riskDetails = riskDetails
                    )

                    results.add(connection)
                }
            }
        } catch (_: Exception) {
            // Proc net read error or restricted
        }

        return results
    }

    private fun parseSocketAddress(addressHex: String): Pair<String, Int> {
        val parts = addressHex.split(":")
        if (parts.size != 2) return Pair("0.0.0.0", 0)

        val ipHex = parts[0]
        val portHex = parts[1]
        val port = portHex.toIntOrNull(16) ?: 0

        val ip = if (ipHex.length == 8) {
            // IPv4: stored in network byte order in 32-bit hex
            try {
                val b1 = ipHex.substring(6, 8).toInt(16)
                val b2 = ipHex.substring(4, 6).toInt(16)
                val b3 = ipHex.substring(2, 4).toInt(16)
                val b4 = ipHex.substring(0, 2).toInt(16)
                "$b1.$b2.$b3.$b4"
            } catch (e: Exception) {
                "0.0.0.0"
            }
        } else if (ipHex.length == 32) {
            // IPv6
            try {
                val bytes = ByteArray(16)
                for (i in 0 until 4) {
                    val word = ipHex.substring(i * 8, (i + 1) * 8)
                    bytes[i * 4 + 0] = word.substring(6, 8).toInt(16).toByte()
                    bytes[i * 4 + 1] = word.substring(4, 6).toInt(16).toByte()
                    bytes[i * 4 + 2] = word.substring(2, 4).toInt(16).toByte()
                    bytes[i * 4 + 3] = word.substring(0, 2).toInt(16).toByte()
                }
                val inet = InetAddress.getByAddress(bytes)
                inet.hostAddress ?: "::"
            } catch (e: Exception) {
                "::"
            }
        } else {
            "0.0.0.0"
        }

        return Pair(ip, port)
    }

    private fun decodeSocketState(stateHex: String): String {
        return when (stateHex.uppercase(Locale.ROOT)) {
            "01" -> "ESTABLISHED"
            "02" -> "SYN_SENT"
            "03" -> "SYN_RECV"
            "04" -> "FIN_WAIT1"
            "05" -> "FIN_WAIT2"
            "06" -> "TIME_WAIT"
            "07" -> "CLOSE"
            "08" -> "CLOSE_WAIT"
            "09" -> "LAST_ACK"
            "0A" -> "LISTEN"
            "0B" -> "CLOSING"
            else -> "UNKNOWN ($stateHex)"
        }
    }

    fun resolveAppInfo(context: Context, uid: Int): Pair<String, String> {
        if (uid <= 0) return Pair("System Kernel", "android.kernel")
        if (uid == 1000) return Pair("Android System", "android.system")

        uidAppCache[uid]?.let { return it }

        try {
            val pm = context.packageManager
            val packages = pm.getPackagesForUid(uid)
            if (!packages.isNullOrEmpty()) {
                val pkgName = packages[0]
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                val result = Pair(appLabel, pkgName)
                uidAppCache[uid] = result
                return result
            }
        } catch (_: Exception) {
        }

        val fallback = Pair("Process UID $uid", "uid.$uid")
        uidAppCache[uid] = fallback
        return fallback
    }

    fun assessConnectionRisk(
        protocol: String,
        remoteIp: String,
        remotePort: Int,
        state: String
    ): Pair<AlertLevel, String> {
        if (remoteIp == "0.0.0.0" || remoteIp == "::" || remotePort == 0) {
            return if (state == "LISTEN") {
                Pair(AlertLevel.NORMAL, "Listening for local incoming connections")
            } else {
                Pair(AlertLevel.NORMAL, "Inactive or listening socket")
            }
        }

        // Check for unencrypted HTTP
        if (remotePort == 80) {
            return Pair(AlertLevel.SUSPICIOUS, "Plaintext HTTP detected (unencrypted traffic)")
        }

        // Check for plain telnet or raw unencrypted protocols
        if (remotePort == 23) {
            return Pair(AlertLevel.HIGH_RISK, "Insecure Telnet connection (cleartext credentials)")
        }

        // Suspicious / vulnerable ports
        if (remotePort in listOf(4444, 5555, 6667, 31337)) {
            return Pair(AlertLevel.HIGH_RISK, "Known malicious / backdoor / debug port: $remotePort")
        }

        // Standard encrypted HTTPS or DNS or SSH
        if (remotePort == 443 || remotePort == 853 || remotePort == 22) {
            return Pair(AlertLevel.NORMAL, "Encrypted secure channel (${NetworkConnection.getStandardServiceName(remotePort)})")
        }

        if (remotePort == 53) {
            return Pair(AlertLevel.NORMAL, "Standard DNS Query")
        }

        // Uncommon high port
        if (remotePort > 10000) {
            return Pair(AlertLevel.NOTICE, "High ephemeral/custom outbound port ($remotePort)")
        }

        return Pair(AlertLevel.NORMAL, "Standard network connection")
    }
}
