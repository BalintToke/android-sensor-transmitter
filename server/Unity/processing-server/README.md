## Sample server (Unity)

A simple example of how to make a server capable of handling data received from the application. 

Assign connecting mobile to an InputReceiver which then takes on the rotation values of the device in real-time. 
As the player presses the "shooting" button on their mobile, initiate a bullet and forward that into the direction the phone was facing at the time of the event arriving.
Handle simple collision upon bullet reaching targets.

### Workflows

1. Map setup (MapSetup.cs)
    - Fills in a grid of cubes based on the "target" gameobject
2. Device to Server connection (ServerSystem.cs)
    - ServerSystem exists as a MonoBehaviour attached to an in-game object
    - ServerSystem creates an instance of ASTServer and assigns in-game objects to receive rotational data from connected devices.
3. Device to Server connection (ASTServer.cs)
    - ASTServer opens up ports expecting TCP and/or UDP connections
    - For TCP: each new client is assigned a new thread to handle arriving data
    - For UDP: a single thread handles all arriving data from different clients
    - Thread to continuously check servers' state
    - Thread to continuously check clients' state

### Issues

 - "android.sensor.game_rotation_vector" sensor can provide data in different base orientations. This problem can be solved by using delta rotation to handle in-game objects.
 - No encryption used.