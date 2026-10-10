package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A website the user added to the Quran browser. */
data class QuranSite(val name: String, val url: String)

/** Small UI-only preferences (not synced): onboarding state and user-added Quran websites. */
class UiPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("addin_ui_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) = prefs.edit().putBoolean("onboarding_done", v).apply()

    var quranSites: List<QuranSite>
        get() = runCatching {
            val arr = JSONArray(prefs.getString("quran_sites", "[]"))
            List(arr.length()) { i -> arr.getJSONObject(i).let { QuranSite(it.getString("name"), it.getString("url")) } }
        }.getOrDefault(emptyList())
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) }
            prefs.edit().putString("quran_sites", arr.toString()).apply()
        }

    companion object {
        /**
         * Normalises user input to an https URL, or returns null if it isn't a plausible web address.
         * Plain http is upgraded to https; other schemes (javascript:, file:, intent:) are rejected.
         */
        fun normaliseUrl(input: String): String? {
            var u = input.trim()
            if (u.isEmpty() || u.contains(' ')) return null
            if (u.startsWith("http://", ignoreCase = true)) u = "https://" + u.substring(7)
            if (!u.startsWith("https://", ignoreCase = true)) {
                if (u.contains("://") || u.contains(':') && !u.substringBefore('/').contains('.')) return null
                u = "https://$u"
            }
            val host = runCatching { android.net.Uri.parse(u).host }.getOrNull() ?: return null
            return if (host.contains('.') && host.length > 3) u else null
        }
    }
}
