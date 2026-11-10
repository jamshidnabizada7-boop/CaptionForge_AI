package com.captionforge.android

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.captionforge.android.audio.SfxPlayerManager
import com.captionforge.android.data.ProbedVideoInfo
import com.captionforge.android.data.ProjectRepository
import com.captionforge.android.export.OnDeviceMp4Renderer
import com.captionforge.android.model.AudioClip
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.RtlTextShaper
import com.captionforge.android.model.SampleProjectsFactory
import com.captionforge.android.model.SrtParser
import com.captionforge.android.model.StylePresetCatalog
import com.captionforge.android.ui.theme.FontCatalog
import com.captionforge.android.ui.theme.resolveBundledFontResId
import com.captionforge.android.viewmodel.CaptionForgeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptionForgeRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        ProjectRepository.clearRegisteredAsrEngines()
    }

    private fun drainMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun `1_verify_main_activity_launches_and_renders_home_screen`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CaptionForge").assertIsDisplayed()
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
        assertTrue("Showcase track must be explicitly marked as a demo track", track.isDemoTrack)
        assertEquals("demo_showcase", track.transcriptionSource)
        assertEquals("Vazirmatn", track.style.font)
        assertEquals("#FF7A00", track.style.textColor)
        assertEquals("#FFB800", track.style.secondaryColor)
        assertTrue("3D extrusion must be active for Reference Video 1 hero text", track.style.extrusion3D >= 3.0)

        val firstCue = track.captions[0]
        assertTrue("Showcase cue must be explicitly marked as a demo subtitle", firstCue.isDemoSubtitle)
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
        assertEquals(4, parsedBack.size)
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

        val renderer = OnDeviceMp4Renderer(app)
        val renderedBitmap = renderer.renderOverlayFrameBitmap(
            project = vm.activeProject.value!!,
            timelineSec = updatedWord.start + 0.05,
            width = 720,
            height = 1280
        )
        assertEquals(720, renderedBitmap.width)
        assertEquals(1280, renderedBitmap.height)
    }

    @Test
    fun `7_regression_asr_vs_explicit_demo_subtitles_and_arbitrary_video_transcription`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        drainMainLooper()

        // Part A: Verify offline fallback subtitles are explicitly labeled as [DEMO] and never disguised as real ASR
        ProjectRepository.clearRegisteredAsrEngines()
        val unindexedVideo = File(app.cacheDir, "unindexed_clip_${System.currentTimeMillis()}.mp4").apply {
            writeBytes(ByteArray(1024) { 0x42 })
        }
        val probedDemo = ProbedVideoInfo(
            filename = "raw_clip_no_asr.mp4",
            localFile = unindexedVideo,
            durationSec = 6.0,
            width = 720,
            height = 1280,
            sizeBytes = unindexedVideo.length()
        )
        val demoFallbackProject = vm.repository.createProjectFromVideo(probedDemo, 0.0, 6.0)
        val fallbackResult = vm.repository.buildOfflineSpeechTranscription(demoFallbackProject, "fa")
        val demoTrack = fallbackResult.tracks.first()

        assertTrue("Offline fallback track MUST set isDemoTrack = true", demoTrack.isDemoTrack)
        assertEquals("demo_template", demoTrack.transcriptionSource)
        assertTrue("Offline fallback track name MUST start with [DEMO]", demoTrack.name.startsWith("[DEMO]"))
        assertTrue("All offline fallback cues MUST set isDemoSubtitle = true", demoTrack.captions.all { it.isDemoSubtitle })
        assertTrue("All offline fallback cues MUST have transcriptionSource == demo_template", demoTrack.captions.all { it.transcriptionSource == "demo_template" })
        assertTrue("All offline fallback cues MUST carry explicit [DEMO] prefix", demoTrack.captions.all { it.text.startsWith("[DEMO]") })

        // Part B: Verify arbitrary imported videos get genuine non-demo transcription via a supported ASR engine
        val customAsrEngine = object : ProjectRepository.SupportedAsrEngine {
            override val engineId: String = "whisper_on_device_asr"
            override suspend fun transcribe(
                videoFile: File,
                trimStart: Double,
                trimEnd: Double,
                language: String?,
                model: String,
                onProgress: (String) -> Unit
            ): List<CaptionCue> {
                val text1 = "این ویدیو با موتور تشخیص گفتار واقعی زیرنویس شده است"
                val text2 = "دقت زمان‌بندی کلمات در سطح حرفه‌ای بررسی می‌شود"
                return listOf(
                    CaptionCue(
                        id = "asr_1",
                        start = trimStart,
                        end = trimStart + 2.8,
                        text = text1,
                        words = SrtParser.buildWordTimings(text1, trimStart, trimStart + 2.8),
                        isDemoSubtitle = false,
                        transcriptionSource = engineId
                    ),
                    CaptionCue(
                        id = "asr_2",
                        start = trimStart + 2.9,
                        end = trimEnd,
                        text = text2,
                        words = SrtParser.buildWordTimings(text2, trimStart + 2.9, trimEnd),
                        isDemoSubtitle = false,
                        transcriptionSource = engineId
                    )
                )
            }
        }
        ProjectRepository.registerAsrEngine(customAsrEngine)

        val arbitraryVideo = File(app.cacheDir, "arbitrary_user_interview.mp4").apply {
            writeBytes(ByteArray(2048) { (it % 97).toByte() })
        }
        val probedArbitrary = ProbedVideoInfo(
            filename = "arbitrary_user_interview.mp4",
            localFile = arbitraryVideo,
            durationSec = 6.0,
            width = 1080,
            height = 1920,
            sizeBytes = arbitraryVideo.length()
        )
        val arbitraryProject = vm.repository.createProjectFromVideo(probedArbitrary, 0.0, 6.0)
        vm.openProject(arbitraryProject)
        drainMainLooper()

        vm.triggerAiTranscription(language = "fa", model = "small")
        Thread.sleep(200)
        drainMainLooper()

        val asrTrack = vm.activeProject.value!!.tracks.first()
        assertFalse("Real ASR track MUST have isDemoTrack = false", asrTrack.isDemoTrack)
        assertEquals("whisper_on_device_asr", asrTrack.transcriptionSource)
        assertFalse("Real ASR track name must NOT contain [DEMO]", asrTrack.name.contains("[DEMO]"))
        assertEquals(2, asrTrack.captions.size)
        assertTrue("Real ASR cues MUST have isDemoSubtitle = false", asrTrack.captions.all { !it.isDemoSubtitle })
        assertTrue("Real ASR cues MUST record whisper_on_device_asr source", asrTrack.captions.all { it.transcriptionSource == "whisper_on_device_asr" })
        assertFalse("Real ASR cue text must NOT start with [DEMO]", asrTrack.captions.first().text.startsWith("[DEMO]"))
    }

    @Test
    fun `8_regression_renderProjectSfxStemWav_decodes_and_mixes_user_imported_audio_files`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sfxManager = SfxPlayerManager(app)

        // Create a real 16-bit PCM WAV file (22050 Hz) with two distinct amplitude sections:
        // - [0.0s .. 1.0s]: Low-amplitude 440 Hz tone (peak = 0.12)
        // - [1.0s .. 3.0s]: High-amplitude 880 Hz tone (peak = 0.85)
        val sampleRate = 22050
        val totalInputSec = 3.0
        val totalInputSamples = (totalInputSec * sampleRate).roundToInt()
        val pcmBuffer = ByteBuffer.allocate(44 + totalInputSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        pcmBuffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        pcmBuffer.putInt(36 + totalInputSamples * 2)
        pcmBuffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        pcmBuffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        pcmBuffer.putInt(16)
        pcmBuffer.putShort(1) // PCM
        pcmBuffer.putShort(1) // Mono
        pcmBuffer.putInt(sampleRate)
        pcmBuffer.putInt(sampleRate * 2)
        pcmBuffer.putShort(2)
        pcmBuffer.putShort(16)
        pcmBuffer.put("data".toByteArray(Charsets.US_ASCII))
        pcmBuffer.putInt(totalInputSamples * 2)

        for (i in 0 until totalInputSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal = if (t < 1.0) {
                0.12 * sin(2.0 * PI * 440.0 * t)
            } else {
                0.85 * sin(2.0 * PI * 880.0 * t)
            }
            val pcm16 = (sampleVal * 32767.0).roundToInt().coerceIn(-32767, 32767).toShort()
            pcmBuffer.putShort(pcm16)
        }

        val userImportedWav = File(app.cacheDir, "user_imported_voiceover.wav").apply {
            writeBytes(pcmBuffer.array())
        }

        // Configure an AudioClip referencing userImportedWav:
        // - start = 1.0s on timeline
        // - trimStart = 1.0s (skips the quiet 0..1s section and reads the loud 0.85 amplitude section!)
        // - duration = 1.6s (plays from 1.0s to 2.6s on timeline)
        // - volume = 0.90, fadeIn = 0.30s, fadeOut = 0.30s
        val userAudioClip = AudioClip(
            id = "aud_user_test",
            name = "User Imported Recording",
            category = "Imported Audio",
            source = userImportedWav.absolutePath,
            start = 1.0,
            duration = 1.6,
            trimStart = 1.0,
            volume = 0.90,
            fadeIn = 0.30,
            fadeOut = 0.30,
            ducking = false,
            loop = false
        )

        val baseProject = SampleProjectsFactory.createVideo1ShowcaseProject().copy(
            duration = 4.0,
            trim = com.captionforge.android.model.VideoTrim(0.0, 4.0),
            tracks = emptyList(), // Isolate audioClips stem
            audioClips = listOf(userAudioClip),
            effects = com.captionforge.android.model.VideoEffects(sfx = "None", sfxVolume = 100)
        )

        val outStemFile = File(app.cacheDir, "regression_mixed_stem.wav")
        sfxManager.renderProjectSfxStemWav(baseProject, outStemFile)
        assertTrue("Rendered stem WAV must exist", outStemFile.exists())

        val decodedStem = sfxManager.decodeRiffWavBytes(outStemFile.readBytes(), sampleRate)
        assertNotNull("Rendered stem WAV must decode cleanly", decodedStem)
        val samples = decodedStem!!

        fun rmsInRange(startSec: Double, endSec: Double): Float {
            val sIdx = (startSec * sampleRate).roundToInt().coerceIn(0, samples.size - 1)
            val eIdx = (endSec * sampleRate).roundToInt().coerceIn(sIdx + 1, samples.size)
            var sumSq = 0.0
            for (i in sIdx until eIdx) {
                sumSq += samples[i] * samples[i]
            }
            return sqrt(sumSq / (eIdx - sIdx)).toFloat()
        }

        val preClipRms = rmsInRange(0.10, 0.85)
        val fadeInEarlyRms = rmsInRange(1.02, 1.12)
        val sustainRms = rmsInRange(1.40, 2.10)
        val fadeOutLateRms = rmsInRange(2.45, 2.58)
        val postClipRms = rmsInRange(2.70, 3.80)

        assertTrue("Must be silent before clip.start (expected < 0.001, got $preClipRms)", preClipRms < 0.001f)
        assertTrue(
            "Sustain RMS ($sustainRms) must reflect trimmed high-amplitude section (> 0.38) rather than untrimmed 0.12 section",
            sustainRms > 0.38f
        )
        assertTrue("Fade-in early RMS ($fadeInEarlyRms) must be lower than sustain RMS ($sustainRms)", fadeInEarlyRms < sustainRms * 0.65f)
        assertTrue("Fade-out late RMS ($fadeOutLateRms) must be lower than sustain RMS ($sustainRms)", fadeOutLateRms < sustainRms * 0.65f)
        assertTrue("Must be silent after clip.start + clip.duration (expected < 0.001, got $postClipRms)", postClipRms < 0.001f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `9_regression_videoClips_sequence_trim_split_reorder_transitions_and_three_tier_rtl_rendering`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        drainMainLooper()

        val sourceVideo = File(app.cacheDir, "multi_clip_source.mp4").apply {
            writeBytes(ByteArray(4096) { (it % 113).toByte() })
        }
        val probed = ProbedVideoInfo(
            filename = "multi_clip_source.mp4",
            localFile = sourceVideo,
            durationSec = 9.0,
            width = 720,
            height = 1280,
            sizeBytes = sourceVideo.length()
        )
        val project = vm.repository.createProjectFromVideo(probed, trimStart = 0.0, trimEnd = 9.0)
        vm.openProject(project)
        drainMainLooper()

        // 1. Split at 3.0s -> [Clip A: 0..3s, Clip B: 3..9s]
        vm.updatePlaybackTime(3.0, fromUserSeek = true)
        vm.splitVideoClipAtPlayhead()
        drainMainLooper()
        assertEquals(2, vm.activeProject.value!!.videoClips.size)

        // 2. Split at 6.0s -> [Clip A: 0..3s, Clip B1: 3..6s, Clip B2: 6..9s]
        vm.updatePlaybackTime(6.0, fromUserSeek = true)
        vm.splitVideoClipAtPlayhead()
        drainMainLooper()
        assertEquals(3, vm.activeProject.value!!.videoClips.size)

        val clip0 = vm.activeProject.value!!.videoClips[0] // sourceTrimStart = 0.0, dur = 3.0
        val clip1 = vm.activeProject.value!!.videoClips[1] // sourceTrimStart = 3.0, dur = 3.0
        val clip2 = vm.activeProject.value!!.videoClips[2] // sourceTrimStart = 6.0, dur = 3.0

        // 3. Trim middle clip (clip1) to start at 3.5s in source with 2.0s duration
        vm.trimVideoClipSegment(clip1.id, newSourceTrimStart = 3.5, newDurationSec = 2.0)
        drainMainLooper()

        // 4. Reorder last clip (index 2, sourceTrimStart = 6.0s) to the front (index 0)
        vm.reorderVideoClips(fromIndex = 2, toIndex = 0)
        drainMainLooper()

        // 5. Verify sequence export plan reflects reordering, trimming, splits, and transitions
        val plan = OnDeviceMp4Renderer.buildVideoClipSequencePlan(vm.activeProject.value!!)
        assertEquals("Export sequence plan must include all 3 split/reordered clips", 3, plan.size)

        // First exported segment must be the reordered clip2 (source 6.0s..9.0s, output 0.0s..3.0s)
        assertEquals(clip2.id, plan[0].clipId)
        assertEquals(6000L, plan[0].sourceStartMs)
        assertEquals(9000L, plan[0].sourceEndMs)
        assertEquals(0.0, plan[0].outputStartSec, 0.01)
        assertEquals(3.0, plan[0].outputEndSec, 0.01)

        // Second exported segment must be clip0 (source 0.0s..3.0s, output 3.0s..6.0s)
        assertEquals(clip0.id, plan[1].clipId)
        assertEquals(0L, plan[1].sourceStartMs)
        assertEquals(3000L, plan[1].sourceEndMs)
        assertEquals(3.0, plan[1].outputStartSec, 0.01)
        assertEquals(6.0, plan[1].outputEndSec, 0.01)

        // Third exported segment must be trimmed clip1 (source 3.5s..5.5s, output 6.0s..8.0s)
        assertEquals(clip1.id, plan[2].clipId)
        assertEquals(3500L, plan[2].sourceStartMs)
        assertEquals(5500L, plan[2].sourceEndMs)
        assertEquals(6.0, plan[2].outputStartSec, 0.01)
        assertEquals(8.0, plan[2].outputEndSec, 0.01)
        assertEquals("Purple Flash", plan[2].transition)

        // 6. Verify 3-tier Persian `captionLayer` distinct per-tier stroke, drop-shadow, inner glow & RTL shaping
        val showcase = SampleProjectsFactory.createVideo1ShowcaseProject()
        val style = showcase.tracks.first().style
        val tier1Layer = style.resolveTierLayerStyle(1)
        val tier2HeadlineLayer = style.resolveTierLayerStyle(2)
        val tier3Layer = style.resolveTierLayerStyle(3)

        assertTrue(
            "Hero headline (Tier 2) must have heavier stroke (${tier2HeadlineLayer.strokeWidth}) than sub-headlines (${tier1Layer.strokeWidth})",
            tier2HeadlineLayer.strokeWidth > tier1Layer.strokeWidth
        )
        assertTrue(
            "Hero headline (Tier 2) must have deeper drop-shadow (${tier2HeadlineLayer.dropShadowDepth}) than sub-headlines (${tier1Layer.dropShadowDepth})",
            tier2HeadlineLayer.dropShadowDepth > tier1Layer.dropShadowDepth
        )
        assertTrue(
            "Hero headline (Tier 2) must have stronger inner glow (${tier2HeadlineLayer.innerGlowRadius}) than sub-headlines (${tier3Layer.innerGlowRadius})",
            tier2HeadlineLayer.innerGlowRadius > tier3Layer.innerGlowRadius
        )

        // Verify RTL shaping normalizes Arabic Yeh/Kaf to Persian Yeh/Kaf while preserving ZWNJ (\u200C)
        val rawArabicMixed = "ويزاي تحصيلي در فنلاند مي‌شود"
        val normalizedPersian = RtlTextShaper.normalizePersianText(rawArabicMixed)
        assertEquals("ویزای تحصیلی در فنلاند می‌شود", normalizedPersian)
        assertTrue("Must detect RTL script accurately", RtlTextShaper.containsRtl(normalizedPersian))

        // Verify OnDeviceMp4Renderer renders both the 3-tier Persian captionLayer and clip transition frames
        val renderer = OnDeviceMp4Renderer(app)
        val captionBitmap = renderer.renderOverlayFrameBitmap(
            project = showcase,
            timelineSec = 1.2,
            width = 720,
            height = 1280
        )
        var nonTransparentCaptionPixels = 0
        for (y in 500 until 1050 step 8) {
            for (x in 100 until 620 step 8) {
                if (Color.alpha(captionBitmap.getPixel(x, y)) > 20) {
                    nonTransparentCaptionPixels++
                }
            }
        }
        assertTrue("3-tier Persian captionLayer must render visible styled glyph pixels ($nonTransparentCaptionPixels)", nonTransparentCaptionPixels > 25)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `10_export_12_second_persian_cinematic_demo_assets_with_imported_audio_and_3tier_overlays`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sfxManager = SfxPlayerManager(app)
        val outDir = File("build/speech_gen").apply { mkdirs() }

        // 1. Synthesize a real user-imported stereo/mono WAV audio file on disk (13.0s @ 22050 Hz)
        val sampleRate = 22050
        val totalSec = 13.0
        val numSamples = (totalSec * sampleRate).roundToInt()
        val wavBuf = ByteBuffer.allocate(44 + numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        wavBuf.put("RIFF".toByteArray(Charsets.US_ASCII))
        wavBuf.putInt(36 + numSamples * 2)
        wavBuf.put("WAVE".toByteArray(Charsets.US_ASCII))
        wavBuf.put("fmt ".toByteArray(Charsets.US_ASCII))
        wavBuf.putInt(16)
        wavBuf.putShort(1)
        wavBuf.putShort(1)
        wavBuf.putInt(sampleRate)
        wavBuf.putInt(sampleRate * 2)
        wavBuf.putShort(2)
        wavBuf.putShort(16)
        wavBuf.put("data".toByteArray(Charsets.US_ASCII))
        wavBuf.putInt(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Warm cinematic D-minor harmonic bed (D3=146.83Hz, A3=220Hz, F4=349.23Hz) with gentle pulse
            val pulse = 0.65 + 0.35 * sin(2.0 * PI * 1.5 * t)
            val chord = (
                0.45 * sin(2.0 * PI * 146.83 * t) +
                0.35 * sin(2.0 * PI * 220.00 * t) +
                0.20 * sin(2.0 * PI * 349.23 * t)
            ) * pulse * 0.55
            val s16 = (chord * 32767.0).roundToInt().coerceIn(-32767, 32767).toShort()
            wavBuf.putShort(s16)
        }

        val userImportedBedFile = File(outDir, "user_imported_bed.wav").apply {
            writeBytes(wavBuf.array())
        }

        // 2. Build 12-second Persian cinematic project with 2 videoClips + Purple Flash transition + imported audio clip
        val baseShowcase = SampleProjectsFactory.createVideo1ShowcaseProject()
        val importedAudioClip = AudioClip(
            id = "aud_user_bed_12s",
            name = "User Imported Cinematic Bed",
            category = "Imported Audio",
            source = userImportedBedFile.absolutePath,
            start = 0.0,
            duration = 12.0,
            trimStart = 0.5,
            volume = 0.34,
            fadeIn = 0.60,
            fadeOut = 0.80,
            ducking = true,
            loop = false,
            isMusic = true
        )
        val splitClips = listOf(
            com.captionforge.android.model.VideoClipSegment(
                id = "vc_demo_part1",
                label = "Studio Intro & Name",
                sourceUri = baseShowcase.source,
                startInTimeline = 0.0,
                endInTimeline = 8.5,
                sourceTrimStart = 0.0,
                transition = "Cut"
            ),
            com.captionforge.android.model.VideoClipSegment(
                id = "vc_demo_part2",
                label = "CTA & Purple Flash",
                sourceUri = baseShowcase.source,
                startInTimeline = 8.5,
                endInTimeline = 12.0,
                sourceTrimStart = 8.5,
                transition = "Purple Flash"
            )
        )
        val demo12sProject = baseShowcase.copy(
            duration = 12.0,
            trim = com.captionforge.android.model.VideoTrim(0.0, 12.0),
            videoClips = splitClips,
            audioClips = baseShowcase.audioClips + importedAudioClip
        )

        val stemOut = File(outDir, "rendered_sfx_and_imported_stem.wav")
        sfxManager.renderProjectSfxStemWav(demo12sProject, stemOut)
        assertTrue("12s mixed SFX + imported audio stem must exist", stemOut.exists() && stemOut.length() > 100_000L)

        // 3. Render 3-tier Persian kinetic overlay bitmaps at key timestamps for inspection & compositing
        val renderer = OnDeviceMp4Renderer(app)
        val sampleTimestamps = listOf(
            "overlay_c1_finland.png" to 1.20,
            "overlay_c2_nazia.png" to 4.20,
            "overlay_c3_migration.png" to 7.20,
            "overlay_transition_purple_flash.png" to 8.62,
            "overlay_c4_comment.png" to 10.20
        )
        sampleTimestamps.forEach { (filename, tSec) ->
            val bmp = renderer.renderOverlayFrameBitmap(demo12sProject, tSec, 720, 1280)
            val pngFile = File(outDir, filename)
            pngFile.outputStream().use { out ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            assertTrue("Rendered overlay PNG $filename must be non-empty", pngFile.exists() && pngFile.length() > 5000L)
        }
    }
}
