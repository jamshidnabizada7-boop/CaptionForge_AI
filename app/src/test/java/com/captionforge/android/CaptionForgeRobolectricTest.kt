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
        composeTestRule.onNodeWithText("CaptionForge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("new_project_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_video1_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_video2_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("backend_status_chip").assertIsDisplayed()
    }

    @Test
    fun `2_verify_bundled_video_and_22_sfx_wav_assets_are_packaged`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rootAssets = context.assets.list("")?.toList().orEmpty()
        assertTrue("sample_video.mp4 must be bundled in assets", rootAssets.contains("sample_video.mp4"))

        val sfxAssets = context.assets.list("sfx")?.filter { it.endsWith(".wav") }.orEmpty()
        assertEquals("All 22 studio WAV sound effects must be bundled in assets/sfx", 22, sfxAssets.size)
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
    fun `4_verify_subtitles_rtl_cues_split_merge_and_srt_export`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = CaptionForgeViewModel(app)
        vm.openProject(SampleProjectsFactory.createVideo1ShowcaseProject())
        drainMainLooper()

        val initialCount = vm.activeProject.value!!.tracks[0].captions.size
        assertEquals(3, initialCount)

        // Add Persian RTL cue at 2.0s
        vm.updatePlaybackTime(2.0, fromUserSeek = true)
        vm.addCaptionAtPlayhead("زیرنویس آزمایشی جدید برای تست")
        drainMainLooper()
        assertEquals(4, vm.activeProject.value!!.tracks[0].captions.size)

        val addedCueId = vm.selectedCueId.value
        assertNotNull(addedCueId)

        // Edit cue text & timing
        vm.updateCue(addedCueId!!, "ویرایش متن فارسی با موفقیت انجام شد", 2.0, 3.2)
        val updatedCue = vm.activeProject.value!!.tracks[0].captions.first { it.id == addedCueId }
        assertEquals("ویرایش متن فارسی با موفقیت انجام شد", updatedCue.text)

        // Split cue at playhead
        vm.updatePlaybackTime(2.6, fromUserSeek = true)
        vm.splitSelectedCueAtPlayhead()
        assertEquals(5, vm.activeProject.value!!.tracks[0].captions.size)

        // Merge split cue back with next
        vm.mergeSelectedCueWithNext()
        assertEquals(4, vm.activeProject.value!!.tracks[0].captions.size)

        // Delete selected cue
        vm.deleteSelectedCue()
        assertEquals(3, vm.activeProject.value!!.tracks[0].captions.size)

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
}
