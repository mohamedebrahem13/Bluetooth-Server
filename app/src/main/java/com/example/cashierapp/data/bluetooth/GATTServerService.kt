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
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import com.example.cashierapp.data.models.BluetoothDeviceDomain
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class GATTServerService : Service() {

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
                val bluetoothDeviceDomain = BluetoothDeviceDomain(
                    name = it.name ?: "Unnamed Device",
                    address = it.address
                )

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Device connected: ${bluetoothDeviceDomain.name}")
                    gattServerManager.updateConnectionState(
                        ConnectionResult.Connected(bluetoothDeviceDomain)
                    )
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Device disconnected: ${bluetoothDeviceDomain.address}")
                    gattServerManager.updateConnectionState(ConnectionResult.Disconnected)
                }
            } ?: run {
                Log.e(TAG, "Device is null on connection state change")
            }
        }


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
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            Log.d(TAG, "Write request from device: ${device?.address}")

            if (characteristic?.uuid == CHARACTERISTIC_UUID) {
                val receivedMessage = value?.toString(Charsets.UTF_8)
                Log.d(TAG, "Received data: $receivedMessage")

                // Handle the received data using GattServerManager
                gattServerManager.handleReceivedData(receivedMessage ?: "")

                // Update the characteristic value
                characteristic.value = "Acknowledgment: Data received".toByteArray(Charsets.UTF_8)
                Log.d(TAG, "Characteristic updated with acknowledgment")


                    val responseData = "Response Data from Server".toByteArray(Charsets.UTF_8)
                    bluetoothGattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,0,
                        responseData // Sending response data back to the client
                    )

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
    fun stopGattServer() {
        Log.d(TAG, "Stopping GATT server and BLE advertising")
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
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