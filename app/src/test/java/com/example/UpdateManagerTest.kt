package com.example

import com.example.update.UpdateManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** In-app updater: only well-formed manifests pointing at GitHub over https are accepted. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateManagerTest {

    private val sha = "a".repeat(64)
    private fun json(url: String, sha256: String = sha, extra: String = "") =
        """{"versionCode":2,"versionName":"1.0.1","apkUrl":"$url","sha256":"$sha256","notes":"Fixes"$extra}"""

    @Test
    fun parsesValidManifest() {
        val m = UpdateManager.parseManifest(json("https://github.com/shakib-mehrab/ad-deen/releases/download/v1.0.1/Ad-Deen-1.0.1.apk", extra = ""","minSupportedVersionCode":2"""))
        assertNotNull(m)
        m!!
        assertEquals(2, m.versionCode)
        assertEquals("1.0.1", m.versionName)
        assertEquals("Fixes", m.notesEn)
        assertEquals("Fixes", m.notesBn) // falls back to English notes
        assertTrue(m.isMandatoryFor(1))
        assertFalse(m.isMandatoryFor(2))
    }

    @Test
    fun rejectsUnsafeOrBrokenManifests() {
        assertNull(UpdateManager.parseManifest(json("http://github.com/x.apk")))        // not https
        assertNull(UpdateManager.parseManifest(json("https://evil.example.com/x.apk"))) // not GitHub
        assertNull(UpdateManager.parseManifest(json("https://github.com/x.apk", sha256 = "123"))) // bad checksum
        assertNull(UpdateManager.parseManifest("not json"))
        assertNull(UpdateManager.parseManifest("""{"versionCode":2}"""))
    }

    @Test
    fun updaterIsOnForGithubBuilds() {
        assertTrue(BuildConfig.SELF_UPDATE)
        assertTrue(BuildConfig.UPDATE_MANIFEST_URL.startsWith("https://raw.githubusercontent.com/shakib-mehrab/ad-deen/"))
        assertTrue(UpdateManager.isEnabled)
    }
}
