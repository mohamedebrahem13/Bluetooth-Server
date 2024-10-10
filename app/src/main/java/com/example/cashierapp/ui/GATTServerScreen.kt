package com.example.cashierapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GATTServerScreen(modifier: Modifier = Modifier, viewModel: GATTServerViewModel = hiltViewModel()) {
    val connectionStatus by viewModel.connectionStates.collectAsState()
    val receivedData by viewModel.receivedData.collectAsState()  // List of orders received
    val errorState by viewModel.errorState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Display connection status
        Text(text = "Connection Status: $connectionStatus")

        Spacer(modifier = Modifier.height(16.dp))

        // Display error if available
        errorState?.let { error ->
            Text(text = "Error: $error")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn for the received data (orders) - scrollable
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(receivedData) { order ->
                Text(text = "Order: $order")
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}