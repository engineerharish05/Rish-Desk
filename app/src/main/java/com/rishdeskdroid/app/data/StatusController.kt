package com.rishdeskdroid.app.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.Date

/** Everything the custom status bar shows. */
data class StatusState(
    val carrier: String = "",
    val time: String = "",
    val date: String = "",
    val bluetoothOn: Boolean = false,
    val wifiOn: Boolean = false,
    val hasCellular: Boolean = false,
    val signalLevel: Int = 4,
    val batteryPercent: Int = 100,
    val charging: Boolean = false,
)

/** Listens to system broadcasts and network callbacks and keeps [state] fresh. */
class StatusController(context: Context) {

    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    var state by mutableStateOf(StatusState())
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    fun start() {
        if (started) return
        started = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        try {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = postRefresh()
                override fun onLost(network: Network) = postRefresh()
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = postRefresh()
            }
            cm.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: Exception) {
            // Status bar still works without live network updates.
        }

        refresh()
    }

    fun stop() {
        if (!started) return
        started = false
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
        }
        networkCallback?.let {
            try {
                context.getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it)
            } catch (e: Exception) {
            }
        }
        networkCallback = null
    }

    private fun postRefresh() {
        handler.post { refresh() }
    }

    fun refresh() {
        // Battery (sticky broadcast, no receiver needed to read it once)
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val chargeStatus = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else 100
        val charging = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            chargeStatus == BatteryManager.BATTERY_STATUS_FULL

        // Cellular
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val simReady = try {
            telephony?.simState == TelephonyManager.SIM_STATE_READY
        } catch (e: Exception) {
            false
        }
        val carrier = try {
            telephony?.networkOperatorName.orEmpty()
        } catch (e: Exception) {
            ""
        }
        var signal = 4
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                signal = telephony?.signalStrength?.level ?: 4
            } catch (e: Exception) {
                signal = 4
            }
        }

        // Bluetooth
        val bluetoothOn = try {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
        } catch (e: Exception) {
            false
        }

        // Wi-Fi
        val wifiOn = try {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) {
            false
        }

        state = StatusState(
            carrier = carrier,
            time = DateFormat.getTimeFormat(context).format(Date()),
            date = DateFormat.format("EEE, d MMM", Date()).toString(),
            bluetoothOn = bluetoothOn,
            wifiOn = wifiOn,
            hasCellular = simReady,
            signalLevel = signal.coerceIn(0, 4),
            batteryPercent = percent,
            charging = charging,
        )
    }
}
