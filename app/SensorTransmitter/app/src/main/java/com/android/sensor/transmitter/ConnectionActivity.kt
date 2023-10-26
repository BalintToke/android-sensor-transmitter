package com.android.sensor.transmitter

import android.app.Activity
import com.android.sensor.transmitter.databinding.ActivityConnectionBinding
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import java.io.OutputStream
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject
import android.view.*
import android.widget.*
import java.net.*
import kotlinx.coroutines.*

// Handle connection, data collection and transmission
class ConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectionBinding

    // Can be Socket or DatagramSocket
    private lateinit var mNetworkClient : Any
    // Only initialized if connection is TCP
    private lateinit var mNetworkClientTcpStream : OutputStream
    // Handler to loop data processing
    private lateinit var mSensorDataHandler: Handler
    // Object to save/process SensorEvent values
    private var mSensorHandler = SensorHandler()
    // Unique ID for remote server to recognize device
    private var mDeviceGeneratedId = -1

    // Mapping the EditText fields to Buttons to Actions
    private var mActionButtonToActionMap = mutableMapOf<Button,String>()
    private var mActionButtonToEditTextMap = mutableMapOf<Button,TextView>()

    // Container for all generated TextViews
    private var mTVGeneratedContainer = ArrayList<TextView>()
    // Data rate, overridden at Activity creation
    private var dataRate:Double=30.0

    // Since there are no console to signal if messages cannot go through,
    // update connection state after trying to send message
    private var mConnectionState = "DISCONNECTED"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mReceivedIPPort = intent?.extras?.getString("ip_port").toString()
        val mReceivedDataRate = intent?.extras?.getString("data_rate").toString()
        val mReceivedSensorIdList = intent.extras?.getIntegerArrayList("sensor_id_list")
        // Basic string validation, does not cover all grounds
        try{
            if(mReceivedIPPort!="null" && mReceivedDataRate!="null" && mReceivedSensorIdList!=null)
            {
                val mIPPortArray = mReceivedIPPort.split(':')
                var valid = false
                if(mIPPortArray.count()==2
                    && mIPPortArray[0].split('.').count()==4
                    && mIPPortArray[1].toIntOrNull()!=null)
                    valid=true

                if(valid)
                {
                    val ip = mIPPortArray[0]
                    val port = mIPPortArray[1]
                    updateUIText(mapOf(
                        binding.ip to ip,
                        binding.port to port))

                    dataRate = mReceivedDataRate.toDouble()
                    connectToServerCoroutineHandler(ip,port.toInt(),mReceivedSensorIdList)
                }
                else
                {
                    handleError(404)
                }
            }
        }
        catch (e:Exception){
            handleError(404)
        }

        binding.exitButton.setOnClickListener { finishConnection() }
    }

    // Handle the coroutine responsible for connection
    private fun connectToServerCoroutineHandler(ip:String,port:Int,mSensorIdList:ArrayList<Int>) {
        updateUIText(mapOf(
            binding.connectionState to "Connecting to server..."))
        binding.exitButton.isEnabled = false

        var connectionResult = -1
        // Coroutine to connect to server
        val serverStartJob = GlobalScope.launch(Dispatchers.Main){
            connectionResult = try {
                connectToServer(ip,port)
            } catch (e:Exception) {
                -1
            }
        }
        // Based on connection result, halt activity or start sensor data collection
        serverStartJob.invokeOnCompletion {
            var connectionResultString = "Could not connect to server"
            when(connectionResult){
                -1->{ // Unspecified error during connection
                    handleError(900)
                    return@invokeOnCompletion
                }
                0->{ // Connection successful
                    connectionResultString="CONNECTED"
                    // Generate unique ID
                    mDeviceGeneratedId= Random.nextInt()
                    // Start collecting data
                    setupDeviceSensorCollector(mSensorIdList)
                }
                503->{
                    connectionResultString="ERROR: Security infringement"
                }
                else->{
                    connectionResultString="DISCONNECTED"
                }
            }
            binding.exitButton.isEnabled = true
            mConnectionState=connectionResultString
            updateUIText(mapOf(
                binding.connectionState to connectionResultString))
        }
    }

    // Called in coroutine to connect to server
    private suspend fun connectToServer(ip:String,port:Int): Int
    {
        var result = -1

        val res = withContext(Dispatchers.IO)
        {
            // Different connection methods for TCP/UDP
            try {
                if(ServerConfig.sConnectionTCP)
                {
                    mNetworkClient=Socket()
                    (mNetworkClient as Socket).connect(InetSocketAddress(ip, port),10000)
                    if((mNetworkClient as Socket).isConnected){
                        mNetworkClientTcpStream=(mNetworkClient as Socket).getOutputStream()
                        result=0
                    }
                }
                else
                {
                    mNetworkClient= DatagramSocket()
                    (mNetworkClient as DatagramSocket).connect(InetAddress.getByName(ip),port)
                    if((mNetworkClient as DatagramSocket).isConnected){
                        result=0
                    }
                }
            }
            catch (e: SecurityException)
            {
                result = 503
            }
            catch (e: Exception)
            {
                result = 1
            }
        }
        return result
    }

    // Initialize SensorHandler instance
    // Add UI elements to screen
    // Post data collection loop to the Handler
    private fun setupDeviceSensorCollector(mSensorIdList: ArrayList<Int>)
    {
        val mSensorManager=getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mSensorList = getSensorList(mSensorManager,mSensorIdList)
        addUIElementsToScreen(mSensorList)

        mSensorHandler.setup(mSensorManager,mSensorList)

        mSensorDataHandler= Handler(Looper.getMainLooper())
        mSensorDataHandler.post(fSensorDataParserLoop)
    }

    // Runnable object to loop data collection functions
    private val fSensorDataParserLoop = object :Runnable{
        override fun run() {
            val jODictionary = JSONObject()

            jODictionary.put("id",mDeviceGeneratedId)

            val jODictionarySensors = JSONObject()

            val mSensorList = mSensorHandler.mSensorList
            val mSensorValues = mSensorHandler.mSensorValues

            for (sensor in mSensorList) {
                val index = mSensorList.indexOf(sensor)

                val jACurrentFloats = JSONArray()
                for (floatValue in mSensorValues[index]){
                    if(floatValue.isNaN())
                        jACurrentFloats.put(0.0)
                    else
                        jACurrentFloats.put(floatValue)
                }
                jODictionarySensors.put(mSensorList[index].stringType,jACurrentFloats)
            }

            jODictionary.put("sensors",jODictionarySensors)

            val jODictionaryActions = JSONObject()

            val mActionData = mSensorHandler.mActionData
            for (key in mActionData.keys){
                jODictionaryActions.put(key,mActionData[key])
                if(mActionData[key] is Boolean){
                    mActionData[key]=false
                }
            }

            jODictionary.put("actions",jODictionaryActions)

            sendMessageCoroutineHandler(jODictionary.toString())

            // Consider calculating delayTime based on device speed
            val delayTime = (1000/dataRate)
            mSensorDataHandler.postDelayed(this,delayTime.toLong())
        }
    }

    // Handler coroutine responsible for sending messages to server
    private fun sendMessageCoroutineHandler(message: String)
    {
        var coroutineResult = ""
        val job = GlobalScope.launch(Dispatchers.Main){
            coroutineResult = sendMessage(message)
        }
        job.invokeOnCompletion {
            when(coroutineResult){
                "DONE"->{
                    updateUIText(mapOf(
                        binding.connectionState to mConnectionState))
                }
                else->{
                    // Stop message sending loop after failure
                    // For LAN this should be fine
                    updateUIText(mapOf(
                        binding.connectionState to "Failed to send message"))
                    if(ServerConfig.sConnectionTCP)
                    {
                        (mNetworkClient as Socket).close()
                    }
                    else
                    {
                        (mNetworkClient as DatagramSocket).close()
                    }
                }
            }
        }
    }

    // Coroutine to send message over connection
    private suspend fun sendMessage(message: String): String
    {
        var result = "Connection closed"
        val res = withContext(Dispatchers.IO)
        {
            try {
                if(ServerConfig.sConnectionTCP)
                {
                    if ((mNetworkClient as Socket).isConnected)
                    {
                        mNetworkClientTcpStream.write(message.toByteArray())
                        result = "DONE"
                    }
                    else {
                        throw Exception("Connection closed")
                    }
                }
                else
                {
                    if ((mNetworkClient as DatagramSocket).isConnected)
                    {
                        val sendData = message.toByteArray()
                        val sendPacket = DatagramPacket(sendData,0,sendData.size,(mNetworkClient as DatagramSocket).remoteSocketAddress)
                        (mNetworkClient as DatagramSocket).send(sendPacket)
                        result = "DONE"
                    }
                    else {
                        throw Exception("Connection closed")
                    }
                }

            } catch (e: Exception) {
                if (e.message != null)
                    result = e.message.toString()
            }
        }
        return result
    }

    // Add all available sensors to mSensorList based on the received mSensorIdList
    private fun getSensorList(mSensorManager:SensorManager,mSensorIdList:ArrayList<Int>):ArrayList<Sensor>{
        val mSensorList = ArrayList<Sensor>()
        for (sensorId in mSensorIdList){
            val mSensor = mSensorManager.getDefaultSensor(sensorId)
            if(mSensor!=null){ mSensorList.add(mSensor) }
        }
        return mSensorList
    }

    private fun addUIElementsToScreen(mSensorList:ArrayList<Sensor>) {
        // Add active sensors' textview
        for(mSensor in mSensorList){
            val tv = TextView(this)
            tv.text="Active sensor: "+ mSensor.stringType
            tv.layoutParams= LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            binding.sensorDataContainer.addView(tv)
            mTVGeneratedContainer.add(tv)
        }
        // Add action configs' button/textview
        for (action in ServerConfig.mActionButtonConfig.keys){
            var createEditText = false
            var textFieldIsNumeric = false
            // Check type to determine if textview is needed
            when(ServerConfig.mActionButtonConfig[action]){
                Boolean->{
                    createEditText=false
                    textFieldIsNumeric=false
                }
                String->{
                    createEditText=true
                    textFieldIsNumeric=false
                }
                Int->{
                    createEditText=true
                    textFieldIsNumeric=true
                }
                else->{
                    createEditText=false
                    textFieldIsNumeric=false
                }
            }
            // Add edit text if needed
            val et = EditText(this)
            if(createEditText){
                et.layoutParams= LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                if(textFieldIsNumeric)
                    et.inputType=InputType.TYPE_NUMBER_FLAG_DECIMAL
                else
                    et.inputType=InputType.TYPE_CLASS_TEXT
            }
            // Add button at all times
            val btn = Button(this)
            btn.text=action
            btn.isEnabled=true
            btn.layoutParams= LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Add onclick listener based on if textview is present
            if(createEditText){
                btn.setOnClickListener{
                    mSensorHandler.mActionData[mActionButtonToActionMap[btn].toString()]=mActionButtonToEditTextMap[btn]?.text.toString()
                }
            }
            else
            {
                btn.setOnClickListener{
                    mSensorHandler.mActionData[mActionButtonToActionMap[btn].toString()]=true
                }
            }
            // Update maps
            mActionButtonToActionMap[btn] = action
            if(createEditText)
                mActionButtonToEditTextMap[btn]=et
            // Place views on screen
            if(createEditText)
                binding.actionContainer.addView(et)
            binding.actionContainer.addView(btn)
        }
    }

    // Central control of UI text elements' update
    // May be unnecessary for this case
    private fun updateUIText(textUpdateMap:Map<View,String>){
        for (kView in textUpdateMap.keys){
            var mPlaceholderString = ""
            mPlaceholderString = when(kView){
                binding.ip->{
                    getString(R.string.ui_tv_ip)
                }
                binding.port->{
                    getString(R.string.ui_tv_port)
                }
                else->{
                    "%s"
                }
            }
            (kView as TextView).text=String.format(mPlaceholderString,textUpdateMap[kView])
        }
    }

    // Central handling of any error occurred during activity's lifespan
    private fun handleError(code:Int) {
        when(code){
            404->{
                updateUIText(mapOf(
                    binding.connectionState to "Invalid address",
                    binding.ip to "INVALID",
                    binding.port to "INVALID"
                ))
            }
            900->{
                updateUIText(mapOf(
                    binding.connectionState to "Error while initializing server"))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if(mSensorHandler.mInitialized)
            mSensorHandler.resume()
    }

    override fun onPause() {
        super.onPause()

        if(mSensorHandler.mInitialized)
            mSensorHandler.pause()
    }

    // Close sockets and return to MainActivity
    private fun finishConnection()
    {
        if(ServerConfig.sConnectionTCP)
        {
            if(!(mNetworkClient as Socket).isClosed)
                (mNetworkClient as Socket).close()
        }
        else
        {
            if(!(mNetworkClient as DatagramSocket).isClosed)
                (mNetworkClient as DatagramSocket).close()
        }
        setResult(Activity.RESULT_OK)
        mSensorHandler.pause()
        finish()
    }
}