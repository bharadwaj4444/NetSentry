package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection

@Entity(tableName = "network_connection_logs")
data class NetworkConnectionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val protocol: String,
    val localAddress: String,
    val localPort: Int,
    val remoteAddress: String,
    val remotePort: Int,
    val remoteHost: String,
    val networkType: String,
    val interfaceName: String,
    val state: String,
    val uid: Int,
    val appName: String,
    val packageName: String,
    val alertLevel: String, // AlertLevel name
    val rxBytes: Long,
    val txBytes: Long,
    val isOutbound: Boolean,
    val riskDetails: String
) {
    fun toDomainModel(): NetworkConnection {
        return NetworkConnection(
            id = id,
            timestamp = timestamp,
            protocol = protocol,
            localAddress = localAddress,
            localPort = localPort,
            remoteAddress = remoteAddress,
            remotePort = remotePort,
            remoteHost = remoteHost,
            networkType = networkType,
            interfaceName = interfaceName,
            state = state,
            uid = uid,
            appName = appName,
            packageName = packageName,
            alertLevel = try {
                AlertLevel.valueOf(alertLevel)
            } catch (e: Exception) {
                AlertLevel.NORMAL
            },
            rxBytes = rxBytes,
            txBytes = txBytes,
            isOutbound = isOutbound,
            riskDetails = riskDetails
        )
    }

    companion object {
        fun fromDomainModel(conn: NetworkConnection): NetworkConnectionLogEntity {
            return NetworkConnectionLogEntity(
                id = conn.id,
                timestamp = conn.timestamp,
                protocol = conn.protocol,
                localAddress = conn.localAddress,
                localPort = conn.localPort,
                remoteAddress = conn.remoteAddress,
                remotePort = conn.remotePort,
                remoteHost = conn.remoteHost,
                networkType = conn.networkType,
                interfaceName = conn.interfaceName,
                state = conn.state,
                uid = conn.uid,
                appName = conn.appName,
                packageName = conn.packageName,
                alertLevel = conn.alertLevel.name,
                rxBytes = conn.rxBytes,
                txBytes = conn.txBytes,
                isOutbound = conn.isOutbound,
                riskDetails = conn.riskDetails
            )
        }
    }
}
