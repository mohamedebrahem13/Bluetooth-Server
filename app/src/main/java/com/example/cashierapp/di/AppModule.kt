package com.example.cashierapp.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.cashierapp.data.bluetooth.GattServerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothAdapter(
        bluetoothManager: BluetoothManager
    ): BluetoothAdapter {
        return bluetoothManager.adapter
    }
    @Provides
    @Singleton
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    // Provide the GattServerManager
    @Provides
    @Singleton
    fun provideGattServerManager( @ApplicationContext context: Context): GattServerManager {
        return GattServerManager(context)
    }
}