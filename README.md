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

### Screenshots

<div style="text-align: center">
<img src="https://btokesoftwares.com/nelson-rule-python/app_main.jpg"
     alt="Markdown Monster icon"
     style="width: 30%" 
     width=300/>
<img src="https://btokesoftwares.com/nelson-rule-python/app_connect.jpg"
     alt="Markdown Monster icon"
     style="width: 30%" 
     width=300/>
</div>

### Video

https://github.com/BalintToke/android-sensor-transmitter/assets/61911394/3e2e2560-0371-447d-bf40-21d8c304c191

