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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rishdeskdroid.app.data.AppRepository
import com.rishdeskdroid.app.data.IconLoader
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.OrientationGuard
import com.rishdeskdroid.app.data.RecentStore
import com.rishdeskdroid.app.data.WidgetController
import com.rishdeskdroid.app.data.dockApps
import com.rishdeskdroid.app.data.launchApp
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
    private var focusSearch by mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // The device list can lag the broadcast by a moment, so check again shortly after.
            handler.postDelayed({ updateOtg() }, 400)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart
            // An updated or removed app can have a new icon, so forget the cached one
            IconLoader.invalidate(pkg)
            repo.reloadSoon()
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
        RecentStore.ensureLoaded(this)

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
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            GlassTheme {
                val status = rememberStatus()
                LaunchedEffect(prefs.showSystemStatusBar) { applyStatusBar() }
                LauncherRoot(
                    prefs = prefs,
                    apps = repo.apps,
                    widgets = widgets,
                    status = status,
                    screen = screen,
                    otgConnected = otgConnected,
                    focusSearch = focusSearch,
                    onSearchFocusHandled = { focusSearch = false },
                    onScreenChange = { screen = it },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgets.start()
        // Put back the user's rotation settings if an app was opened in forced landscape
        OrientationGuard.restore(this)
        repo.reload()
        updateOtg()
    }

    override fun onStop() {
        widgets.stop()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        repo.close()
        try {
            unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
        }
        try {
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
        }
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
        if (hasFocus) applyStatusBar()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgets.onActivityResult(requestCode, resultCode)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Esc acts like Back, so it closes menus, search, edit mode and screens in the usual order
        if (prefs.keyboardEnabled &&
            event.keyCode == KeyEvent.KEYCODE_ESCAPE &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && isShortcut(event)) {
            if (!prefs.keyboardEnabled) {
                Toast.makeText(this, "Keyboard shortcuts are off (Settings > Keyboard and mouse)", Toast.LENGTH_SHORT).show()
                return super.dispatchKeyEvent(event)
            }
            if (handleShortcut(event)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** Shortcuts use Ctrl, Alt or the Meta (Windows/Command) key, so plain typing is never captured. */
    private fun hasModifier(event: KeyEvent): Boolean =
        event.isCtrlPressed || event.isAltPressed || event.isMetaPressed

    private fun isShortcut(event: KeyEvent): Boolean {
        if (!hasModifier(event)) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_SLASH,
            KeyEvent.KEYCODE_COMMA, KeyEvent.KEYCODE_H -> true
            in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> true
            else -> false
        }
    }

    private fun handleShortcut(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_D -> {
                focusSearch = true
                screen = Screen.Drawer
                true
            }
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_SLASH -> {
                focusSearch = true
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
     * By default the design draws its own status bar and Android's is hidden (swipe down from the top
     * edge to peek at it and pull down notifications). The setting can show Android's bar instead.
     */
    private fun applyStatusBar() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (prefs.showSystemStatusBar) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}
