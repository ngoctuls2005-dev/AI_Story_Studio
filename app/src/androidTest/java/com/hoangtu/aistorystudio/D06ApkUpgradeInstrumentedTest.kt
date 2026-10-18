package com.hoangtu.aistorystudio

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class D06ApkUpgradeInstrumentedTest {
    @Test
    fun reservedAppDataFixtureCanBeVerifiedAcrossUpgrade() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val markerFile = File(context.cacheDir, MARKER_FILE_NAME)
        val phase = InstrumentationRegistry.getArguments().getString("phase")

        when (phase) {
            "seed" -> {
                assertFalse("Reserved D-06 fixture already exists.", markerFile.exists())
                markerFile.writeText(MARKER_CONTENT, Charsets.UTF_8)
                assertEquals(MARKER_CONTENT, markerFile.readText(Charsets.UTF_8))
            }
            "verify" -> {
                assertTrue("D-06 fixture was not retained after APK update.", markerFile.isFile)
                assertEquals(MARKER_CONTENT, markerFile.readText(Charsets.UTF_8))
                assertTrue(markerFile.delete())
            }
            "cleanup" -> removeFixtureIfOwned(markerFile)
            else -> {
                if (markerFile.exists()) removeFixtureIfOwned(markerFile)
                markerFile.writeText(MARKER_CONTENT, Charsets.UTF_8)
                assertEquals(MARKER_CONTENT, markerFile.readText(Charsets.UTF_8))
                assertTrue(markerFile.delete())
            }
        }
    }

    private fun removeFixtureIfOwned(markerFile: File) {
        if (markerFile.exists()) {
            assertEquals(MARKER_CONTENT, markerFile.readText(Charsets.UTF_8))
            assertTrue(markerFile.delete())
        }
    }

    private companion object {
        const val MARKER_FILE_NAME = "d06-apk-upgrade-fixture.txt"
        const val MARKER_CONTENT = "AI Story Studio test fixture D06 v1"
    }
}
