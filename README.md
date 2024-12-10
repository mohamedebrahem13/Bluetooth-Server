Bluetooth Low Energy (BLE) Overview
1. What is Bluetooth Low Energy (BLE)?
Bluetooth Low Energy (BLE) is a wireless communication technology designed to provide 
reduced power consumption while maintaining communication range and performance. It 
is especially useful for devices that transmit small amounts of data and need to operate on 
battery for extended periods, such as fitness trackers, smartwatches, or mobile app 
peripherals.
Key Features of BLE:
- Low Power Consumption: BLE devices use less energy compared to traditional Bluetooth, 
making it suitable for devices requiring long battery life.
- Short-Range Communication: BLE typically operates over a range of up to 100 meters, 
depending on the environment.
- Small Data Transfers: BLE is optimized for short data bursts, making it ideal for use cases 
like sensor data collection, notifications, or small message exchanges.
2. BLE Architecture: GATT (Generic Attribute Profile)
The Generic Attribute Profile (GATT) is the underlying protocol used by BLE to send and 
receive small data packets between two devices. BLE follows a client-server model, where:
- Client: The device that requests data (in your case, the Order App).
- Server: The device that holds the data or provides services (the Cashier App).
Components of GATT:
- Services: A service is a collection of characteristics that represent a specific functionality. 
Each service is identified by a unique UUID.
- Characteristics: A characteristic is a data point within a service. It contains the actual data 
and can have properties like Read, Write, Notify, or Indicate.
- Descriptors: These provide additional information about the characteristic, such as 
enabling or disabling notifications.
Key GATT Operations:
- Reading a Characteristic: The client reads the value of a characteristic from the server.
- Writing a Characteristic: The client writes data to the server by writing to a characteristic.
- Notifications/Indications: The server sends updates to the client without the client having 
to ask for them. Notifications do not require acknowledgment, whereas indications do.
GATT Server Service in the Cashier App
1. Service Overview
The `GATTServerService` creates and runs the GATT server, which manages the connection 
and communication with BLE clients. This server allows the Order App to send orders and 
receive acknowledgments through BLE.
Core Components of the Service:
- BluetoothLeAdvertiser: This component advertises the server so that nearby devices can 
discover and connect to it.
- BluetoothGattServer: The server that handles BLE connections and manages GATT 
services and characteristics.
- GATT Characteristics and Service:
 - Service UUID: `00002222-0000-1000-8000-00805f9b34fb`
 - Characteristic UUID: `00001111-0000-1000-8000-00805f9b34fb` (used for both writing 
order data and sending notifications).
 - Client Characteristic Configuration Descriptor (CCD): `00002902-0000-1000-8000-
00805f9b34fb` (used to enable/disable notifications).
2. Advertising Process
Advertising the GATT Server
When the service starts, the BluetoothLeAdvertiser is initialized to start advertising the 
server to nearby clients.
- Advertise Settings: Defines how the server will advertise itself, with parameters like 
advertise mode (low latency) and transmission power.
- Advertise Data: This includes the service UUID (`00002222-0000-1000-8000-
00805f9b34fb`) and device name, allowing clients to discover the service.
- Advertise Callback: Handles the results of advertising, logging whether advertising was 
successful or failed.
3. GATT Service and Characteristics Setup
When the GATT server is started, it sets up the custom service and its characteristic.
- Service: The service is identified by its Service UUID and acts as the main point of 
communication between the server and client.
- Characteristic: The characteristic is identified by its Characteristic UUID (`00001111-
0000-1000-8000-00805f9b34fb`) and supports multiple operations:
 - Read: The client can read data from the server.
 - Write: The client writes order data to the server using this characteristic.
 - Notify/Indicate: The server can send real-time updates or confirmation back to the client 
