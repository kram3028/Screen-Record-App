package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors device internet connectivity and fires notifications whenever
 * internet connection becomes available.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkCurrentConnectivity())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var onConnectedListener: (() -> Unit)? = null
    private var isRegistered = false

    fun setOnConnectedListener(listener: () -> Unit) {
        this.onConnectedListener = listener
        // If already connected on attach, trigger right away if needed
        if (_isConnected.value) {
            listener.invoke()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("NetworkMonitor", "Network available")
            val currentlyConnected = checkCurrentConnectivity()
            _isConnected.value = true
            if (currentlyConnected) {
                onConnectedListener?.invoke()
            }
        }

        override fun onLost(network: Network) {
            Log.d("NetworkMonitor", "Network lost")
            _isConnected.value = checkCurrentConnectivity()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                            !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED))
            val wasConnected = _isConnected.value
            _isConnected.value = hasInternet
            if (hasInternet && !wasConnected) {
                Log.d("NetworkMonitor", "Internet validated, notifying onConnectedListener")
                onConnectedListener?.invoke()
            }
        }
    }

    fun startMonitoring() {
        if (isRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
            Log.d("NetworkMonitor", "Started monitoring network connectivity")
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Failed to register network callback", e)
        }
    }

    fun stopMonitoring() {
        if (!isRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Failed to unregister network callback", e)
        }
    }

    fun isCurrentlyConnected(): Boolean {
        return checkCurrentConnectivity()
    }

    private fun checkCurrentConnectivity(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }
}
