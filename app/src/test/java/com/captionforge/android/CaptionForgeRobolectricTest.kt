package com.captionforge.android

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.captionforge.android.data.ProbedVideoInfo
import com.captionforge.android.data.ProjectRepository
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.SampleProjectsFactory
import com.captionforge.android.model.SrtParser
import com.captionforge.android.model.StylePresetCatalog
import com.captionforge.android.ui.theme.FontCatalog
import com.captionforge.android.ui.theme.resolveBundledFontResId
import com.captionforge.android.viewmodel.CaptionForgeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptionForgeRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun drainMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `1_verify_main_activity_launches_and_renders_home_screen`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MagicCap").assertIsDisplayed()
        composeTestRule.onNodeWithTag("new_project_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_video1_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_video2_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("backend_status_chip").assertIsDisplayed()
    }

    @Test
    fun `2_verify_26_multi_script_fonts_and_60_sfx_presets_with_waveforms`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rootAssets = context.assets.list("")?.toList().orEmpty()
        assertTrue("sample_video.mp4 must be bundled in assets", rootAssets.contains("sample_video.mp4"))

        // Verify all 26 fonts in FontCatalog resolve to real bundled R.font resource IDs
        val allFonts = FontCatalog.allFonts()
        assertEquals("FontCatalog must contain 26 curated multi-script fonts", 26, allFonts.size)
        allFonts.forEach { fontItem ->
            val resolvedRes = resolveBundledFontResId(fontItem.name)
            assertTrue("Font ${fontItem.name} must resolve to bundled R.font resource", resolvedRes != 0)
            assertTrue("Font ${fontItem.name} must support at least 2 weights", fontItem.supportedWeights.size >= 2)
        }

        // Verify script coverage for English, Persian/Dari, Pashto, Urdu, and Arabic
        listOf("en", "fa", "ps", "ur", "ar").forEach { scriptCode ->
            val matching = allFonts.filter { it.matchesScript(scriptCode) }
            assertTrue("Must have multiple fonts supporting script $scriptCode", matching.size >= 2)
        }

        // Verify 60 studio SFX + Music presets across all 23 categories with 32-bar real waveforms
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        drainMainLooper()

        assertEquals("SfxPlayerManager must expose 60 distinct sound presets", 60, vm.sfxPlayer.presets.size)
        assertEquals("SfxPlayerManager must cover All + 23 studio sound categories", 24, vm.sfxPlayer.categories.size)

        val popPreset = vm.sfxPlayer.presets.first { it.id == "pop_crisp" }
        val bars = popPreset.waveformBars
        assertEquals("Waveform preview must compute 32 normalized amplitude bars", 32, bars.size)
        assertTrue("Waveform bars must have non-zero acoustic energy", bars.any { it > 0.2f })
    }

    @Test
    fun `3_verify_video_import_playback_trim_and_clip_splitting`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        drainMainLooper()

        val tempVideo = File(app.cacheDir, "imported_test_clip.mp4").apply {
            writeBytes(ByteArray(2048) { (it % 127).toByte() })
        }
        val probed = ProbedVideoInfo(
            filename = "interview_reel.mp4",
            localFile = tempVideo,
            durationSec = 8.0,
            width = 1080,
            height = 1920,
            sizeBytes = tempVideo.length()
        )

        val createdProject = vm.repository.createProjectFromVideo(probed, trimStart = 0.5, trimEnd = 6.5)
        vm.openProject(createdProject)
        drainMainLooper()

        val active = vm.activeProject.value
        assertNotNull(active)
        assertEquals("interview_reel", active!!.name)
        assertEquals(0.5, active.trim.start, 0.01)
        assertEquals(6.5, active.trim.end, 0.01)

        // Verify playback & playhead clamping within trim bounds
        vm.setPlaying(true)
        assertTrue(vm.isPlaying.value)
        vm.updatePlaybackTime(12.0, fromUserSeek = true)
        assertEquals(6.5, vm.currentTime.value, 0.01)

        // Move playhead inside clip and split video clip
        vm.updatePlaybackTime(3.0, fromUserSeek = true)
        assertEquals(1, vm.activeProject.value!!.videoClips.size)
        vm.splitVideoClipAtPlayhead()
        drainMainLooper()
        assertEquals(2, vm.activeProject.value!!.videoClips.size)

        // Cycle transition on second clip
        val secondClipId = vm.activeProject.value!!.videoClips[1].id
        assertEquals("Purple Flash", vm.activeProject.value!!.videoClips[1].transition)
        vm.cycleClipTransition(secondClipId)
        assertEquals("Cut", vm.activeProject.value!!.videoClips[1].transition)
    }

    @Test
    fun `4_verify_reference_video1_three_tier_persian_kinetic_style_and_word_inspector`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        val showcase = SampleProjectsFactory.createVideo1ShowcaseProject()
        vm.openProject(showcase)
        drainMainLooper()

        val track = vm.activeProject.value!!.tracks[0]
        assertTrue("Showcase project must enable 3-tier Persian kinetic layout", track.style.threeTierLayout)
        assertEquals("Vazirmatn", track.style.font)
        assertEquals("#FF7A00", track.style.textColor)
        assertEquals("#FFB800", track.style.secondaryColor)
        assertTrue("3D extrusion must be active for Reference Video 1 hero text", track.style.extrusion3D >= 3.0)

        val firstCue = track.captions[0]
        val tiers = firstCue.resolveThreeTiers()
        assertEquals("ویزای تحصیلی", tiers.first)
        assertEquals("فنلاند", tiers.second)
        assertEquals("بدون مدرک زبان", tiers.third)

        // Verify word-level custom color, scale boost, animation, and synchronized SFX
        val heroWord = firstCue.words.first { it.text == "فنلاند" }
        assertEquals("#FF7A00", heroWord.customColor)
        assertEquals(1.35, heroWord.scaleBoost, 0.01)
        assertEquals("Pop", heroWord.animation)
        assertEquals("Punchy Pop", heroWord.sfx)

        // Verify dragging caption position directly on the canvas updates coordinates
        vm.selectCue(firstCue.id)
        vm.updateCuePositionFromCanvas(0.42, 0.68)
        val movedCue = vm.activeProject.value!!.tracks[0].captions.first { it.id == firstCue.id }
        assertEquals(0.42, movedCue.customStyle!!.xPos, 0.01)
        assertEquals(0.68, movedCue.customStyle!!.yPos, 0.01)

        // Verify 16 studio style presets and custom preset saving
        assertEquals(16, StylePresetCatalog.presets.size)
        vm.saveCurrentStyleAsCustomPreset("My Custom Dari Kinetic")
        assertEquals(1, vm.activeProject.value!!.customStylePresets.size)

        // Export SRT sidecar file
        vm.exportSrtSidecar()
        drainMainLooper()
        val srtFile = vm.exportState.value.exportedSrtFile
        assertNotNull("SRT file should be generated locally", srtFile)
        assertTrue(srtFile!!.exists())
        val parsedBack = SrtParser.parseSrt(srtFile.readText())
        assertEquals(3, parsedBack.size)
    }

    @Test
    fun `5_verify_overlays_keyframes_sfx_autosave_and_undo_redo`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        vm.openProject(SampleProjectsFactory.createVideo2VfxShowcaseProject())
        drainMainLooper()

        val initialOverlays = vm.activeProject.value!!.overlays.size
        val initialAudio = vm.activeProject.value!!.audioClips.size

        // Add Graphic Overlay
        vm.updatePlaybackTime(1.0, fromUserSeek = true)
        vm.addGraphicOverlay("glow_hologram", "Test Hologram", "◈ HOLO ◈", "#38BDF8")
        assertEquals(initialOverlays + 1, vm.activeProject.value!!.overlays.size)

        // Add keyframe to overlay
        val addedOv = vm.activeProject.value!!.overlays.last()
        val withKf = addedOv.copy(
            keyframes = listOf(
                OverlayKeyframe(time = 0.0, xPos = 0.3, yPos = 0.3, scale = 1.0),
                OverlayKeyframe(time = 1.5, xPos = 0.7, yPos = 0.6, scale = 1.25)
            )
        )
        vm.updateOverlay(withKf)
        assertEquals(2, vm.activeProject.value!!.overlays.last().keyframes.size)

        // Add SFX audio clip at playhead
        vm.addSfxClipAtPlayhead("Whoosh")
        assertEquals(initialAudio + 1, vm.activeProject.value!!.audioClips.size)

        // Verify Undo restores previous state (removes SFX clip)
        assertTrue(vm.canUndo.value)
        vm.undo()
        assertEquals(initialAudio, vm.activeProject.value!!.audioClips.size)

        // Verify Redo re-applies SFX clip
        assertTrue(vm.canRedo.value)
        vm.redo()
        assertEquals(initialAudio + 1, vm.activeProject.value!!.audioClips.size)

        // Verify Autosave persisted to local disk repository
        drainMainLooper()
        val freshRepo = ProjectRepository(app)
        val reloaded = freshRepo.getLocalProject(vm.activeProject.value!!.id)
        assertNotNull("Project must be autosaved to disk", reloaded)
        assertEquals(initialAudio + 1, reloaded!!.audioClips.size)
        assertEquals(initialOverlays + 1, reloaded.overlays.size)
    }

    @Test
    fun `6_verify_first_acceptance_test_end_to_end_on_newly_imported_video`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        drainMainLooper()

        // 1. Import a new video
        val tempVideo = File(app.cacheDir, "acceptance_imported_video.mp4").apply {
            app.assets.open("sample_video.mp4").use { input ->
                outputStream().use { output -> input.copyTo(output) }
            }
        }
        val probed = ProbedVideoInfo(
            filename = "persian_creator_reel.mp4",
            localFile = tempVideo,
            durationSec = 9.0,
            width = 720,
            height = 1280,
            sizeBytes = tempVideo.length()
        )
        val importedProject = vm.repository.createProjectFromVideo(probed, trimStart = 0.0, trimEnd = 9.0)
        vm.openProject(importedProject)
        drainMainLooper()

        // 2. Generate Persian subtitles from speech
        vm.triggerAiTranscription(language = "fa", model = "base")
        Thread.sleep(250)
        drainMainLooper()

        // 3. Verify accurate caption segments & word timings in the timeline
        val segmentedTrack = vm.activeProject.value!!.tracks.first()
        assertEquals("fa", segmentedTrack.language)
        assertTrue("Must generate multiple time-aligned Persian caption segments", segmentedTrack.captions.size >= 3)
        val firstCue = segmentedTrack.captions.first()
        assertTrue("First cue must have word-level timings", firstCue.words.size >= 4)
        assertTrue("Word timings must fall within cue bounds", firstCue.words.all { it.start >= firstCue.start && it.end <= firstCue.end + 0.05 })

        // 4. Open visual style gallery and apply "Cinematic Orange Persian"
        val cinematicOrange = StylePresetCatalog.findByNameOrId("Cinematic Orange Persian")
        assertNotNull("Cinematic Orange Persian preset must exist in catalog", cinematicOrange)
        vm.applyStylePreset(cinematicOrange!!, applyToEntireTrack = true)
        drainMainLooper()
        assertEquals("Cinematic Orange Persian", vm.activeProject.value!!.tracks.first().style.preset)
        assertTrue(vm.activeProject.value!!.tracks.first().style.threeTierLayout)

        // 5. Open font gallery and select a real Persian font ("Lalezar")
        vm.selectFontForActiveTrack("Lalezar", 900)
        drainMainLooper()
        assertEquals("Lalezar", vm.activeProject.value!!.tracks.first().style.font)

        // 6. Edit and animate an individual word (index 2: "فنلاند")
        vm.selectCue(firstCue.id)
        vm.selectWordIndex(2)
        vm.updateWordInCue(firstCue.id, 2) { w ->
            w.copy(
                text = "فنلاند",
                customColor = "#FF7A00",
                scaleBoost = 1.40,
                animation = "Bounce"
            )
        }
        drainMainLooper()

        // 7. Add a professional whoosh sound ("Fast Whoosh") to that word in Word mode
        vm.updateVideoEffects { it.copy(sfxMode = "Word") }
        vm.selectWordIndex(2)
        vm.auditionAndSelectSfx("Fast Whoosh")
        drainMainLooper()

        val updatedWord = vm.activeProject.value!!.tracks.first().captions.first().words[2]
        assertEquals("فنلاند", updatedWord.text)
        assertEquals("Bounce", updatedWord.animation)
        assertEquals(1.40, updatedWord.scaleBoost, 0.01)
        assertEquals("Fast Whoosh", updatedWord.sfx)

        // 8. Preview the video with caption animation and word-synchronized sound
        vm.setPlaying(true)
        vm.updatePlaybackTime(updatedWord.start, fromUserSeek = false)
        assertTrue("Playback at word start must trigger SFX pulse", vm.sfxPulse.value)
        vm.setPlaying(false)

        // 9. Save and reopen the project, verifying full persistence
        val savedProjectId = vm.activeProject.value!!.id
        val reloadedFromDisk = ProjectRepository(app).getLocalProject(savedProjectId)
        assertNotNull("Saved project must reload from disk", reloadedFromDisk)
        vm.openProject(reloadedFromDisk!!)
        drainMainLooper()
        val reopenedTrack = vm.activeProject.value!!.tracks.first()
        assertEquals("Cinematic Orange Persian", reopenedTrack.style.preset)
        assertEquals("Lalezar", reopenedTrack.style.font)
        assertEquals("Fast Whoosh", reopenedTrack.captions.first().words[2].sfx)

        // 10. Export project (verifying SRT sidecar + OnDeviceMp4Renderer frame burn-in)
        vm.exportSrtSidecar()
        drainMainLooper()
        assertNotNull("Export pipeline must generate SRT sidecar", vm.exportState.value.exportedSrtFile)
        assertTrue(vm.exportState.value.exportedSrtFile!!.exists())

        val renderer = com.captionforge.android.export.OnDeviceMp4Renderer(app)
        val renderedBitmap = renderer.renderOverlayFrameBitmap(
            project = vm.activeProject.value!!,
            timelineSec = updatedWord.start + 0.05,
            width = 720,
            height = 1280
        )
        assertEquals(720, renderedBitmap.width)
        assertEquals(1280, renderedBitmap.height)
    }
}
