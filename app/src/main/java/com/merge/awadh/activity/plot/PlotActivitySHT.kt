 package com.merge.awadh.activity.plot

import android.bluetooth.le.ScanResult
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.merge.awadh.R
import com.merge.awadh.databinding.ActivityPlotShtBinding
import com.merge.awadh.ble.BLEManager.registerScanResultListener
import com.merge.awadh.ble.BLEManager.unregisterScanResultListener
import com.merge.awadh.ble.ScanResultListener
import timber.log.Timber


 class PlotActivitySHT : AppCompatActivity(), ScanResultListener {

    private lateinit var binding: ActivityPlotShtBinding
    private lateinit var tempChart: LineChart
    private lateinit var humidChart: LineChart
     private var isDarkMode: Boolean = false
    private var deviceAddress: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var tempEntries = ArrayList<Entry>()
    private var humidEntries = ArrayList<Entry>()
    private var time = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_plot_sht)

        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)

        // Set the initial state of the switch
        val isDarkTheme = sharedPreferences.getBoolean("isDarkTheme", false)
        if (isDarkTheme) {
            isDarkMode = true
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            isDarkMode = false
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        setSupportActionBar(binding.toolbar)
        // Set the custom navigation icon
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_navigation_black)
        }

        tempChart = binding.tempChart
        humidChart = binding.humidChart

        // Initialize charts
        initChart(tempChart)
        initChart(humidChart)

        // Set custom marker
        val markertemp = CustomMarkerView(this, R.layout.marker_view_temp)
        tempChart.marker = markertemp
        val markerhumid = CustomMarkerView(this, R.layout.marker_view_humid)
        humidChart.marker = markerhumid

        // Get data from the intent
        val tempData = intent.getFloatArrayExtra("TEMP_DATA")
        val humidData = intent.getFloatArrayExtra("HUMID_DATA")

        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")

        // Register this fragment as a listener
        registerScanResultListener(this)

        tempData?.let {
            for (i in it.indices) {
                addEntryToChart(tempChart, tempEntries, time + i, it[i], "#FFD81F" )
            }
        }

        humidData?.let {
            for (i in it.indices) {
                addEntryToChart(humidChart, humidEntries, time + i, it[i], "#4152C3")
            }
        }
    }

    override fun onScanResultUpdated(result: ScanResult){
        if (result.device.address == deviceAddress) {

            // Access the byte array from the scan record
            val bytes = result.scanRecord?.bytes ?: byteArrayOf()

            // Convert bytes to unsigned integers and log them
            val unsignedBytes = bytes.map { it.toUByte().toInt() }


            // Extract bytes if the array has the required length

            val temp1 = unsignedBytes.getOrNull(5)
            val temp2 = unsignedBytes.getOrNull(6)
            val humid1 = unsignedBytes.getOrNull(7)
            val humid2 = unsignedBytes.getOrNull(8)
            if (temp1 != null && temp2 != null && humid1 != null && humid2 != null) {
                val temperature: Double = temp1.toDouble() + (temp2.toDouble() / 100.0)
                val humidity: Double = humid1.toDouble() + (humid2.toDouble() / 100.0)

                addNewData(temperature.toFloat(), humidity.toFloat())
            }
        }
    }
    private fun initChart(chart: LineChart) {
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)

        // Disable grid lines and axis labels
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)
        // Fix the Y-axis range to be between -20 and 20
        chart.axisLeft.axisMinimum = -10f
        chart.axisLeft.axisMaximum = 50f
        // Disable legend
        chart.legend.isEnabled = false


    }

    private fun addEntryToChart(chart: LineChart, entries: ArrayList<Entry>, x: Float, y: Float, color: String) {
        entries.add(Entry(x, y))

        // Check if the number of entries exceeds 30
        if (entries.size > 30) {
            entries.removeAt(0) // Remove the oldest entry
        }

        val dataSet = LineDataSet(entries, if (chart == tempChart) "Temperature" else "Humidity")

        dataSet.setDrawValues(false)  // Disable values on data points

        // Enable cubic Bezier curve
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        dataSet.color = Color.parseColor(color) // Line color
        dataSet.setCircleColor(Color.parseColor(color)) // Point color
        dataSet.circleRadius = 1f // Point radius
        dataSet.setDrawCircleHole(false) // Filled circles
        dataSet.highLightColor = Color.parseColor(color) // Highlight color
        dataSet.setDrawHighlightIndicators(true) // Draw highlight indicators
        dataSet.lineWidth = 2f

        // Enable fill and set fill color
        dataSet.setDrawFilled(false)

        if (isDarkMode) {
            chart.axisLeft.textColor = Color.WHITE
            chart.axisRight.textColor = Color.WHITE
            chart.xAxis.textColor = Color.WHITE
            chart.legend.textColor = Color.WHITE
            chart.setBackgroundColor(Color.BLACK) // Set background to black
        } else {
            chart.axisLeft.textColor = Color.BLACK
            chart.axisRight.textColor = Color.BLACK
            chart.xAxis.textColor = Color.BLACK
            chart.legend.textColor = Color.BLACK
            chart.setBackgroundColor(Color.WHITE) // Set background to white
        }

        dataSet.valueTextSize = 0f // Hide value text

        val lineData = LineData(dataSet)
        chart.data = lineData
        val limitLine = LimitLine(0f, "")
        limitLine.lineWidth = 2f
        limitLine.lineColor = Color.GRAY // Set line color
//        limitLine.enableDashedLine(10f, 10f, 0f) // Optional dashed style

        // Configure the Y-axis and add the limit line
        val yAxis = chart.axisLeft
        yAxis.addLimitLine(limitLine) // Add the limit line
        yAxis.setDrawLimitLinesBehindData(true) // Ensure the limit line is behind the chart data

        chart.invalidate() // Refresh the chart
    }


override fun onSupportNavigateUp(): Boolean {

    clearChartData(tempChart)
    clearChartData(humidChart)

    tempEntries = ArrayList()
    humidEntries = ArrayList()

    // Unregister the listener to avoid memory leaks
    unregisterScanResultListener(this)
//    Timber.i("Unregistered the plot")
    onBackPressed()
    return true
}
     // Helper function to clear chart data
     private fun clearChartData(chart: LineChart) {
         chart.clear() // Clear the chart's data
         chart.invalidate() // Refresh the chart to reflect the changes
     }

    fun addNewData(temp: Float, humid: Float) {
        time += 1
        addEntryToChart(tempChart, tempEntries, time, temp, "#FFD81F")
        addEntryToChart(humidChart, humidEntries, time, humid, "#4152C3")
    }


}
