package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Guards against blank logos: the real Android drawables must draw the gold crescent and cream mosque. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LogoRenderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun render(resId: Int, name: String): Bitmap {
        val d = ContextCompat.getDrawable(context, resId)!!
        val bmp = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, 216, 216)
        d.draw(Canvas(bmp))
        File(System.getProperty("user.dir"), "build/reports/layout-screens").apply { mkdirs() }
            .resolve("$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    private fun Bitmap.countNear(rgb: Int): Int {
        var n = 0
        for (x in 0 until width) for (y in 0 until height) {
            val p = getPixel(x, y)
            if ((p ushr 24) < 200) continue
            val dr = ((p shr 16) and 0xFF) - ((rgb shr 16) and 0xFF)
            val dg = ((p shr 8) and 0xFF) - ((rgb shr 8) and 0xFF)
            val db = (p and 0xFF) - (rgb and 0xFF)
            if (dr * dr + dg * dg + db * db < 900) n++
        }
        return n
    }

    @Test
    fun inAppLogo_drawsCrescentAndMosque() {
        val b = render(R.drawable.ic_app_logo, "logo_in_app")
        assertTrue("gold crescent missing", b.countNear(0xFFB030) > 2000)
        assertTrue("cream mosque missing", b.countNear(0xFFF4D6) > 1000)
    }

    @Test
    fun adaptiveForeground_drawsArtwork_andMonochromeIsNotEmpty() {
        val fg = render(R.drawable.ic_launcher_foreground, "launcher_foreground")
        assertTrue(fg.countNear(0xFFB030) > 1000)
        assertTrue(fg.countNear(0xFFF4D6) > 500)
        val mono = render(R.drawable.ic_launcher_monochrome, "launcher_monochrome")
        assertTrue(mono.countNear(0xFFFFFF) > 1500)
    }
}
