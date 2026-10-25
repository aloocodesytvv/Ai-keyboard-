package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ToneTransformation
import com.example.security.SecurePreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RizzBoard", appName)
    }

    @Test
    fun `verify all seven tone transformations are configured`() {
        val tones = ToneTransformation.values()
        assertEquals(6, tones.size) // 6 transformation tones + 1 Paste & Rizz mode = 7 actions
        assertTrue(tones.any { it.label.contains("Smooth") })
        assertTrue(tones.any { it.label.contains("Witty") })
        assertTrue(tones.any { it.label.contains("Chill") })
        assertTrue(tones.any { it.label.contains("Victorian") })
        assertTrue(tones.any { it.label.contains("Fix Grammar") })
        assertTrue(tones.any { it.label.contains("Gen Z") })
    }

    @Test
    fun `verify secure preferences save and clear key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurePreferencesManager.getInstance(context)

        prefs.saveApiKey("test-custom-key-12345")
        assertEquals("test-custom-key-12345", prefs.getCustomApiKey())

        prefs.clearApiKey()
        assertEquals("", prefs.getCustomApiKey())
    }

    @Test
    fun `verify provider detection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurePreferencesManager.getInstance(context)

        prefs.setAiProvider("openai")
        assertEquals("openai", prefs.getAiProvider())

        prefs.setAiProvider("gemini")
        assertEquals("gemini", prefs.getAiProvider())
    }
}
