package com.example.cashierapp.data.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.cashierapp.data.models.BluetoothDeviceDomain
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class GATTServerService : Service() {
    private val dataBuffer = mutableMapOf<String, StringBuilder>() // Keep track of incoming chunks per device
    private val connectedDevices = mutableMapOf<String, BluetoothDevice>()  // Track connected devices


    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("00002222-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb")
        val CCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val TAG = "GATTServerService"
    }

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    @Inject
    lateinit var gattServerManager: GattServerManager  // Inject GattServerManager

    private lateinit var bluetoothGattServer: BluetoothGattServer
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser


    override fun onCreate() {
        super.onCreate()
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        startAdvertising()
        startGattServer()
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed with error code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(true)
            .build()

        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
    }


    @SuppressLint("MissingPermission")
    private fun startGattServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val characteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE,  // Add PROPERTY_NOTIFY
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Create the Client Characteristic Configuration Descriptor (CCD)
        val ccdDescriptor = BluetoothGattDescriptor(
            CCD_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        characteristic.addDescriptor(ccdDescriptor)

        // Add the characteristic to the service
        service.addCharacteristic(characteristic)

        // Add the service to the GATT server
        bluetoothGattServer.addService(service)

        Log.d(TAG, "GATT server started with CCD")
    }

    @SuppressLint("MissingPermission")
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)

            device?.let {
                val deviceAddress = it.address

                when (newState) {
                    BluetoothProfile.STATE_CONNECTING -> {
                        Log.d(TAG, "Device connecting: $deviceAddress")
                        gattServerManager.updateConnectionState(deviceAddress, ConnectionResult.Connecting)
                    }
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Device connected: ${it.name ?: "Unnamed"} - $deviceAddress")
                        connectedDevices[deviceAddress] = it  // Track connected device
                        gattServerManager.updateConnectionState(deviceAddress, ConnectionResult.Connected(BluetoothDeviceDomain(it.name ?: "Unnamed Device", deviceAddress)))
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Device disconnected: $deviceAddress")
                        connectedDevices.remove(deviceAddress)  // Remove device from tracking
                        dataBuffer.remove(deviceAddress)  // Clear buffer
                        gattServerManager.updateConnectionState(deviceAddress, ConnectionResult.Disconnected)
                    }
                    else -> {
                        Log.d(TAG, "Unknown connection state for $deviceAddress")
                    }
                }
            } ?: run {
                Log.e(TAG, "Device is null on connection state change")
            }
        }


        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)

            Log.d(TAG, "Write request from device: ${device?.address}")

            if (characteristic?.uuid == CHARACTERISTIC_UUID && device != null) {
                val deviceAddress = device.address
                val receivedMessage = value?.toString(Charsets.UTF_8) ?: ""

                // Append the received chunk to the buffer for this device
                val buffer = dataBuffer.getOrPut(deviceAddress) { StringBuilder() }
                buffer.append(receivedMessage)

                Log.d(TAG, "Buffered data for $deviceAddress: $buffer")

                // Check if the buffer contains the "END" marker indicating the complete message has been received
                if (buffer.contains("END")) {
                    // Complete message received, remove "END" marker
                    val completeData = buffer.toString().substringBefore("END")  // Get everything before "END"

                    Log.d(TAG, "Complete data received: $completeData")

                    // Process the complete data as one shot
                    gattServerManager.handleReceivedData(completeData)

                    // Send complete order details as a notification to the client
                    val responseData = "Order Process Complete: Details for $completeData"
                    sendManualDataToClient(device, responseData)

                    // Clear the buffer for this device after processing
                    dataBuffer.remove(deviceAddress)
                }

                // Send acknowledgment to the client
                val acknowledgment = "Acknowledgment: Data received"
                characteristic.value = acknowledgment.toByteArray(Charsets.UTF_8)

                if (responseNeeded) {
                    bluetoothGattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        acknowledgment.toByteArray(Charsets.UTF_8)
                    )
                }
            }
        }


        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)

            if (descriptor?.uuid == CCD_UUID) {
                Log.d(TAG, "Descriptor write request for notifications")

                if (value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                        value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    Log.d(TAG, "Notifications enabled for ${device?.address}")
                } else {
                    Log.d(TAG, "Notifications disabled for ${device?.address}")
                }


                    bluetoothGattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )

            }

        }


}
    @SuppressLint("MissingPermission")
    fun sendManualDataToClient(device: BluetoothDevice?, data: String) {
        val characteristic = bluetoothGattServer.getService(SERVICE_UUID)
            ?.getCharacteristic(CHARACTERISTIC_UUID)

        if (characteristic != null) {
            val dataBytes = (data + "END").toByteArray(Charsets.UTF_8)  // Add terminator "END"

            // Break data into chunks if it's too large
            val maxPacketSize = 20
            val chunkedData = dataBytes.toList().chunked(maxPacketSize)

            for (chunk in chunkedData) {
                val chunkArray = chunk.toByteArray()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        val success: Int = bluetoothGattServer.notifyCharacteristicChanged(device!!, characteristic, false, chunkArray)
                        if (success == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(TAG, "Data chunk sent successfully to the client.")
                        } else {
                            Log.e(TAG, "Failed to send data chunk to the client. Error code: $success")
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Error sending data chunk: ${e.message}")
                    }
                } else {
                    try {
                        val success: Boolean = bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false)
                        if (success) {
                            Log.d(TAG, "Data chunk sent successfully to the client on lower API.")
                        } else {
                            Log.e(TAG, "Failed to send data chunk to the client on lower API.")
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Error sending data chunk on lower API: ${e.message}")
                    }
                }
            }
        } else {
            Log.e(TAG, "Characteristic not found or not initialized.")
        }
    }


    @SuppressLint("MissingPermission")
    fun stopGattServer() {
        Log.d(TAG, "Stopping GATT server and BLE advertising")

        // Stop advertising
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)

        // Close the GATT server
        bluetoothGattServer.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGattServer()
        Log.d(TAG, "GATT server stopped")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}