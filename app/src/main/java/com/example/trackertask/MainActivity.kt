package com.example.trackertask

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.trackertask.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val LOCATION_PERMISSION_CODE = 101
    private val BACKGROUND_LOCATION_PERMISSION_CODE = 102
    private lateinit var viewBinding : ActivityMainBinding
    val permissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_ADMIN,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        checkPermission()
        requestBluetoothPermissions()

    }

    private fun requestBluetoothPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                ),
                0
            )
        }
        else{
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                ),
                0
            )
        }

    }

    fun startTracking(view: View){
        if (hasRequiredPermission()){
            Intent(this, LocationService::class.java).apply {
                action = LocationService.ACTION_START
                startService(this)
            }

        }
        else askForLocationPermission()
    }

    fun stopTracking(view: View){
        if (hasRequiredPermission()){
            Intent(this, LocationService::class.java).apply {
                action = LocationService.ACTION_STOP
                stopService(this)
            }

        }
    }

    private fun checkPermission() {
        if (!hasRequiredPermission()){
            askForLocationPermission()
        }
    }

    private fun askForLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("Permission Needed!")
                .setMessage("Location Permission Needed!")
                .setPositiveButton("OK"
                ) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_CODE
                    )
                }
                .setNegativeButton("CANCEL") { _, _ ->
                    // Permission is denied by the user
                }
                .create().show()
        }
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    private fun askPermissionForBackgroundUsage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("Permission Needed!")
                .setMessage("Background Location Permission Needed!, tap \"Allow all time in the next screen\"")
                .setPositiveButton("OK",
                    DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf<String>(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            BACKGROUND_LOCATION_PERMISSION_CODE
                        )
                    })
                .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog, which ->
                    // User declined for Background Location Permission.
                })
                .create().show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // Background Location Permission is granted so do your work here
                    } else {
                        // Ask for Background Location Permission
                        askPermissionForBackgroundUsage()
                    }
                }
            } else {
                // User denied location permission
            }
        }
    }
}