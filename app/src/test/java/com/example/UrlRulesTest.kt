package com.example

import com.example.data.UiPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** User-added Quran websites: only plausible https web addresses are accepted. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UrlRulesTest {
    @Test
    fun acceptsAndNormalisesWebAddresses() {
        assertEquals("https://quran.example.com", UiPrefs.normaliseUrl("quran.example.com"))
        assertEquals("https://example.org/read", UiPrefs.normaliseUrl("http://example.org/read"))
        assertEquals("https://sunnah.com", UiPrefs.normaliseUrl("  https://sunnah.com "))
    }

    @Test
    fun rejectsUnsafeOrInvalidInput() {
        assertNull(UiPrefs.normaliseUrl("javascript:alert(1)"))
        assertNull(UiPrefs.normaliseUrl("file:///sdcard/x.html"))
        assertNull(UiPrefs.normaliseUrl("intent://scan"))
        assertNull(UiPrefs.normaliseUrl("not a url"))
        assertNull(UiPrefs.normaliseUrl("localhost"))
        assertNull(UiPrefs.normaliseUrl(""))
    }
}
