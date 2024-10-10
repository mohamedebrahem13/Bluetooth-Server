package com.example.cashierapp.data.bluetooth

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GattServerManager @Inject constructor(private val context: Context) {

    // Map to track the connection state for each device
    private val _connectionStates = MutableStateFlow<Map<String, ConnectionResult>>(emptyMap())
    val connectionStates: StateFlow<Map<String, ConnectionResult>> = _connectionStates
    // List to accumulate received orders
    private val _receivedOrders = MutableStateFlow<List<String>>(emptyList())
    val receivedOrders: StateFlow<List<String>> = _receivedOrders

    // Update the connection state for a specific device
    fun updateConnectionState(deviceAddress: String, result: ConnectionResult) {
        Log.d("GattServerManager", "Connection state updated for $deviceAddress: $result")
        val updatedStates = _connectionStates.value.toMutableMap()
        updatedStates[deviceAddress] = result
        _connectionStates.value = updatedStates
        Log.d("GattServerManager", "Current connection states: ${_connectionStates.value}")
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