package com.shawn.petdemo

import android.content.Context
import android.net.wifi.WifiManager

private val hexCode = "0123456789ABCDEF".toCharArray()

fun printHexBinary(data: ByteArray): String {
    val r = java.lang.StringBuilder(data.size * 2)
    data.forEach {
        r.append(hexCode[it.toInt() shr 4 and 0xF])
        r.append(hexCode[it.toInt() and 0xf])
    }

    return r.toString()
}

fun getLocalIPAddress(context: Context): String {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    if (wifiManager != null) {
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }
    return ""
}