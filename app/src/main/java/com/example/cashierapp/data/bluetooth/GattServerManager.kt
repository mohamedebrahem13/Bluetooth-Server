package com.example.cashierapp.data.bluetooth

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GattServerManager @Inject constructor(private val context: Context) {

    // Use StateFlow directly for connection state
    private val _connectionState = MutableStateFlow<ConnectionResult>(ConnectionResult.Connecting)
    val connectionState: StateFlow<ConnectionResult> = _connectionState

    // List to accumulate received orders
    private val _receivedOrders = MutableStateFlow<List<String>>(emptyList())
    val receivedOrders: StateFlow<List<String>> = _receivedOrders

    // Update connection state
    fun updateConnectionState(result: ConnectionResult) {
        Log.d("GattServerManager", "Connection state updated: $result")
        _connectionState.value = result
        Log.d("GattServerManager", "Current connection state: ${_connectionState.value}")

    }

    // Handle received data and add it to the list of orders
    fun handleReceivedData(data: String) {
        Log.d("GattServerManager", "Received data: $data")
        // Add the new data to the existing list of orders
        _receivedOrders.value += data
    }
    // Start GATT server
    fun startGattServer() {
        val intent = Intent(context, GATTServerService::class.java)
        context.startService(intent)
    }

    // Stop GATT server
    fun stopGattServer() {
        val intent = Intent(context, GATTServerService::class.java)
        context.stopService(intent)
    }
}