package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.AdDinTheme
import com.example.ui.theme.AppColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the redesigned screens (day + night) to build/reports/layout-screens for visual review. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w392dp-h860dp-xxhdpi")
class ScreenshotTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val out = File(System.getProperty("user.dir"), "build/reports/layout-screens").apply { mkdirs() }

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1500)
        val v = rule.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun allScreens_dayAndNight() {
        val vm = MainViewModel(ApplicationProvider.getApplicationContext<Application>())
        var night by mutableStateOf(false)
        var screen by mutableStateOf("onboarding")
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AdDinTheme(isNight = night) {
                Box(Modifier.fillMaxSize().background(AppColors.BackgroundBrush)) {
                    when (screen) {
                        "onboarding" -> OnboardingScreen(vm) { }
                        "home" -> HomeScreen(vm, {}, {}, {})
                        "wakt" -> PrayerTimesScreen(vm)
                        "tracker" -> TrackerScreen(vm)
                        "azkhar" -> AzkharScreen(vm)
                        else -> MoreScreen(vm)
                    }
                }
            }
        }
        for (s in listOf("onboarding", "home", "wakt", "tracker", "azkhar", "more")) {
            screen = s
            shot("${s}_day")
        }
        night = true
        for (s in listOf("home", "wakt", "more")) {
            screen = s
            shot("${s}_night")
        }
    }
}
