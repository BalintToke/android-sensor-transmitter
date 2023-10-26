package com.android.sensor.transmitter

// Contains the default values and remote server configuration
class ServerConfig {
    companion object{
        // Default values
        var sIP = "192.168.100.14"
        var sTCPPORT = "56967"
        var sUDPPORT = "56968"
        var sConnectionTCP = false // TCP or UDP
        var sDataRate = 60.0
        // After reading, put all boolean values to false
        var sReadPullBooleanToFalse = false
        // Extra action data provided besides sensor values (generate Views)
        //    Key: Button name / JSON data name
        //    Value: Type of data
        //        String/Int will generate input field as well
        var mActionButtonConfig= mapOf(
            "shooting" to Boolean,
            "nothing" to String
        )
    }
}