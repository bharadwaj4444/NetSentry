package com.example.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.model.NetworkInterfaceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface
import java.util.Collections

class NetworkInterfaceTracker(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _activeTransport = MutableStateFlow("Wi-Fi")
    val activeTransport: StateFlow<String> = _activeTransport.asStateFlow()

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _isCellularConnected = MutableStateFlow(false)
    val isCellularConnected: StateFlow<Boolean> = _isCellularConnected.asStateFlow()

    private val _interfacesList = MutableStateFlow<List<NetworkInterfaceInfo>>(emptyList())
    val interfacesList: StateFlow<List<NetworkInterfaceInfo>> = _interfacesList.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStates()
        }

        override fun onLost(network: Network) {
            updateNetworkStates()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkStates()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
        }
        updateNetworkStates()
    }

    fun updateNetworkStates() {
        var wifi = false
        var cellular = false
        var primaryTransport = "Disconnected"

        try {
            val activeNet = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNet)

            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifi = true
                    primaryTransport = "Wi-Fi"
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    cellular = true
                    primaryTransport = "Cellular"
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    primaryTransport = "Ethernet"
                }
            }

            // Also check all networks for both active transports
            val allNetworks = connectivityManager.allNetworks
            for (net in allNetworks) {
                val netCaps = connectivityManager.getNetworkCapabilities(net) ?: continue
                if (netCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
                if (netCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) cellular = true
            }
        } catch (_: Exception) {
        }

        _isWifiConnected.value = wifi
        _isCellularConnected.value = cellular
        _activeTransport.value = primaryTransport

        // Refresh hardware interfaces
        refreshInterfaces()
    }

    fun refreshInterfaces() {
        val list = mutableListOf<NetworkInterfaceInfo>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val ips = Collections.list(intf.inetAddresses).mapNotNull { it.hostAddress }
                val isWifi = intf.name.startsWith("wlan") || intf.name.startsWith("wifi")
                val isCell = intf.name.startsWith("rmnet") || intf.name.startsWith("ccmni") || intf.name.startsWith("wwan")
                val mac = try {
                    intf.hardwareAddress?.joinToString(":") { String.format("%02X", it) }
                } catch (e: Exception) {
                    null
                }

                list.add(
                    NetworkInterfaceInfo(
                        name = intf.name,
                        displayName = intf.displayName,
                        ipAddresses = ips,
                        isUp = intf.isUp,
                        isLoopback = intf.isLoopback,
                        isWifi = isWifi,
                        isCellular = isCell,
                        mtu = try { intf.mtu } catch (_: Exception) { 1500 },
                        hardwareAddress = mac
                    )
                )
            }
        } catch (_: Exception) {
        }
        _interfacesList.value = list
    }
}
