package se.floreteng.spotlightblocker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class BlockedApp(
    val packageName: String,
    val appName: String,
    val blockedStrings: List<String>
)

object AppSettings {
    private const val PREFS_NAME = "blocker_settings"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private val gson = Gson()

    fun getBlockedApps(context: Context): List<BlockedApp> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BLOCKED_APPS, null)

        return if (json != null) {
            val type = object : TypeToken<List<BlockedApp>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Default to Snapchat
            listOf(
                BlockedApp(
                    packageName = "com.snapchat.android",
                    appName = "Snapchat",
                    blockedStrings = listOf("View Profile", "For you")
                )
            )
        }
    }

    fun saveBlockedApps(context: Context, apps: List<BlockedApp>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(apps)
        prefs.edit().putString(KEY_BLOCKED_APPS, json).apply()
    }
}
