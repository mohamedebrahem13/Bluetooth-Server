package com.example.cashierapp.data.bluetooth

import com.example.cashierapp.data.models.BluetoothDeviceDomain

sealed class ConnectionResult {
    data object Connecting : ConnectionResult()
    data class Connected (val device: BluetoothDeviceDomain): ConnectionResult()
    data object Disconnected : ConnectionResult()
}