when order processing is complete.
- Descriptor (CCD): The Client Characteristic Configuration Descriptor (CCD) enables or 
disables notifications or indications for the client.
4. Handling Callbacks in the GATT Server
Connection State Changes:
onConnectionStateChange: This callback is triggered whenever a client connects to or 
disconnects from the server.
- Connected: Logs when a client connects and updates the connection state.
- Disconnected: Handles client disconnection events, stopping notifications and cleaning up 
resources.
Handling Write Requests:
onCharacteristicWriteRequest: This is called when the client sends order data to the server. 
The server:
- Logs the received data.
- Processes the order and sends an acknowledgment back to the client.
- Optionally sends additional data (e.g., "Order Process Complete") as a notification to the 
client.
Handling Descriptor Write Requests:
onDescriptorWriteRequest: This is triggered when the client attempts to enable or disable 
notifications/indications. The server acknowledges the request and updates the notification 
status.
5. Sending Notifications to the Client
Once the server receives and processes the order, it can send notifications or indications 
back to the client.
- Notification/Indication: The `notifyCharacteristicChanged` method sends data back to the 
client. In your implementation, if the data is too large (exceeding the BLE packet size of 20 
bytes), it is split into smaller chunks.
- Data Chunking: Data is sent in multiple packets if necessary, with a termination marker 
like "END" to signal the end of the message.
6. Reconnection and Disconnection Handling
The GATTServerService handles reconnection and disconnection events:
- Reconnection: When the client goes out of range and comes back, it automatically 
reconnects (managed by the client’s auto-reconnect mechanism).
- Disconnection: The server properly handles client disconnection events by cleaning up 
resources and stopping notifications.
7. Stopping the GATT Server
When the service is stopped, the GATT server is also gracefully stopped:
- Stop Advertising: The server stops advertising itself to clients.
- Close GATT Server: The GATT server is closed, and all resources are freed.
Conclusion
The GATTServerService in the Cashier App acts as a central point for handling BLE 
communication with the Order App. It advertises itself, handles incoming connections, 
processes order data, and sends notifications to the client. Through various callbacks like 
`onCharacteristicWriteRequest` and `onConnectionStateChange`, it manages the entire BLE 
communication lifecycle, ensuring smooth interaction between the client and server.
BluetoothStateReceiver
The BluetoothStateReceiver class is responsible for monitoring changes in the Bluetooth 
state on the device. It listens for the ACTION_STATE_CHANGED broadcast, which is 
triggered whenever the Bluetooth state changes (e.g., Bluetooth is turned on or off). 
When the Bluetooth is turned off, the receiver calls the provided `onBluetoothOff` callback. 
Similarly, when the Bluetooth is turned on, the `onBluetoothOn` callback is triggered. This 
ensures that the application can respond to Bluetooth state changes dynamically, such as 
updating the UI or restarting services that depend on Bluetooth.
GattServerManager
The GattServerManager class is responsible for managing the lifecycle of the GATT server 
and maintaining the connection state. It handles starting and stopping the GATT server and 
tracks incoming orders from the client.
1. **updateConnectionState**: This function updates the connection state of the GATT 
server based on the events received (e.g., client connected or disconnected). It logs the 
updated connection state and reflects the change using a StateFlow object.
2. **handleReceivedData**: This function processes data received from the client. It 
appends the received data to the list of orders, which is also managed using StateFlow for 
real-time updates to any observers.
3. **startGattServer**: This function starts the GATT server by invoking the 
GATTServerService. It uses an Intent to start the service that manages the GATT server.
4. **stopGattServer**: This function stops the GATT server by stopping the 
GATTServerService using an Intent. It ensures that all resources related to the GATT server 
are freed when no longer needed.
5. **getConnectionState**: Provides a real-time observable StateFlow object that reflects 
the current state of the connection, allowing other components to react to changes in 
connectivity.
6. **getReceivedOrders**: Allows observers to track the received orders in real time by 
exposing a StateFlow list of received orders.
StateFlow Explanation
The `GattServerManager` makes use of Kotlin's StateFlow, which is a reactive data holder 
that emits values to its observers in real time. By using StateFlow, connection states and 
received orders are updated as soon as changes occur, ensuring the app can respond 
immediately to new data.
Conclusion
The GattServerManager ensures that the GATT server is properly managed and keeps track 
of all data communication between the client and server
