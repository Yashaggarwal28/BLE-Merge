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
import com.merge.awadh.databinding.ActivityPlotAccBinding
import com.merge.awadh.ble.BLEManager.registerScanResultListener
import com.merge.awadh.ble.BLEManager.unregisterScanResultListener
import com.merge.awadh.ble.ScanResultListener
import kotlinx.coroutines.*


class PlotActivityAcc : AppCompatActivity(), ScanResultListener {

    private lateinit var binding: ActivityPlotAccBinding
    private lateinit var xChart: LineChart
    private lateinit var yChart: LineChart
    private lateinit var zChart: LineChart

    private var deviceAddress: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var xEntries = ArrayList<Entry>()
    private var yEntries = ArrayList<Entry>()
    private var zEntries = ArrayList<Entry>()
    private var time = 0f
    private var isDarkMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_plot_acc)

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
        xChart = binding.xChart
        yChart = binding.yChart
        zChart = binding.zChart

        // Initialize charts
        initChart(xChart)
        initChart(yChart)
        initChart(zChart)

        // Set custom marker
        val markerX = CustomMarkerView(this, R.layout.marker_view_humid)
        xChart.marker = markerX
        val markerY = CustomMarkerView(this, R.layout.marker_view_temp)
        yChart.marker = markerY

        zChart.marker = markerY

        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")

        // Register this activity as a listener
        registerScanResultListener(this)

        // Get data from the intent
        val xData = intent.getFloatArrayExtra("X_DATA")
        val yData = intent.getFloatArrayExtra("Y_DATA")
        val zData = intent.getFloatArrayExtra("Z_DATA")
        GlobalScope.launch(Dispatchers.Default) {
            val time = (1..30).toList() // Range from 1 to 30

            xEntries.clear()
            yEntries.clear()
            zEntries.clear()
            // Populate xEntries with time and xData pairs
            for (i in time.indices) {
                val timeValue = time[i].toFloat()
                val xDataValue = xData?.getOrNull(i) ?: 0f
                val yDataValue = yData?.getOrNull(i) ?: 0f
                val zDataValue = zData?.getOrNull(i) ?: 0f

                val entryx = Entry(timeValue, xDataValue)
                xEntries.add(entryx)

                val entryy = Entry(timeValue, yDataValue)
                yEntries.add(entryy)

                val entryz = Entry(timeValue, zDataValue)
                zEntries.add(entryz)
            }
            withContext(Dispatchers.Main) {
                addEntryToChart(xChart, xEntries, "#FF0000") // Red for X
                addEntryToChart(yChart, yEntries, "#FFFF00") // Yellow for Y
                addEntryToChart(zChart, zEntries, "#0000FF") // Blue for Z
            }
        }
    }
    //gets the new scanned result and checks that the address is same as the opened device and if yes then it adds the new entry to graph
    override fun onScanResultUpdated(result: ScanResult){
        if (result.device.address == deviceAddress) {
            GlobalScope.launch(Dispatchers.Default) {
            // Access the byte array from the scan record
            val bytes = result.scanRecord?.bytes ?: byteArrayOf()

            // Convert bytes to signed integers and log them
            val signedBytes = bytes.map { it.toInt() }

            // Extract bytes if the array has the required length
            val xd = signedBytes.getOrNull(5)
            val xf = signedBytes.getOrNull(6)?.toUByte()?.toInt()
            val yd = signedBytes.getOrNull(7)
            val yf = signedBytes.getOrNull(8)?.toUByte()?.toInt()
            val zd = signedBytes.getOrNull(9)
            val zf = signedBytes.getOrNull(10)?.toUByte()?.toInt()

            if (xd != null && xf != null && yd != null && yf != null && zd != null && zf != null) {
                val X: Double = xd.toDouble() + (xf.toDouble() / 100.0)
                val Y: Double = yd.toDouble() + (yf.toDouble() / 100.0)
                val Z: Double = zd.toDouble() + (zf.toDouble() / 100.0)
                withContext(Dispatchers.Main) {
                    addNewData(X.toFloat(), Y.toFloat(), Z.toFloat())
                }
            }
                }
        }
    }

    //initialises the graph
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
        chart.axisLeft.axisMinimum = -20f
        chart.axisLeft.axisMaximum = 20f

        // Optionally, disable automatic scaling of the Y-axis
        chart.axisLeft.isGranularityEnabled = true
        chart.axisLeft.granularity = 1f // Set the granularity (step size) for the Y-axis

        // Disable legend
        chart.legend.isEnabled = false


    }

    //add entry to each graph and has color for each graph
    private fun addEntryToChart(chart: LineChart, entries: ArrayList<Entry>, color: String) {

        val dataSet = LineDataSet(entries, when (chart) {
            xChart -> "X"
            yChart -> "Y"
            else -> "Z"
        })

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
        clearChartData(xChart)
        clearChartData(yChart)
        clearChartData(zChart)
        xEntries = ArrayList()
        yEntries = ArrayList()
        zEntries = ArrayList()

        // Unregister the listener to avoid memory leaks
        unregisterScanResultListener(this)
        onBackPressed()
        return true
    }


    override fun onPause() {
        super.onPause()
        unregisterScanResultListener(this)
    }

    override fun onResume() {
        super.onResume()
        registerScanResultListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterScanResultListener(this)
    }

    private fun clearChartData(chart: LineChart) {
        chart.clear() // Clear the chart's data
        chart.invalidate() // Refresh the chart to reflect the changes
    }
    fun addNewData(x: Float, y: Float, z: Float) {
        time += 1
        xEntries.add(Entry(time, x))
        yEntries.add(Entry(time, y))
        zEntries.add(Entry(time, z))

        // Limit the size of the entries
        if (xEntries.size > 30) xEntries.removeAt(0)
        if (yEntries.size > 30) yEntries.removeAt(0)
        if (zEntries.size > 30) zEntries.removeAt(0)

        // Update charts less frequently
        if ((time % 5).toInt() == 0) {
            addEntryToChart(xChart, xEntries, "#FF0000") // Red for X
            addEntryToChart(yChart, yEntries, "#FFFF00") // Yellow for Y
            addEntryToChart(zChart, zEntries, "#0000FF") // Blue for Z
        }
    }


}


