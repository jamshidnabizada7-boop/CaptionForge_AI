package com.captionforge.android

import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.Project
import com.captionforge.android.model.SampleProjectsFactory
import com.captionforge.android.model.SrtParser
import com.captionforge.android.ui.theme.isRtlScript
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionForgeModelsTest {

    @Test
    fun `test RTL script detection for Persian Pashto Urdu Arabic and Latin`() {
        assertTrue(isRtlScript("به کپشن فورج خوش آمدید"))
        assertTrue(isRtlScript("په خیر راغلاست"))
        assertTrue(isRtlScript("خوش آمدید اردو"))
        assertTrue(isRtlScript("مرحبا بكم"))
        assertFalse(isRtlScript("Welcome to CaptionForge"))
    }

    @Test
    fun `test SRT formatting and parsing roundtrip`() {
        val cues = listOf(
            CaptionCue(id = "c1", start = 0.5, end = 2.75, text = "Hello CaptionForge"),
            CaptionCue(id = "c2", start = 3.0, end = 5.5, text = "به کپشن فورج خوش آمدید")
        )
        val srt = SrtParser.exportSrt(cues)
        assertTrue(srt.contains("00:00:00,500 --> 00:00:02,750"))
        assertTrue(srt.contains("به کپشن فورج خوش آمدید"))

        val parsed = SrtParser.parseSrt(srt)
        assertEquals(2, parsed.size)
        assertEquals("Hello CaptionForge", parsed[0].text)
        assertEquals(0.5, parsed[0].start, 0.01)
        assertEquals(2.75, parsed[0].end, 0.01)
        assertEquals("به کپشن فورج خوش آمدید", parsed[1].text)
    }

    @Test
    fun `test project serialization roundtrip`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val project = SampleProjectsFactory.createVideo1ShowcaseProject()
        val encoded = json.encodeToString(project)
        val decoded = json.decodeFromString<Project>(encoded)
        assertEquals(project.id, decoded.id)
        assertEquals(project.name, decoded.name)
        assertEquals(project.tracks.size, decoded.tracks.size)
        assertEquals(project.overlays.size, decoded.overlays.size)
    }
}
