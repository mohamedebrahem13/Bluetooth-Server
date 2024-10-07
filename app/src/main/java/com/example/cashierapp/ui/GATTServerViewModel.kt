package com.example.cashierapp.ui

import androidx.lifecycle.ViewModel
import com.example.cashierapp.data.bluetooth.ConnectionResult
import com.example.cashierapp.data.bluetooth.GattServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GATTServerViewModel @Inject constructor(
    private val gattServerManager: GattServerManager
) : ViewModel() {

    // Directly expose the connectionState and receivedData from GattServerManager
    val connectionStatus: StateFlow<ConnectionResult> = gattServerManager.connectionState
    val receivedData: StateFlow<List<String>> = gattServerManager.receivedOrders

    // Error state
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    fun startGattServer() {
        gattServerManager.startGattServer()
    }

    fun stopGattServer() {
        gattServerManager.stopGattServer()
    }

    // Clear error state
    private fun clearError() {
        _errorState.value = null
    }
    // Called when the ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        // Stop GATT server to clean up resources
        stopGattServer()
        clearError()
    }
}