package com.rishdeskdroid.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rishdeskdroid.app.data.AppRepository
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.WidgetController
import com.rishdeskdroid.app.data.dockApps
import com.rishdeskdroid.app.data.launchApp
import com.rishdeskdroid.app.data.shouldReloadAppsForPackageAction
import com.rishdeskdroid.app.ui.GlassTheme
import com.rishdeskdroid.app.ui.LauncherRoot
import com.rishdeskdroid.app.ui.Screen
import com.rishdeskdroid.app.ui.rememberStatus

class MainActivity : ComponentActivity() {

    private lateinit var prefs: LauncherPrefs
    private lateinit var repo: AppRepository
    private lateinit var widgets: WidgetController

    private var screen by mutableStateOf(Screen.Home)
    private var otgConnected by mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())
    private val packageReloadRunnable = Runnable {
        if (::repo.isInitialized) repo.reload()
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // The device list can lag the broadcast by a moment, so check again shortly after.
            handler.postDelayed({ updateOtg() }, 400)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!shouldReloadAppsForPackageAction(intent?.action)) return

            // Replacements may emit several package broadcasts in quick succession.
            // Coalesce them into one scan after the package manager has settled.
            handler.removeCallbacks(packageReloadRunnable)
            handler.postDelayed(packageReloadRunnable, PACKAGE_RELOAD_DEBOUNCE_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        prefs = LauncherPrefs(this)
        repo = AppRepository(this)
        widgets = WidgetController(this, prefs)

        ContextCompat.registerReceiver(
            this,
            usbReceiver,
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            GlassTheme {
                val status = rememberStatus()
                LauncherRoot(
                    prefs = prefs,
                    apps = repo.apps,
                    widgets = widgets,
                    status = status,
                    screen = screen,
                    otgConnected = otgConnected,
                    onScreenChange = { screen = it },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgets.start()
        repo.reload()
        updateOtg()
    }

    override fun onStop() {
        widgets.stop()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(usbReceiver)
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
        }
        repo.close()
        super.onDestroy()
    }

    /** Pressing the Home button while the launcher is open always returns to the home screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            screen = Screen.Home
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && HIDE_SYSTEM_STATUS_BAR) hideSystemStatusBar()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgets.onActivityResult(requestCode, resultCode)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (prefs.keyboardEnabled &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0 &&
            handleShortcut(event)
        ) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleShortcut(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (screen == Screen.Home) return false
            screen = Screen.Home
            return true
        }
        if (!event.isCtrlPressed) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_D -> {
                screen = Screen.Drawer
                true
            }
            KeyEvent.KEYCODE_COMMA -> {
                screen = Screen.Settings
                true
            }
            KeyEvent.KEYCODE_H -> {
                screen = Screen.Home
                true
            }
            in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> {
                dockApps(prefs, repo.apps)
                    .getOrNull(event.keyCode - KeyEvent.KEYCODE_1)
                    ?.let { launchApp(this, it) }
                true
            }
            else -> false
        }
    }

    private fun updateOtg() {
        otgConnected = try {
            getSystemService(UsbManager::class.java)?.deviceList?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * The design draws its own status bar, so Android's is hidden. Swipe down from the top edge
     * to peek at it (and pull down notifications).
     */
    private fun hideSystemStatusBar() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }

    private companion object {
        const val PACKAGE_RELOAD_DEBOUNCE_MS = 250L

        /** Set to false to keep Android's own status bar instead of the custom one. */
        const val HIDE_SYSTEM_STATUS_BAR = true
    }
}
