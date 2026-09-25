package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NetSentryApplication
import com.example.ai.GeminiSecurityAnalyzer
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection
import com.example.data.model.NetworkInterfaceInfo
import com.example.data.model.SecurityThreatAssessment
import com.example.data.model.TrafficSnapshot
import com.example.service.NetworkMonitorService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NetSentryApplication
    private val repository = app.repository
    private val engine = app.detectorEngine
    private val interfaceTracker = app.interfaceTracker
    private val aiAnalyzer = GeminiSecurityAnalyzer()

    val liveConnections: StateFlow<List<NetworkConnection>> = repository.liveConnections
    val trafficSnapshot: StateFlow<TrafficSnapshot> = engine.trafficSnapshot
    val isMonitoring: StateFlow<Boolean> = engine.isMonitoring
    val recentAlerts: StateFlow<List<NetworkConnection>> = repository.recentAlerts
    val interfacesList: StateFlow<List<NetworkInterfaceInfo>> = interfaceTracker.interfacesList

    val logCount: StateFlow<Int> = repository.logCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val suspiciousCount: StateFlow<Int> = repository.suspiciousCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Filter controls for Audit Log Screen
    val selectedProtocol = MutableStateFlow("ALL")
    val selectedNetworkType = MutableStateFlow("ALL")
    val selectedAlertLevel = MutableStateFlow("ALL")
    val searchQuery = MutableStateFlow("")

    // Real-time live filtered logs from Room
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredLogs: StateFlow<List<NetworkConnection>> = combine(
        selectedProtocol,
        selectedNetworkType,
        selectedAlertLevel,
        searchQuery
    ) { proto, netType, alertLvl, query ->
        FilterParams(proto, netType, alertLvl, query)
    }.flatMapLatest { params ->
        repository.getFilteredLogs(
            protocol = params.protocol,
            networkType = params.networkType,
            alertLevel = params.alertLevel,
            searchQuery = params.query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Rule Settings
    val alertOnNewOutbound = repository.alertOnNewOutbound
    val alertOnInsecureHttp = repository.alertOnInsecureHttp
    val alertOnSuspiciousPorts = repository.alertOnSuspiciousPorts
    val notificationsEnabled = repository.notificationsEnabled
    val hapticAlertEnabled = repository.hapticAlertEnabled
    val scanIntervalSeconds = repository.scanIntervalSeconds

    // Probe testing state
    private val _probeState = MutableStateFlow<ProbeResultState>(ProbeResultState.Idle)
    val probeState: StateFlow<ProbeResultState> = _probeState.asStateFlow()

    // Active connection inspection dialog state
    private val _inspectedConnection = MutableStateFlow<NetworkConnection?>(null)
    val inspectedConnection: StateFlow<NetworkConnection?> = _inspectedConnection.asStateFlow()

    // Export preview dialog state
    private val _exportPreview = MutableStateFlow<ExportPreviewData?>(null)
    val exportPreview: StateFlow<ExportPreviewData?> = _exportPreview.asStateFlow()

    // Gemini AI Threat Assessment state
    private val _aiAssessmentState = MutableStateFlow<AiThreatAssessmentState>(AiThreatAssessmentState.Idle)
    val aiAssessmentState: StateFlow<AiThreatAssessmentState> = _aiAssessmentState.asStateFlow()

    fun toggleMonitoring(context: Context) {
        if (isMonitoring.value) {
            engine.stopMonitoring()
            NetworkMonitorService.stopService(context)
        } else {
            engine.startMonitoring()
            NetworkMonitorService.startService(context)
        }
    }

    fun inspectConnection(connection: NetworkConnection?) {
        _inspectedConnection.value = connection
    }

    fun dismissExportPreview() {
        _exportPreview.value = null
    }

    fun dismissAiAssessment() {
        _aiAssessmentState.value = AiThreatAssessmentState.Idle
    }

    fun resetFilters() {
        selectedProtocol.value = "ALL"
        selectedNetworkType.value = "ALL"
        selectedAlertLevel.value = "ALL"
        searchQuery.value = ""
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    fun executeTestProbe(targetHost: String, targetPort: Int) {
        viewModelScope.launch {
            _probeState.value = ProbeResultState.Testing(targetHost, targetPort)
            val result = engine.executeProbeConnection(targetHost, targetPort, "TCP")
            result.onSuccess { conn ->
                _probeState.value = ProbeResultState.Success(conn)
            }.onFailure { err ->
                _probeState.value = ProbeResultState.Error(err.message ?: "Failed to connect to $targetHost:$targetPort")
            }
        }
    }

    fun resetProbeState() {
        _probeState.value = ProbeResultState.Idle
    }

    fun analyzeConnectionWithAI(connection: NetworkConnection) {
        viewModelScope.launch {
            _aiAssessmentState.value = AiThreatAssessmentState.Loading(
                "Analyzing socket destination ${connection.remoteAddress}:${connection.remotePort} (${connection.serviceName})..."
            )
            val result = aiAnalyzer.analyzeConnection(connection)
            result.onSuccess { assessment ->
                _aiAssessmentState.value = AiThreatAssessmentState.Success(
                    assessment = assessment,
                    title = "Threat Assessment: ${connection.appName} -> ${connection.displayDestination}"
                )
            }.onFailure { err ->
                _aiAssessmentState.value = AiThreatAssessmentState.Error(
                    err.message ?: "AI Threat Assessment failed."
                )
            }
        }
    }

    fun analyzeLogsBatchWithAI() {
        viewModelScope.launch {
            val logs = filteredLogs.value.ifEmpty { liveConnections.value }
            if (logs.isEmpty()) {
                _aiAssessmentState.value = AiThreatAssessmentState.Error("No connections available to analyze.")
                return@launch
            }
            _aiAssessmentState.value = AiThreatAssessmentState.Loading(
                "Gemini AI analyzing ${logs.take(25).size} connection logs for security threats & anomalies..."
            )
            val result = aiAnalyzer.analyzeAuditLogs(logs)
            result.onSuccess { assessment ->
                _aiAssessmentState.value = AiThreatAssessmentState.Success(
                    assessment = assessment,
                    title = "Executive Network Security Audit (${logs.size} connections)"
                )
            }.onFailure { err ->
                _aiAssessmentState.value = AiThreatAssessmentState.Error(
                    err.message ?: "AI Audit failed."
                )
            }
        }
    }

    fun prepareExportCsv() {
        viewModelScope.launch {
            val logs = filteredLogs.value
            val csvContent = repository.exportLogsToCsv(logs)
            _exportPreview.value = ExportPreviewData(
                format = "CSV",
                content = csvContent,
                itemCount = logs.size
            )
        }
    }

    fun prepareExportJson() {
        viewModelScope.launch {
            val logs = filteredLogs.value
            val jsonContent = repository.exportLogsToJson(logs)
            _exportPreview.value = ExportPreviewData(
                format = "JSON",
                content = jsonContent,
                itemCount = logs.size
            )
        }
    }

    fun shareExport(context: Context, format: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (format == "CSV") "text/csv" else "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "NetSentry Network Security Audit ($format)")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(intent, "Share Security Audit Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    data class FilterParams(
        val protocol: String,
        val networkType: String,
        val alertLevel: String,
        val query: String
    )
}

sealed class ProbeResultState {
    object Idle : ProbeResultState()
    data class Testing(val host: String, val port: Int) : ProbeResultState()
    data class Success(val connection: NetworkConnection) : ProbeResultState()
    data class Error(val message: String) : ProbeResultState()
}

sealed class AiThreatAssessmentState {
    object Idle : AiThreatAssessmentState()
    data class Loading(val contextMessage: String) : AiThreatAssessmentState()
    data class Success(val assessment: SecurityThreatAssessment, val title: String) : AiThreatAssessmentState()
    data class Error(val message: String) : AiThreatAssessmentState()
}

data class ExportPreviewData(
    val format: String,
    val content: String,
    val itemCount: Int
)
