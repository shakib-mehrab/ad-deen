package com.example.data

import android.content.Context

/** Small UI-only preferences (not synced): onboarding state and the last location for the startup theme. */
class UiPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("addin_ui_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) = prefs.edit().putBoolean("onboarding_done", v).apply()
}
