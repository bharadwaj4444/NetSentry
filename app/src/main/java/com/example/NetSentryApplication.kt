package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.NetworkMonitorRepository
import com.example.monitor.ConnectionDetectorEngine
import com.example.monitor.NetworkInterfaceTracker

class NetSentryApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: NetworkMonitorRepository
        private set

    lateinit var interfaceTracker: NetworkInterfaceTracker
        private set

    lateinit var detectorEngine: ConnectionDetectorEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = NetworkMonitorRepository(this, database.networkConnectionDao())
        interfaceTracker = NetworkInterfaceTracker(this)
        detectorEngine = ConnectionDetectorEngine(this, repository, interfaceTracker)
    }
}
