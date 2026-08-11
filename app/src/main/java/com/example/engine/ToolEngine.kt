package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ToolActionResult(
    val success: Boolean,
    val message: String,
    val actionType: String,
    val details: String? = null
)

class ToolEngine(private val context: Context) {

    fun executeAppLaunch(targetName: String, aliasMapping: String? = null): ToolActionResult {
        val appNameToFind = (aliasMapping ?: targetName).trim().lowercase()

        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        var matchedPackage: String? = null
        var matchedLabel: String? = null

        // First pass: exact title match
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase()
            val pkg = info.activityInfo.packageName
            if (label == appNameToFind || pkg.lowercase().contains(appNameToFind)) {
                matchedPackage = pkg
                matchedLabel = info.loadLabel(pm).toString()
                break
            }
        }

        // Second pass: contains match
        if (matchedPackage == null) {
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase()
                if (label.contains(appNameToFind) || appNameToFind.contains(label)) {
                    matchedPackage = info.activityInfo.packageName
                    matchedLabel = info.loadLabel(pm).toString()
                    break
                }
            }
        }

        // Common package mapping fallback
        if (matchedPackage == null) {
            val commonApps = mapOf(
                "youtube" to "com.google.android.youtube",
                "yt" to "com.google.android.youtube",
                "tiktok" to "com.zhiliaoapp.musically",
                "firefox" to "org.mozilla.firefox",
                "chrome" to "com.android.chrome",
                "gmail" to "com.google.android.gm",
                "maps" to "com.google.android.apps.maps",
                "spotify" to "com.spotify.music",
                "settings" to "com.android.settings",
                "camera" to "com.android.camera"
            )

            val packageId = commonApps[appNameToFind]
            if (packageId != null) {
                try {
                    pm.getPackageInfo(packageId, 0)
                    matchedPackage = packageId
                    matchedLabel = appNameToFind.replaceFirstChar { it.uppercase() }
                } catch (e: Exception) {
                    Log.w("ToolEngine", "Fallback package $packageId not installed")
                }
            }
        }

        if (matchedPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolActionResult(
                    success = true,
                    message = "Opening ${matchedLabel ?: targetName}.",
                    actionType = "OPEN_APP",
                    details = "Package: $matchedPackage"
                )
            }
        }

        // Special system intent fallbacks
        if (appNameToFind.contains("setting")) {
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return ToolActionResult(
                success = true,
                message = "Opening Android Settings.",
                actionType = "OPEN_SETTINGS"
            )
        }

        return ToolActionResult(
            success = false,
            message = "I couldn't find or open '$targetName' on your device.",
            actionType = "OPEN_APP"
        )
    }

    fun executeWebSearch(query: String): ToolActionResult {
        return try {
            val url = "https://www.google.com/search?q=" + Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolActionResult(
                success = true,
                message = "Searching web for '$query'.",
                actionType = "SEARCH_WEB"
            )
        } catch (e: Exception) {
            ToolActionResult(
                success = false,
                message = "Unable to launch browser for search: ${e.localizedMessage}",
                actionType = "SEARCH_WEB"
            )
        }
    }

    fun getSystemInfo(): ToolActionResult {
        val timeFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val isOnline = caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))

        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val gigabytesAvailable = bytesAvailable / (1024 * 1024 * 1024)

        val info = "System Status:\n• Time: $currentTime\n• Battery: $batteryPct%\n• Network: ${if (isOnline) "Connected" else "Offline"}\n• Free Storage: ${gigabytesAvailable} GB"

        return ToolActionResult(
            success = true,
            message = info,
            actionType = "SYSTEM_INFO"
        )
    }

    fun evaluateMath(expression: String): ToolActionResult {
        return try {
            val cleanExpr = expression.replace("x", "*").replace("×", "*").replace("÷", "/").replace("?", "").trim()
            val result = simpleEvaluate(cleanExpr)
            ToolActionResult(
                success = true,
                message = "$cleanExpr = $result",
                actionType = "CALCULATE"
            )
        } catch (e: Exception) {
            ToolActionResult(
                success = false,
                message = "Unable to calculate expression '$expression'.",
                actionType = "CALCULATE"
            )
        }
    }

    private fun simpleEvaluate(expr: String): String {
        val parts = expr.split("+", "-", "*", "/")
        if (expr.contains("+")) {
            val p = expr.split("+")
            return (p[0].trim().toDouble() + p[1].trim().toDouble()).toString()
        }
        if (expr.contains("-")) {
            val p = expr.split("-")
            return (p[0].trim().toDouble() - p[1].trim().toDouble()).toString()
        }
        if (expr.contains("*")) {
            val p = expr.split("*")
            return (p[0].trim().toDouble() * p[1].trim().toDouble()).toString()
        }
        if (expr.contains("/")) {
            val p = expr.split("/")
            return (p[0].trim().toDouble() / p[1].trim().toDouble()).toString()
        }
        return expr
    }
}
