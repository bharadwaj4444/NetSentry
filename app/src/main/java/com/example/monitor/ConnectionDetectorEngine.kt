package com.example.monitor

import android.content.Context
import android.net.TrafficStats
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection
import com.example.data.model.TrafficSnapshot
import com.example.data.repository.NetworkMonitorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class ConnectionDetectorEngine(
    private val context: Context,
    private val repository: NetworkMonitorRepository,
    private val interfaceTracker: NetworkInterfaceTracker
) {
    private val engineScope = CoroutineScope(Dispatchers.Default + Job())

    private val _isMonitoring = MutableStateFlow(true)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _trafficSnapshot = MutableStateFlow(TrafficSnapshot())
    val trafficSnapshot: StateFlow<TrafficSnapshot> = _trafficSnapshot.asStateFlow()

    // Shared flow to broadcast real-time alert events to listeners (e.g., UI snackbar/banner, service notifications)
    private val _newConnectionAlerts = MutableSharedFlow<NetworkConnection>(replay = 0, extraBufferCapacity = 64)
    val newConnectionAlerts: SharedFlow<NetworkConnection> = _newConnectionAlerts.asSharedFlow()

    // Cache of seen connections
    private val knownFingerprints = mutableSetOf<String>()
    private val dnsCache = mutableMapOf<String, String>()

    // Last traffic readings for speed calculations
    private var lastTrafficTimestamp = System.currentTimeMillis()
    private var lastTotalRx = TrafficStats.getTotalRxBytes()
    private var lastTotalTx = TrafficStats.getTotalTxBytes()
    private var lastMobileRx = TrafficStats.getMobileRxBytes()
    private var lastMobileTx = TrafficStats.getMobileTxBytes()

    private var monitorJob: Job? = null

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        _isMonitoring.value = true

        monitorJob = engineScope.launch {
            while (isActive && _isMonitoring.value) {
                try {
                    performScanCycle()
                } catch (_: Exception) {
                }
                val interval = repository.scanIntervalSeconds.value.coerceIn(1, 10) * 1000L
                delay(interval)
            }
        }
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun performScanCycle() {
        // 1. Update Traffic Snapshot & Rates
        updateTrafficStats()

        // 2. Scan active sockets from /proc/net
        val rawSockets = withContext(Dispatchers.IO) {
            ProcNetParser.parseActiveSockets(context)
        }

        val activeTransport = interfaceTracker.activeTransport.value
        val enhancedConnections = mutableListOf<NetworkConnection>()

        for (conn in rawSockets) {
            val netType = if (conn.localAddress.startsWith("127.") || conn.localAddress == "::1") {
                "Loopback"
            } else {
                activeTransport
            }

            // Resolve hostname if external
            val resolvedHost = if (conn.remoteAddress != "0.0.0.0" && conn.remoteAddress != "::" && !conn.remoteAddress.startsWith("127.")) {
                resolveRemoteHost(conn.remoteAddress)
            } else {
                conn.remoteAddress
            }

            val fingerprint = "${conn.protocol}:${conn.localPort}->${conn.remoteAddress}:${conn.remotePort}"
            val isNewConnection = !knownFingerprints.contains(fingerprint) && conn.isOutbound

            val updatedConn = conn.copy(
                networkType = netType,
                remoteHost = resolvedHost,
                isNew = isNewConnection
            )
            enhancedConnections.add(updatedConn)

            if (isNewConnection) {
                knownFingerprints.add(fingerprint)
                handleNewConnectionDetected(updatedConn)
            }
        }

        // Update repository live view
        repository.updateLiveConnections(enhancedConnections)
    }

    private suspend fun handleNewConnectionDetected(connection: NetworkConnection) {
        var shouldAlert = false

        if (repository.alertOnNewOutbound.value && connection.isOutbound) {
            shouldAlert = true
        }
        if (repository.alertOnInsecureHttp.value && connection.remotePort == 80) {
            shouldAlert = true
        }
        if (repository.alertOnSuspiciousPorts.value && connection.alertLevel != AlertLevel.NORMAL) {
            shouldAlert = true
        }

        // Persist to Room
        repository.logConnection(connection)

        if (shouldAlert) {
            repository.addRecentAlert(connection)
            _newConnectionAlerts.emit(connection)

            if (repository.hapticAlertEnabled.value) {
                triggerHapticFeedback()
            }
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 120, 80, 120),
                            intArrayOf(0, 180, 0, 220),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun updateTrafficStats() {
        val now = System.currentTimeMillis()
        val deltaMs = (now - lastTrafficTimestamp).coerceAtLeast(100)
        val deltaSec = deltaMs / 1000.0

        val currentTotalRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0)
        val currentTotalTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
        val currentMobileRx = TrafficStats.getMobileRxBytes().coerceAtLeast(0)
        val currentMobileTx = TrafficStats.getMobileTxBytes().coerceAtLeast(0)

        // Wi-Fi traffic estimated by subtracting mobile from total
        val currentWifiRx = (currentTotalRx - currentMobileRx).coerceAtLeast(0)
        val currentWifiTx = (currentTotalTx - currentMobileTx).coerceAtLeast(0)

        val totalRxRate = if (lastTotalRx > 0) ((currentTotalRx - lastTotalRx).coerceAtLeast(0) / deltaSec).toLong() else 0L
        val totalTxRate = if (lastTotalTx > 0) ((currentTotalTx - lastTotalTx).coerceAtLeast(0) / deltaSec).toLong() else 0L
        val mobileRxRate = if (lastMobileRx > 0) ((currentMobileRx - lastMobileRx).coerceAtLeast(0) / deltaSec).toLong() else 0L
        val mobileTxRate = if (lastMobileTx > 0) ((currentMobileTx - lastMobileTx).coerceAtLeast(0) / deltaSec).toLong() else 0L
        val wifiRxRate = ((totalRxRate - mobileRxRate).coerceAtLeast(0))
        val wifiTxRate = ((totalTxRate - mobileTxRate).coerceAtLeast(0))

        lastTrafficTimestamp = now
        lastTotalRx = currentTotalRx
        lastTotalTx = currentTotalTx
        lastMobileRx = currentMobileRx
        lastMobileTx = currentMobileTx

        _trafficSnapshot.value = TrafficSnapshot(
            timestamp = now,
            totalRxBytes = currentTotalRx,
            totalTxBytes = currentTotalTx,
            mobileRxBytes = currentMobileRx,
            mobileTxBytes = currentMobileTx,
            wifiRxBytes = currentWifiRx,
            wifiTxBytes = currentWifiTx,
            totalRxRate = totalRxRate,
            totalTxRate = totalTxRate,
            mobileRxRate = mobileRxRate,
            mobileTxRate = mobileTxRate,
            wifiRxRate = wifiRxRate,
            wifiTxRate = wifiTxRate,
            activeTransport = interfaceTracker.activeTransport.value
        )
    }

    private suspend fun resolveRemoteHost(ip: String): String = withContext(Dispatchers.IO) {
        dnsCache[ip]?.let { return@withContext it }
        try {
            val addr = InetAddress.getByName(ip)
            val hostName = addr.canonicalHostName
            if (hostName != ip) {
                dnsCache[ip] = hostName
                return@withContext hostName
            }
        } catch (_: Exception) {
        }
        ip
    }

    /**
     * Diagnostic Probe Tool:
     * Initiates a real TCP outbound connection probe to a test target (e.g., 8.8.8.8:53, 1.1.1.1:443, or custom host:port)
     * so that the user can immediately observe real-time detection, alerting, and log persistence!
     */
    suspend fun executeProbeConnection(targetHost: String, targetPort: Int, protocol: String = "TCP"): Result<NetworkConnection> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            val socketAddress = InetSocketAddress(targetHost, targetPort)
            socket.connect(socketAddress, 3000)

            val localIp = socket.localAddress?.hostAddress ?: "0.0.0.0"
            val localPort = socket.localPort
            val remoteIp = socket.inetAddress?.hostAddress ?: targetHost

            socket.close()

            val (alertLevel, riskDetails) = ProcNetParser.assessConnectionRisk(protocol, remoteIp, targetPort, "ESTABLISHED")
            val conn = NetworkConnection(
                timestamp = startTime,
                protocol = protocol,
                localAddress = localIp,
                localPort = localPort,
                remoteAddress = remoteIp,
                remotePort = targetPort,
                remoteHost = targetHost,
                networkType = interfaceTracker.activeTransport.value,
                interfaceName = if (interfaceTracker.activeTransport.value == "Wi-Fi") "wlan0" else "rmnet0",
                state = "ESTABLISHED",
                uid = android.os.Process.myUid(),
                appName = "NetSentry Probe",
                packageName = context.packageName,
                alertLevel = alertLevel,
                isOutbound = true,
                isNew = true,
                riskDetails = "Outbound active probe ($riskDetails)"
            )

            // Trigger immediate scan & alert
            val fingerprint = "$protocol:$localPort->$remoteIp:$targetPort"
            knownFingerprints.add(fingerprint)
            handleNewConnectionDetected(conn)

            // Also immediately update live connections
            val updatedList = repository.liveConnections.value.toMutableList()
            updatedList.add(0, conn)
            repository.updateLiveConnections(updatedList)

            Result.success(conn)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
