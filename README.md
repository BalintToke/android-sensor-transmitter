Grabs available and selected sensor data and forward it to a specified server bundled in JSON format.

Application (folder: app):
 - Made with Android Studio
 - Using Kotlin
 - TCP or UDP
 - Modify data rate
 - Collects available sensors at runtime 

Server (folder: server):
 - Made with Unity 2021.3.15f1
 - TCP and/or UDP
 - Using android.sensor.game_rotation_vector to match the rotation of a connected mobile device to an object on the screen

Info:
- TCP Port: 56967
- UDP Port: 56968

JSON packet format:
```json
{
        "id":Integer,
        "sensors": 
        {
                "sensor.name": FloatArray
        },
        "actions":
        {
                "action.name.string":String,
                "action.name.boolean":Boolean,
                "action.name.integer":Integer
        }
}
```