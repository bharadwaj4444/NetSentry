package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkConnectionDao {

    @Query("SELECT * FROM network_connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getAllLogsFlow(limit: Int = 500): Flow<List<NetworkConnectionLogEntity>>

    @Query("SELECT * FROM network_connection_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<NetworkConnectionLogEntity>

    @Query("""
        SELECT * FROM network_connection_logs 
        WHERE (:protocol IS NULL OR protocol = :protocol)
          AND (:networkType IS NULL OR networkType = :networkType)
          AND (:alertLevel IS NULL OR alertLevel = :alertLevel)
          AND (:searchQuery IS NULL OR remoteAddress LIKE '%' || :searchQuery || '%' 
               OR remoteHost LIKE '%' || :searchQuery || '%' 
               OR appName LIKE '%' || :searchQuery || '%'
               OR CAST(remotePort AS TEXT) LIKE '%' || :searchQuery || '%')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getFilteredLogsFlow(
        protocol: String? = null,
        networkType: String? = null,
        alertLevel: String? = null,
        searchQuery: String? = null,
        limit: Int = 500
    ): Flow<List<NetworkConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NetworkConnectionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<NetworkConnectionLogEntity>)

    @Query("DELETE FROM network_connection_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM network_connection_logs")
    fun getLogCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM network_connection_logs WHERE alertLevel IN ('SUSPICIOUS', 'HIGH_RISK')")
    fun getSuspiciousCountFlow(): Flow<Int>
}
