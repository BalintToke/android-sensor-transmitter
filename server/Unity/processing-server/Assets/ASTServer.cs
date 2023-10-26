using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;
using System.Text;
using System.Threading;
using UnityEngine;
using Newtonsoft.Json;

// Android Sensor Transmitter Server
//   Input receiver:
//      1. subscribe to ASTserver by calling Subscribe with a unique ID
//      2. subscribe to onReceivedValue
class ASTServer
{
    public bool isActive = false;

    bool mTcpActive = false;
    bool mUdpActive = true;

    static int TCP_PORT = 56967;
    static int UDP_PORT = 56968;

    static int clientCheckTimeout = 1000;
    static int serverCheckTimeout = 5000;

    // Tcp
    TcpListener mTcpServer;
    Task tServerListener;

    List<Thread> lActiveClientThreads = new List<Thread>();
    List<TcpClient> lActiveClients = new List<TcpClient>();
    Thread tClientStateCheck;
    Thread tServerStateCheck;

    List<GameObject> lInputReceiverPool = new List<GameObject>();

    List<int> lIdBuffer = new List<int>();

    // To calculate delta
    Quaternion mPrevQuaternion = new Quaternion(0, 0, 0, 0);

    // Udp
    UdpClient mUdpServer;
    IPEndPoint mUdpEP;
    Dictionary<int, int> mUdpDeviceIdToObjectMap = new Dictionary<int, int>();

    public static event Action<int, Quaternion, Vector3, bool> onReceivedValue;
    public static void sendValue(int id, Quaternion angles, Vector3 force, bool shooting)
    {
        if (onReceivedValue != null)
        {
            onReceivedValue(id, angles, force, shooting);
        }
    }

    public ASTServer()
    {
        if (mTcpActive)
        {
            mTcpServer = new TcpListener(IPAddress.Any, TCP_PORT);
        }
        if (mUdpActive)
        {
            mUdpServer = new UdpClient(UDP_PORT);
            mUdpEP = new IPEndPoint(IPAddress.Any, UDP_PORT);
        }
    }

    public void Subscribe(int id)
    {
        lIdBuffer.Add(id);
    }

    public void Start()
    {
        tServerListener = Task.Run(() => ServerListenerAsync());
    }

    public void AddInputReceiver(GameObject rec)
    {
        if (!lInputReceiverPool.Contains(rec))
            lInputReceiverPool.Add(rec);
    }

    public void RemoveInputReceiver(GameObject rec)
    {
        if (lInputReceiverPool.Contains(rec))
            lInputReceiverPool.Remove(rec);
    }

    // DONT USE THIS
    public void Stop()
    {
        mTcpServer.Stop();
    }

    System.Threading.SynchronizationContext sMainThreadContext = System.Threading.SynchronizationContext.Current;

    async Task ServerListenerAsync()
    {
        if (mTcpActive)
        {
            mTcpServer.Start();
            Debug.Log("[TCP]:[INFO]: Server started!");
        }
        if (mUdpServer != null)
            Debug.Log("[UDP]:[INFO]: Server started!");
       

        tClientStateCheck = new Thread(new ThreadStart(ClientStateCheck));
        tClientStateCheck.Start();

        tServerStateCheck = new Thread(new ThreadStart(ServerStateCheck));
        tServerStateCheck.Start();

        TcpClient client;
        while (true)
        {
            try
            {
                if (mTcpActive)
                {
                    client = await mTcpServer.AcceptTcpClientAsync();
                    Debug.Log("[TCP]:[INFO]: Client CONNECTED!");
                    Thread clientThread = new Thread(new ParameterizedThreadStart(ClientHandler));
                    clientThread.Start(client);
                    lActiveClientThreads.Add(clientThread);
                }
                if (mUdpActive)
                {
                    byte[] bytes = mUdpServer.Receive(ref mUdpEP);
                    UdpPacketReceivedHandler(bytes);
                }
                
            }
            catch
            {
                break;
            }

        }
    }

    public void ServerStateCheck()
    {
        // TODO
    }

    public void ClientStateCheck()
    {
        int counter = 0;
        while (true)
        {
            Thread.Sleep(clientCheckTimeout);
            if (lActiveClients.Count == 0)
                continue;
            if (lActiveClientThreads.Count == 0)
                continue;
            if (!lActiveClients[counter].Connected)
            {
                lActiveClients.RemoveAt(counter);
                lActiveClientThreads[counter].Abort();
                lActiveClientThreads.RemoveAt(counter);
            }
            if (lActiveClientThreads[counter].ThreadState != ThreadState.Running)
            {
                lActiveClientThreads.RemoveAt(counter);
                if (lActiveClients[counter].Connected)
                    lActiveClients[counter].Close();
                lActiveClients.RemoveAt(counter);
            }

            counter++;
            if (counter >= lActiveClients.Count)
                counter = 0;
        }

    }

