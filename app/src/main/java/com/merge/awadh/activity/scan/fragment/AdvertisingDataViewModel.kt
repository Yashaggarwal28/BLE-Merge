package com.merge.awadh.activity.scan.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AdvertisingDataViewModel : ViewModel() {
    private val _tempData = MutableLiveData<List<Float>>()
    val tempData: LiveData<List<Float>> get() = _tempData

    private val _humidData = MutableLiveData<List<Float>>()
    val humidData: LiveData<List<Float>> get() = _humidData

    private val _xData = MutableLiveData<List<Float>>()
    val xData: LiveData<List<Float>> get() = _xData

    private val _yData = MutableLiveData<List<Float>>()
    val yData: LiveData<List<Float>> get() = _yData

    private val _zData = MutableLiveData<List<Float>>()
    val zData: LiveData<List<Float>> get() = _zData

    private val _speedData = MutableLiveData<List<Float>>()
    val speedData: LiveData<List<Float>> get() = _speedData

    private val _stepCountData = MutableLiveData<Int>()
    val stepCountData: LiveData<Int> get() = _stepCountData

    fun updateTempData(data: List<Float>) {
        _tempData.value = data
    }

    fun updateHumidData(data: List<Float>) {
        _humidData.value = data
    }

    fun updateXData(data: List<Float>) {
        _xData.value = data
    }

    fun updateYData(data: List<Float>) {
        _yData.value = data
    }

    fun updateZData(data: List<Float>) {
        _zData.value = data
    }

    fun updateSpeedData(data: List<Float>) {
        _speedData.value = data
    }

    fun updateStepCount(count: Int) {
        _stepCountData.value = count
    }
}
