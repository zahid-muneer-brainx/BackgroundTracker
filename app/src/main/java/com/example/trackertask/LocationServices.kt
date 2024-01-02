package com.example.trackertask

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService: Service() {


    private var bluetoothAdapter: BluetoothAdapter? = null
    private var systemBluetoothManager: BluetoothManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private var notificationManager: NotificationManager? = null

    private var bluetoothLeScanner : BluetoothLeScanner? = null

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        // Initializes Bluetooth adapter
        systemBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (systemBluetoothManager != null && systemBluetoothManager?.adapter != null) {
            bluetoothAdapter = systemBluetoothManager?.adapter
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        }
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, "locations", NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        bluetoothLeScanner?.startScan(leScanCallback)
    }

    private fun start() {
        locationClient
            .getLocationUpdates(SCAN_PERIOD)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                scanLeDevice()
                val lat = location.latitude.toString().takeLast(6)
                val long = location.longitude.toString().takeLast(6)
                val updatedNotification = getNotification().setContentText(
                    "Location: ($lat, $long)"
                )
                notificationManager?.notify(NOTIFICATION_ID, updatedNotification.build())
            }
            .launchIn(serviceScope)

        startForeground(NOTIFICATION_ID, getNotification().build())
    }

    @SuppressLint("MissingPermission")
    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        bluetoothLeScanner?.stopScan(leScanCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "12345"
        const val NOTIFICATION_ID=12345
    }

    private fun getNotification(): NotificationCompat.Builder {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            notification.setChannelId(CHANNEL_ID)
        }
        return notification
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("ScanResult", result.device.address)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach{
                Log.d("ScanResult", it.device.address)
            }
        }
    }
}