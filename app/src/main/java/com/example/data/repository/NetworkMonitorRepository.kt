package com.example.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.NetworkConnectionDao
import com.example.data.local.NetworkConnectionLogEntity
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkMonitorRepository(
    private val context: Context,
    private val dao: NetworkConnectionDao
) {

    // Active live connections from the latest scan
    private val _liveConnections = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val liveConnections: StateFlow<List<NetworkConnection>> = _liveConnections.asStateFlow()

    // Real-time alert feed (last 10 alerts)
    private val _recentAlerts = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val recentAlerts: StateFlow<List<NetworkConnection>> = _recentAlerts.asStateFlow()

    // Preferences & Rules
    val alertOnNewOutbound = MutableStateFlow(true)
    val alertOnInsecureHttp = MutableStateFlow(true)
    val alertOnSuspiciousPorts = MutableStateFlow(true)
    val notificationsEnabled = MutableStateFlow(true)
    val hapticAlertEnabled = MutableStateFlow(true)
    val scanIntervalSeconds = MutableStateFlow(2)

    val logCount: Flow<Int> = dao.getLogCountFlow()
    val suspiciousCount: Flow<Int> = dao.getSuspiciousCountFlow()

    fun getAllLogs(limit: Int = 500): Flow<List<NetworkConnection>> {
        return dao.getAllLogsFlow(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getFilteredLogs(
        protocol: String? = null,
        networkType: String? = null,
        alertLevel: String? = null,
        searchQuery: String? = null,
        limit: Int = 500
    ): Flow<List<NetworkConnection>> {
        val cleanProtocol = if (protocol == "ALL" || protocol.isNullOrBlank()) null else protocol
        val cleanType = if (networkType == "ALL" || networkType.isNullOrBlank()) null else networkType
        val cleanLevel = if (alertLevel == "ALL" || alertLevel.isNullOrBlank()) null else alertLevel
        val cleanSearch = if (searchQuery.isNullOrBlank()) null else searchQuery.trim()

        return dao.getFilteredLogsFlow(
            protocol = cleanProtocol,
            networkType = cleanType,
            alertLevel = cleanLevel,
            searchQuery = cleanSearch,
            limit = limit
        ).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun logConnection(connection: NetworkConnection) = withContext(Dispatchers.IO) {
        val entity = NetworkConnectionLogEntity.fromDomainModel(connection)
        dao.insertLog(entity)
    }

    suspend fun logConnections(connections: List<NetworkConnection>) = withContext(Dispatchers.IO) {
        val entities = connections.map { NetworkConnectionLogEntity.fromDomainModel(it) }
        dao.insertLogs(entities)
    }

    fun updateLiveConnections(connections: List<NetworkConnection>) {
        _liveConnections.value = connections
    }

    fun addRecentAlert(connection: NetworkConnection) {
        val current = _recentAlerts.value.toMutableList()
        current.add(0, connection)
        if (current.size > 20) {
            _recentAlerts.value = current.take(20)
        } else {
            _recentAlerts.value = current
        }
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        dao.clearAllLogs()
    }

    /**
     * Generates a standard RFC 4180 CSV audit export
     */
    suspend fun exportLogsToCsv(logsToExport: List<NetworkConnection>? = null): String = withContext(Dispatchers.IO) {
        val logs = logsToExport ?: dao.getAllLogsList().map { it.toDomainModel() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        val sb = StringBuilder()
        sb.append("Timestamp,DateTime,Protocol,LocalAddress,LocalPort,RemoteAddress,RemotePort,RemoteHost,NetworkType,Interface,State,AppName,PackageName,AlertLevel,RiskDetails,Service\n")

        for (conn in logs) {
            val dateStr = dateFormat.format(Date(conn.timestamp))
            val app = escapeCsv(conn.appName)
            val pkg = escapeCsv(conn.packageName)
            val host = escapeCsv(conn.remoteHost)
            val risk = escapeCsv(conn.riskDetails)
            val service = escapeCsv(conn.serviceName)

            sb.append("${conn.timestamp},$dateStr,${conn.protocol},${conn.localAddress},${conn.localPort},")
            sb.append("${conn.remoteAddress},${conn.remotePort},$host,${conn.networkType},${conn.interfaceName},")
            sb.append("${conn.state},$app,$pkg,${conn.alertLevel.name},$risk,$service\n")
        }

        sb.toString()
    }

    /**
     * Generates a comprehensive JSON security audit report
     */
    suspend fun exportLogsToJson(logsToExport: List<NetworkConnection>? = null): String = withContext(Dispatchers.IO) {
        val logs = logsToExport ?: dao.getAllLogsList().map { it.toDomainModel() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        val report = JSONObject()
        val meta = JSONObject()
        meta.put("generatedAt", dateFormat.format(Date()))
        meta.put("tool", "NetSentry Network Security Auditor")
        meta.put("totalConnectionsLogged", logs.size)

        // Protocol Breakdown
        val protocolCounts = logs.groupingBy { it.protocol }.eachCount()
        val protocolsJson = JSONObject()
        protocolCounts.forEach { (proto, count) -> protocolsJson.put(proto, count) }
        meta.put("protocolDistribution", protocolsJson)

        // Network Transport Breakdown
        val transportCounts = logs.groupingBy { it.networkType }.eachCount()
        val transportsJson = JSONObject()
        transportCounts.forEach { (type, count) -> transportsJson.put(type, count) }
        meta.put("transportDistribution", transportsJson)

        // Security Alert Summary
        val alertCounts = logs.groupingBy { it.alertLevel.name }.eachCount()
        val alertsJson = JSONObject()
        alertCounts.forEach { (level, count) -> alertsJson.put(level, count) }
        meta.put("alertLevelDistribution", alertsJson)

        // Unique remote hosts
        val uniqueHosts = logs.mapNotNull { if (it.remoteAddress != "0.0.0.0") it.remoteAddress else null }.distinct()
        meta.put("uniqueRemoteDestinationsCount", uniqueHosts.size)

        report.put("auditSummary", meta)

        val logsArray = JSONArray()
        for (conn in logs) {
            val item = JSONObject()
            item.put("timestamp", conn.timestamp)
            item.put("dateTime", dateFormat.format(Date(conn.timestamp)))
            item.put("protocol", conn.protocol)
            item.put("local", "${conn.localAddress}:${conn.localPort}")
            item.put("remote", "${conn.remoteAddress}:${conn.remotePort}")
            item.put("remoteHost", conn.remoteHost)
            item.put("networkType", conn.networkType)
            item.put("interface", conn.interfaceName)
            item.put("state", conn.state)
            item.put("app", conn.appName)
            item.put("package", conn.packageName)
            item.put("alertLevel", conn.alertLevel.name)
            item.put("service", conn.serviceName)
            item.put("riskDetails", conn.riskDetails)
            logsArray.put(item)
        }
        report.put("connections", logsArray)

        report.toString(2)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