    public void UdpPacketReceivedHandler(byte[] bytes)
    {
        string msg = Encoding.ASCII.GetString(bytes);
        // Enable subthreads to interact with main thread objects 
        sMainThreadContext.Post(_ => ApplyInput(msg, -1), null);
    }

    public void ClientHandler(object receivedClient)
    {
        try
        {
            TcpClient client = receivedClient as TcpClient;
            if (client == null)
                throw new Exception("[TCP]:[ERROR]: Client not valid");
            NetworkStream stream = client.GetStream();
            if (lIdBuffer.Count == 0)
            {
                throw new Exception("[TCP]:[ERROR]: No available input receiver, close connection");
            }
            int choosenId = lIdBuffer[0];

            while (true)
            {
                if (client.ReceiveBufferSize > 0)
                {
                    Byte[] bytes;
                    bytes = new byte[client.ReceiveBufferSize];
                    stream.Read(bytes, 0, client.ReceiveBufferSize);
                    if (bytes.Length > 0)
                    {
                        if (bytes[0].Equals((byte)0))
                        {
                            throw new Exception("[TCP]:[ERROR]: Client closed connection");
                        }
                    }
                    string msg = Encoding.ASCII.GetString(bytes); //the message incoming
                    sMainThreadContext.Post(_ => ApplyInput(msg, choosenId), null);
                    //ApplyInput(choosenId, msg);
                }
            }


        }
        catch (Exception exc)
        {
            Debug.Log(exc.Message);
            return;
        }

    }

    public void ApplyInput(string states, int id = -1)
    {
        try
        {
            bool shooting = false; 
            
            ServerDataConfig data = JsonConvert.DeserializeObject<ServerDataConfig>(states);
            if (!data.actionData.ContainsKey("shooting"))
                return;
            if (!data.sensorDictionary.ContainsKey("android.sensor.game_rotation_vector"))
                return;

            shooting = (bool)data.actionData["shooting"];
            string empty = data.actionData["nothing"].ToString();

            if (id != -1) // TCP only: separate merged packets and ignore everything but the first one
            {
                string[] splittedState = states.Split('{');
                StringBuilder recover = new StringBuilder();
                recover.Append("{");
                recover.Append(splittedState[1]);
                recover.Append("{");
                recover.Append(splittedState[2]);
                recover.Append("{");
                recover.Append(splittedState[3]);

                states = recover.ToString();
            }
            else // Udp only: ID check
            {
                if (mUdpDeviceIdToObjectMap.ContainsKey(data.deviceId))
                {
                    id = mUdpDeviceIdToObjectMap[data.deviceId];
                }
                else
                {
                    if (lIdBuffer.Count > 0)
                    {
                        mUdpDeviceIdToObjectMap[data.deviceId] = lIdBuffer[0];
                        id = lIdBuffer[0];
                    }
                }
            }

            float[] rotData = data.sensorDictionary["android.sensor.game_rotation_vector"];
            Quaternion rot = new Quaternion(rotData[0], rotData[1], rotData[2], rotData[3]);

            // Calculate delta rotation
            //Quaternion delta = rot * Quaternion.Inverse(prevQuaternion);
            //prevQuaternion = rot;

            // Apply delta rotation
            //sendValue(id, delta, new Vector3(0, 0, 0), shooting);
            // Apply direct rotation
            sendValue(id, rot, new Vector3(0, 0, 0), shooting);
        }
        catch (Exception exc)
        {
            Debug.Log("ERROR: " + exc.Message);
            Debug.Log(states);
            Debug.Log("----");
        }
    }

    public void Abort()
    {
        tServerStateCheck.Abort();
        tClientStateCheck.Abort();
        foreach (Thread thread in lActiveClientThreads)
        {
            try
            {
                thread.Abort();
            }
            catch { }
        }
        foreach (TcpClient client in lActiveClients)
        {
            try
            {
                client.Close();
            }
            catch { }
        }
        lActiveClients.Clear();
        lActiveClientThreads.Clear();
        if (mTcpActive)
        {
            mTcpServer.Stop();
        }
        if (mUdpActive)
        {
            mUdpServer.Close();
        }
    }

    public bool AllThreadDown()
    {
        bool result = true;
        foreach (var item in lActiveClientThreads)
        {
            if (item.IsAlive)
                result = false;
        }
        if (tClientStateCheck.ThreadState == ThreadState.Running)
            result = false;
        if (tServerStateCheck.ThreadState == ThreadState.Running)
            result = false;

        return result;
    }
}

public class ServerDataConfig {

    [JsonProperty("id")]
    public int deviceId;
    [JsonProperty("sensors")]
    public Dictionary<string, float[]> sensorDictionary;
    [JsonProperty("actions")]
    public Dictionary<string, object> actionData;


    public class SensorData {
        [JsonProperty]
        public Dictionary<string, float[]> values;
    }
    public class ActionData
    {
        [JsonProperty]
        public object actionData;
    }
}

