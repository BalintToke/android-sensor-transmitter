package com.android.sensor.transmitter

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Encapsulates sensor data collection handling
class SensorHandler: SensorEventListener {
    var mSensorList=ArrayList<Sensor>()
        private set
    // Contains current sensor values
    lateinit var mSensorValues:ArrayList<FloatArray>
        private set
    var mInitialized = false
        private set
    // Contains current action related values
    var mActionData = mutableMapOf<String,Any>()
        get(){
            if(!ServerConfig.sReadPullBooleanToFalse)
                return field

            for (key in field.keys){
                if(field[key] is Boolean) {
                    field[key]=false
                }
            }
            return field
        }

    private lateinit var mSensorManager:SensorManager

    fun setup(
        mSensorManager:SensorManager,
        mSensorList:ArrayList<Sensor>)
    {
        this.mSensorManager=mSensorManager
        this.mSensorList=mSensorList
        val mValuesFloatArray = ArrayList<FloatArray>()
        for (sensor in mSensorList){
            mValuesFloatArray.add(FloatArray(3))
        }
        this.mSensorValues= mValuesFloatArray

        val mActions=ServerConfig.mActionButtonConfig
        // Populate mActionData with default values based on types
        for (key in mActions.keys) {
            var value:Any
            value = when(mActions[key]){
                Boolean->{ false }
                String->{ "" }
                Int->{ -1 }
                else->{ false }
            }
            mActionData[key]=value
        }

        mInitialized=true

        resume()
    }

    override fun onSensorChanged(event: SensorEvent) {
        var index = mSensorList.indexOf(event.sensor)
        mSensorValues[index]=event.values
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Magic happens here (unused)
    }

    fun resume() {
        for (sensor in mSensorList){
            sensor.also { sensor ->
                mSensorManager.registerListener(this,sensor , SensorManager.SENSOR_DELAY_FASTEST)
            }
        }
    }
    fun pause() {
        var initialized = this::mSensorManager.isInitialized
        if(initialized)
            mSensorManager.unregisterListener(this)
    }
}