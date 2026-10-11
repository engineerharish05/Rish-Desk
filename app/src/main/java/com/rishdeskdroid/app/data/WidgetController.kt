package com.rishdeskdroid.app.data

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Hosts Android app widgets on the home screen.
 *
 * Adding a widget is a three step flow: allocate an id, get permission to bind it
 * (Android shows a system dialog the first time), and run the widget's own configuration
 * screen if it has one. Only then is the id saved.
 */
class WidgetController(
    private val activity: ComponentActivity,
    private val prefs: LauncherPrefs,
) {
    val host = AppWidgetHost(activity.applicationContext, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)

    private var pendingId = -1

    private val bindLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            configureOrFinish(pendingId)
        } else {
            cancelPending()
        }
    }

    fun start() {
        try {
            host.startListening()
        } catch (e: Exception) {
        }
        validate()
    }

    fun stop() {
        try {
            host.stopListening()
        } catch (e: Exception) {
        }
    }

    /** Drops saved widgets whose provider app has been uninstalled. */
    private fun validate() {
        val valid = prefs.widgetIds.filter { infoOf(it) != null }
        if (valid.size != prefs.widgetIds.size) prefs.updateWidgetIds(valid)
    }

    fun infoOf(id: Int): AppWidgetProviderInfo? = try {
        manager.getAppWidgetInfo(id)
    } catch (e: Exception) {
        null
    }

    fun providers(): List<AppWidgetProviderInfo> {
        val pm = activity.packageManager
        return manager.installedProviders.sortedBy { it.loadLabel(pm).lowercase() }
    }

    fun beginAdd(info: AppWidgetProviderInfo) {
        if (pendingId != -1) cancelPending()
        val id = host.allocateAppWidgetId()
        pendingId = id
        if (manager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            configureOrFinish(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            try {
                bindLauncher.launch(intent)
            } catch (e: Exception) {
                cancelPending()
            }
        }
    }

    private fun configureOrFinish(id: Int) {
        val info = manager.getAppWidgetInfo(id)
        if (info?.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQ_CONFIGURE, null)
            } catch (e: Exception) {
                finish(id)
            }
        } else {
            finish(id)
        }
    }

    /** Forward from the activity's onActivityResult. */
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode != REQ_CONFIGURE) return
        if (resultCode == Activity.RESULT_OK) finish(pendingId) else cancelPending()
    }

    private fun finish(id: Int) {
        if (id == -1) return
        prefs.updateWidgetIds(prefs.widgetIds + id)
        pendingId = -1
    }

    private fun cancelPending() {
        if (pendingId != -1) {
            try {
                host.deleteAppWidgetId(pendingId)
            } catch (e: Exception) {
            }
        }
        pendingId = -1
    }

    fun remove(id: Int) {
        try {
            host.deleteAppWidgetId(id)
        } catch (e: Exception) {
        }
        prefs.updateWidgetIds(prefs.widgetIds - id)
    }

    private companion object {
        const val HOST_ID = 0x474C41 // "GLA"
        const val REQ_CONFIGURE = 4711
    }
}

/** One widget cell is this many dp wide and tall. */
const val WIDGET_UNIT_DP = 80
private const val MAX_SPAN_W = 6
private const val MAX_SPAN_H = 2

/** The sizes (in widget cells) a widget may take, taken from the provider's own resize rules. */
data class SpanLimits(
    val minW: Int,
    val maxW: Int,
    val minH: Int,
    val maxH: Int,
    val defaultW: Int,
    val defaultH: Int,
) {
    val resizable: Boolean get() = minW != maxW || minH != maxH

    fun clamp(size: WidgetSize?): WidgetSize {
        val w = (size?.w ?: defaultW).coerceIn(minW, maxW)
        val h = (size?.h ?: defaultH).coerceIn(minH, maxH)
        return WidgetSize(w, h)
    }
}

fun widgetSpanLimits(info: AppWidgetProviderInfo, density: Float): SpanLimits {
    fun spans(px: Int): Int = kotlin.math.ceil(px / density / WIDGET_UNIT_DP).toInt().coerceAtLeast(1)

    val defaultW = spans(info.minWidth).coerceIn(1, MAX_SPAN_W)
    val defaultH = spans(info.minHeight).coerceIn(1, MAX_SPAN_H)

    val horizontal = info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val vertical = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0

    var minW = defaultW
    var maxW = defaultW
    if (horizontal) {
        val smallest = if (info.minResizeWidth > 0) info.minResizeWidth else info.minWidth
        minW = spans(smallest).coerceIn(1, defaultW)
        maxW = MAX_SPAN_W
        if (android.os.Build.VERSION.SDK_INT >= 31 && info.maxResizeWidth > 0) {
            maxW = (info.maxResizeWidth / density / WIDGET_UNIT_DP).toInt().coerceIn(defaultW, MAX_SPAN_W)
        }
    }
    var minH = defaultH
    var maxH = defaultH
    if (vertical) {
        val smallest = if (info.minResizeHeight > 0) info.minResizeHeight else info.minHeight
        minH = spans(smallest).coerceIn(1, defaultH)
        maxH = MAX_SPAN_H
        if (android.os.Build.VERSION.SDK_INT >= 31 && info.maxResizeHeight > 0) {
            maxH = (info.maxResizeHeight / density / WIDGET_UNIT_DP).toInt().coerceIn(defaultH, MAX_SPAN_H)
        }
    }
    return SpanLimits(minW, maxW, minH, maxH, defaultW, defaultH)
}
