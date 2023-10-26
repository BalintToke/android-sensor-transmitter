## Application (Kotlin)

At the start, collect all available sensors and generate checkboxes based on them to let the user choose which one to use during the session.
At connection, generate buttons and text fields based on config.

### Activities

#### MainActivity
 - Collect available sensors and generate checkboxes based on them.
 - Populate IP:PORT and datarate fields with default values found in ServerConfig.kt
 - Start ConnectionActivity at button press.

#### ConnectionActivity
 - Perform basic validation of the data received from MainActivity.
 - Using coroutines, connect to the server.
 - Generate buttons and EditText fields based on configuration found in SensorConfig.kt
 - Instantiate SensorHandler object which subscribes to SensorEvents and stores their current data.
 - Using coroutines, perform message transmission to the server based on received data rate bundled in json format.
 - At Exit, close all connections and return to MainActivity

 ### Issues
 - If numerous sensors are available, selecting everything and sending them to a server might result in split packets.
 - Config is baked in the source code.

