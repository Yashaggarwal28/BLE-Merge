package com.merge.awadh.activity.scan

interface ScanInterface {
    fun startIntent()
    fun startToast(message: String)
    fun promptEnableBluetooth()
    fun requestPermissions()
}