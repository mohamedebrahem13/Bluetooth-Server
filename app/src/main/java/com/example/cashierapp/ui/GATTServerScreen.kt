package com.example.cashierapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GATTServerScreen(modifier: Modifier = Modifier,viewModel: GATTServerViewModel = hiltViewModel()) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val receivedData by viewModel.receivedData.collectAsState(null)
    val errorState by viewModel.errorState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Connection Status
        Text(text = "Connection Status: $connectionStatus")

        Spacer(modifier = Modifier.height(16.dp))

        // Display received data
        receivedData?.let { data ->
            Text(text = "Received Data: $data")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error State
        errorState?.let { error ->
            Text(text = "Error: $error")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to start GATT server
        Button(onClick = { viewModel.startGattServer() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Start GATT Server")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to stop GATT server
        Button(onClick = { viewModel.stopGattServer() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Stop GATT Server")
        }
    }
}