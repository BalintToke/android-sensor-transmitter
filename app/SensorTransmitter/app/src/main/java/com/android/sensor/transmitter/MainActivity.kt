package com.android.sensor.transmitter

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import com.android.sensor.transmitter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    // Prevent adding checkboxes again after returning from other activity
    private var alreadyInitialized = false

    // Contains all of the programmatically created checkboxes
    private var mSensorCheckBoxList = ArrayList<CheckBox>()
    // Internal list of all available sensor
    private var mSensorIdListAll = ArrayList<Int>()
    // Internal list containing selected sensors
    private var mSensorIdListSelected = ArrayList<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Feed in the default values to the view
        if(ServerConfig.sConnectionTCP)
            mBinding.ipPort.setText(ServerConfig.sIP+":"+ServerConfig.sTCPPORT)
        else
            mBinding.ipPort.setText(ServerConfig.sIP+":"+ServerConfig.sUDPPORT)
        mBinding.dataRate.setText(ServerConfig.sDataRate.toInt().toString())

        // Setup connection button
        mBinding.connect.setOnClickListener{instantiateConnection()}
        mBinding.connect.isEnabled=false
    }

    override fun onStart() {
        super.onStart()

        if(!alreadyInitialized) {
            addAvailableSensorCheckBoxes()
            alreadyInitialized=true
        }
    }

    // Based on available sensors, add checkboxes to the view
    private fun addAvailableSensorCheckBoxes()
    {
        val mSensorManager=getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL)
        var counter = 0
        for (item in sensors)
        {
            // Deprecated
            if(item.stringType=="android.sensor.orientation")
                continue

            var cb = CheckBox(this.applicationContext)
            cb.text = item.name
            cb.id=counter
            counter+=1
            cb.layoutParams=LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            cb.setOnCheckedChangeListener { buttonView, isChecked ->
                val bid = buttonView.id
                // Move in/out of sending list
                if(isChecked)
                {
                    if(!mSensorIdListSelected.contains(mSensorIdListAll[bid]))
                        mSensorIdListSelected.add(mSensorIdListAll[bid])
                }
                else
                {
                    if(mSensorIdListSelected.contains(mSensorIdListAll[bid]))
                        mSensorIdListSelected.remove(mSensorIdListAll[bid])
                }
                // Disable/enable connect button based on if at least one checkbox is selected
                var atLeastOneChecked = false
                for (cbItem in mSensorCheckBoxList)
                {
                    if(cbItem.isChecked)
                    {
                        atLeastOneChecked=true
                        break
                    }
                }
                mBinding.connect.isEnabled=atLeastOneChecked
            }

            mBinding.sensorCbContainer.addView(cb)
            mSensorIdListAll.add(item.type)
            mSensorCheckBoxList.add(cb)
        }
    }

    // Setup ConnectionActivity and start it
    private fun instantiateConnection(){
        val mIntent = Intent(this,ConnectionActivity::class.java)
        mIntent.putExtra("ip_port",mBinding.ipPort.text.toString())
        mIntent.putExtra("data_rate",mBinding.dataRate.text.toString())
        var bundle = Bundle()
        bundle.putIntegerArrayList("sensor_id_list",mSensorIdListSelected)
        mIntent.putExtras(bundle)
        startActivity(mIntent)
    }
}

