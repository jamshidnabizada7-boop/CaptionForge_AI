package com.captionforge.android.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.captionforge.android.audio.SfxPlayerManager
import com.captionforge.android.data.ProbedVideoInfo
import com.captionforge.android.data.ProjectRepository
import com.captionforge.android.export.OnDeviceMp4Renderer
import com.captionforge.android.model.AudioClip
import com.captionforge.android.model.BackendConfig
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.Project
import com.captionforge.android.model.SampleProjectsFactory
import com.captionforge.android.model.SrtParser
import com.captionforge.android.model.SubtitleStyle
import com.captionforge.android.model.Track
import com.captionforge.android.model.VideoClipSegment
import com.captionforge.android.model.VideoEffects
import com.captionforge.android.model.VideoTrim
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class EditorTool(val label: String) {
    STYLES("Style"),
    TEMPLATES("Templates"),
    OVERLAYS("Layers"),
    CLIPS("Clips"),
    FORMAT("Format"),
    FONTS("Font"),
    COLORS("Color"),
    ANIMATIONS("Animation"),
    TRANSLATE("Translate"),
    SFX("SFX"),
    TIMING("Timing"),
    FILTERS("Filters"),
    CLEAR_VOICE("Voice"),
    FOLLOW("Follow")
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val progressMessage: String? = null,
    val downloadUrl: String? = null,
    val exportedMp4File: File? = null,
    val exportedSrtFile: File? = null,
    val error: String? = null
)

class CaptionForgeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ProjectRepository(application.applicationContext)
    val sfxPlayer = SfxPlayerManager(application.applicationContext)
    private val onDeviceMp4Renderer = OnDeviceMp4Renderer(application.applicationContext)

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _isLoadingProjects = MutableStateFlow(false)
    val isLoadingProjects: StateFlow<Boolean> = _isLoadingProjects.asStateFlow()

    private val _isBackendOnline = MutableStateFlow(false)
    val isBackendOnline: StateFlow<Boolean> = _isBackendOnline.asStateFlow()

    private val _backendConfig = MutableStateFlow<BackendConfig?>(null)
    val backendConfig: StateFlow<BackendConfig?> = _backendConfig.asStateFlow()

    private val _backendUrl = MutableStateFlow(repository.getBackendUrl())
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    private val _activeTrackIndex = MutableStateFlow(0)
    val activeTrackIndex: StateFlow<Int> = _activeTrackIndex.asStateFlow()

    private val _selectedCueId = MutableStateFlow<String?>(null)
    val selectedCueId: StateFlow<String?> = _selectedCueId.asStateFlow()

    private val _selectedOverlayId = MutableStateFlow<String?>(null)
    val selectedOverlayId: StateFlow<String?> = _selectedOverlayId.asStateFlow()

    private val _activeTool = MutableStateFlow(EditorTool.STYLES)
    val activeTool: StateFlow<EditorTool> = _activeTool.asStateFlow()

    private val _currentTime = MutableStateFlow(0.0)
    val currentTime: StateFlow<Double> = _currentTime.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _sfxPulse = MutableStateFlow(false)
    val sfxPulse: StateFlow<Boolean> = _sfxPulse.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _busyMessage = MutableStateFlow<String?>(null)
    val busyMessage: StateFlow<String?> = _busyMessage.asStateFlow()

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var lastTriggeredCueId: String? = null

    init {
        refreshHome()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun refreshHome() {
        viewModelScope.launch {
            _isLoadingProjects.value = true
            val (list, online) = repository.syncProjectsList()
            _projects.value = list
            _isBackendOnline.value = online
            if (online) {
                repository.checkBackendConfig().onSuccess { cfg ->
                    _backendConfig.value = cfg
                }
            } else {
                _backendConfig.value = null
            }
            _isLoadingProjects.value = false
        }
    }

    fun updateServerUrl(newUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.setBackendUrl(newUrl)
            _backendUrl.value = repository.getBackendUrl()
            val startMs = System.currentTimeMillis()
            val res = repository.checkBackendConfig()
            val elapsed = System.currentTimeMillis() - startMs
            res.fold(
                onSuccess = { cfg ->
                    _isBackendOnline.value = true
                    _backendConfig.value = cfg
                    refreshHome()
                    onResult(
                        true,
                        "Connected (${elapsed}ms) • ASR: ${cfg.transcription} • Render: ${if (cfg.render) "FFmpeg Ready" else "No FFmpeg"}"
                    )
                },
                onFailure = { err ->
                    _isBackendOnline.value = false
                    _backendConfig.value = null
                    onResult(
                        false,
                        "Backend unreachable at ${_backendUrl.value} (${err.localizedMessage ?: "Timeout"}). App runs in local offline-capable mode."
                    )
                }
            )
        }
    }

    fun openProject(project: Project) {
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoFlags()
        _activeProject.value = project
        _activeTrackIndex.value = 0
        _selectedCueId.value = null
        _selectedOverlayId.value = null
        _currentTime.value = project.trim.start
        _isPlaying.value = false
        repository.saveLocalProject(project)
    }

    fun openDemoShowcase(which: Int, onOpened: () -> Unit) {
        val sample = if (which == 2) {
            SampleProjectsFactory.createVideo2VfxShowcaseProject()
        } else {
            SampleProjectsFactory.createVideo1ShowcaseProject()
        }
        val saved = repository.saveLocalProject(sample)
        openProject(saved)
        refreshHome()
        onOpened()
    }

    fun probeSelectedVideo(uri: Uri, onProbed: (ProbedVideoInfo) -> Unit) {
        viewModelScope.launch {
            _busyMessage.value = "Inspecting video stream..."
            val result = repository.probeAndCopyVideoUri(uri)
            _busyMessage.value = null
            result.fold(
                onSuccess = onProbed,
                onFailure = { showToast("Could not read video: ${it.localizedMessage}") }
            )
        }
    }

    fun createProjectFromProbedVideo(
        probed: ProbedVideoInfo,
        trimStart: Double,
        trimEnd: Double,
        onCreated: () -> Unit
    ) {
        viewModelScope.launch {
            _busyMessage.value = "Creating project & preparing timeline..."
            val created = repository.createProjectFromVideo(probed, trimStart, trimEnd)
            _busyMessage.value = null
            openProject(created)
            refreshHome()
            onCreated()
        }
    }

    fun renameProject(project: Project, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = project.copy(name = newName.trim())
            repository.saveProjectEverywhere(updated)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = updated
            }
            refreshHome()
            showToast("Project renamed to \"${newName.trim()}\"")
        }
    }

    fun duplicateProject(project: Project) {
        viewModelScope.launch {
            val copy = project.copy(
                id = UUID.randomUUID().toString().replace("-", ""),
                name = "${project.name} (Copy)",
                isSyncedWithBackend = false
            )
            repository.saveLocalProject(copy)
            refreshHome()
            showToast("Project duplicated")
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
            }
            refreshHome()
            showToast("Deleted \"${project.name}\"")
        }
    }

    private var backgroundSyncJob: kotlinx.coroutines.Job? = null

    private fun pushUndo(current: Project) {
        undoStack.addLast(current)
        if (undoStack.size > 30) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        updateUndoRedoFlags()
    }

    private fun updateUndoRedoFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun persistAndSync(project: Project) {
        val savedLocal = repository.saveLocalProject(project)
        _activeProject.value = savedLocal
        backgroundSyncJob?.cancel()
        backgroundSyncJob = viewModelScope.launch {
            repository.syncProjectToBackendOnly(savedLocal)
        }
    }

    private fun commitProjectChange(newProject: Project) {
        val current = _activeProject.value
        if (current != null) {
            pushUndo(current)
        }
        persistAndSync(newProject)
    }

    fun undo() {
        val current = _activeProject.value ?: return
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        updateUndoRedoFlags()
        persistAndSync(prev)
    }

    fun redo() {
        val current = _activeProject.value ?: return
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        updateUndoRedoFlags()
        persistAndSync(next)
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
        if (!playing) {
            _sfxPulse.value = false
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun updatePlaybackTime(seconds: Double, fromUserSeek: Boolean = false) {
        val pr = _activeProject.value ?: return
        val clamped = seconds.coerceIn(pr.trim.start, pr.trim.end)
        _currentTime.value = clamped

        if (fromUserSeek) {
            lastTriggeredCueId = null
            return
        }

        // Check SFX trigger on active caption cue entrance
        val sfxCat = pr.effects.sfx
        if (sfxCat != "None" && _isPlaying.value) {
            val activeTrack = pr.tracks.getOrNull(_activeTrackIndex.value) ?: pr.tracks.firstOrNull()
            val hitCue = activeTrack?.captions?.firstOrNull { cue ->
                abs(clamped - cue.start) < 0.06
            }
            if (hitCue != null && hitCue.id != lastTriggeredCueId) {
                lastTriggeredCueId = hitCue.id
                _sfxPulse.value = true
                if (!_isMuted.value) {
                    sfxPlayer.playCategory(sfxCat, pr.effects.sfxVolume)
                }
            } else if (hitCue == null) {
                _sfxPulse.value = false
            }
        } else {
            _sfxPulse.value = false
        }
    }

    fun selectTool(tool: EditorTool) {
        _activeTool.value = tool
    }

    fun selectTrack(index: Int) {
        val pr = _activeProject.value ?: return
        if (index in pr.tracks.indices) {
            _activeTrackIndex.value = index
            _selectedCueId.value = null
        }
    }

    fun selectCue(cueId: String?) {
        _selectedCueId.value = cueId
    }

    fun selectOverlay(overlayId: String?) {
        _selectedOverlayId.value = overlayId
    }

    fun updateAspectRatio(aspectRatio: String) {
        val pr = _activeProject.value ?: return
        commitProjectChange(pr.copy(aspectRatio = aspectRatio))
    }

    fun updateResolution(resolution: Int) {
        val pr = _activeProject.value ?: return
        commitProjectChange(pr.copy(resolution = resolution))
    }

    fun updateActiveTrackStyle(transform: (SubtitleStyle) -> SubtitleStyle) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        if (pr.tracks.isEmpty()) return
        val currentTrack = pr.tracks[idx]
        if (currentTrack.locked) {
            showToast("Track \"${currentTrack.name}\" is locked")
            return
        }
        val newStyle = transform(currentTrack.style)
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = currentTrack.copy(style = newStyle)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
    }

    fun updateVideoEffects(transform: (VideoEffects) -> VideoEffects) {
        val pr = _activeProject.value ?: return
        val newEffects = transform(pr.effects)
        commitProjectChange(pr.copy(effects = newEffects))
    }

    fun auditionAndSelectSfx(category: String) {
        updateVideoEffects { it.copy(sfx = category) }
        if (category != "None") {
            val vol = _activeProject.value?.effects?.sfxVolume ?: 65
            sfxPlayer.playCategory(category, vol)
        }
    }

    fun addSfxClipAtPlayhead(category: String) {
        val pr = _activeProject.value ?: return
        val cat = if (category == "None") "Pop" else category
        val clip = AudioClip(
            id = "aud_${UUID.randomUUID().toString().take(6)}",
            name = "$cat Hit",
            category = cat,
            source = "sfx/${cat.lowercase().replace(" ", "_")}.wav",
            start = _currentTime.value,
            duration = 0.8,
            volume = 1.0,
            ducking = true
        )
        sfxPlayer.playCategory(cat, pr.effects.sfxVolume)
        commitProjectChange(pr.copy(audioClips = pr.audioClips + clip))
        showToast("Added $cat audio clip at ${String.format("%.1f", _currentTime.value)}s")
    }

    fun deleteAudioClip(clipId: String) {
        val pr = _activeProject.value ?: return
        commitProjectChange(pr.copy(audioClips = pr.audioClips.filterNot { it.id == clipId }))
    }

    fun addCaptionAtPlayhead(text: String = "New subtitle cue") {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx) ?: return
        if (track.locked) {
            showToast("Track is locked")
            return
        }
        val start = _currentTime.value.coerceIn(0.0, max(0.0, pr.duration - 0.3))
        val end = (start + 1.6).coerceAtMost(pr.duration)
        val newCue = CaptionCue(
            id = "cue_${System.currentTimeMillis() % 100000}",
            start = (start * 100.0).roundToInt() / 100.0,
            end = (end * 100.0).roundToInt() / 100.0,
            text = text,
            words = SrtParser.buildWordTimings(text, start, end)
        )
        val updatedCues = (track.captions + newCue).sortedBy { it.start }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = track.copy(captions = updatedCues)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
        _selectedCueId.value = newCue.id
    }

    fun updateCue(cueId: String, newText: String, newStart: Double, newEnd: Double) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx) ?: return
        if (track.locked) {
            showToast("Track is locked")
            return
        }
        val validStart = newStart.coerceIn(0.0, pr.duration)
        val validEnd = newEnd.coerceIn(validStart + 0.1, pr.duration)
        val updatedCues = track.captions.map { c ->
            if (c.id == cueId) {
                c.copy(
                    text = newText.trim(),
                    start = validStart,
                    end = validEnd,
                    words = SrtParser.buildWordTimings(newText.trim(), validStart, validEnd)
                )
            } else c
        }.sortedBy { it.start }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = track.copy(captions = updatedCues)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
    }

    fun splitSelectedCueAtPlayhead() {
        val pr = _activeProject.value ?: return
        val cueId = _selectedCueId.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx) ?: return
        val cue = track.captions.firstOrNull { it.id == cueId } ?: return
        val splitTime = if (_currentTime.value > cue.start + 0.1 && _currentTime.value < cue.end - 0.1) {
            _currentTime.value
        } else {
            (cue.start + cue.end) / 2.0
        }

        val words = cue.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val mid = max(1, words.size / 2)
        val text1 = words.take(mid).joinToString(" ").ifEmpty { cue.text }
        val text2 = words.drop(mid).joinToString(" ").ifEmpty { cue.text }

        val cue1 = cue.copy(
            id = "${cue.id}_a",
            end = (splitTime * 100.0).roundToInt() / 100.0,
            text = text1,
            words = SrtParser.buildWordTimings(text1, cue.start, splitTime)
        )
        val cue2 = cue.copy(
            id = "${cue.id}_b",
            start = (splitTime * 100.0).roundToInt() / 100.0,
            text = text2,
            words = SrtParser.buildWordTimings(text2, splitTime, cue.end)
        )

        val updatedCues = track.captions.flatMap {
            if (it.id == cue.id) listOf(cue1, cue2) else listOf(it)
        }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = track.copy(captions = updatedCues)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
        _selectedCueId.value = cue1.id
        showToast("Cue split at ${String.format("%.2f", splitTime)}s")
    }

    fun mergeSelectedCueWithNext() {
        val pr = _activeProject.value ?: return
        val cueId = _selectedCueId.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx) ?: return
        val cuePos = track.captions.indexOfFirst { it.id == cueId }
        if (cuePos < 0 || cuePos >= track.captions.lastIndex) {
            showToast("No subsequent cue on this track to merge with")
            return
        }
        val first = track.captions[cuePos]
        val second = track.captions[cuePos + 1]
        val mergedText = "${first.text} ${second.text}".trim()
        val merged = first.copy(
            end = second.end,
            text = mergedText,
            words = SrtParser.buildWordTimings(mergedText, first.start, second.end)
        )
        val updatedCues = track.captions.toMutableList().apply {
            this[cuePos] = merged
            removeAt(cuePos + 1)
        }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = track.copy(captions = updatedCues)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
        _selectedCueId.value = merged.id
        showToast("Merged adjacent cues")
    }

    fun deleteSelectedCue() {
        val pr = _activeProject.value ?: return
        val cueId = _selectedCueId.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx) ?: return
        val updatedCues = track.captions.filterNot { it.id == cueId }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = track.copy(captions = updatedCues)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
        _selectedCueId.value = null
    }

    // Multi-track management
    fun addTrack(name: String, language: String) {
        val pr = _activeProject.value ?: return
        if (pr.tracks.size >= 5) {
            showToast("Maximum 5 subtitle tracks supported")
            return
        }
        val palette = listOf("#8B5CF6", "#06B6D4", "#F59E0B", "#10B981", "#EC4899")
        val newTrack = Track(
            id = "tr_${UUID.randomUUID().toString().take(6)}",
            name = name.ifBlank { "Track ${pr.tracks.size + 1}" },
            language = language,
            color = palette[pr.tracks.size % palette.size],
            style = SubtitleStyle(yPos = (0.70 + pr.tracks.size * 0.06).coerceAtMost(0.90))
        )
        val updated = pr.copy(tracks = pr.tracks + newTrack)
        commitProjectChange(updated)
        _activeTrackIndex.value = updated.tracks.lastIndex
    }

    fun toggleTrackVisibility(index: Int) {
        val pr = _activeProject.value ?: return
        if (index !in pr.tracks.indices) return
        val updated = pr.tracks.toMutableList().apply {
            this[index] = this[index].copy(visible = !this[index].visible)
        }
        commitProjectChange(pr.copy(tracks = updated))
    }

    fun toggleTrackLock(index: Int) {
        val pr = _activeProject.value ?: return
        if (index !in pr.tracks.indices) return
        val updated = pr.tracks.toMutableList().apply {
            this[index] = this[index].copy(locked = !this[index].locked)
        }
        commitProjectChange(pr.copy(tracks = updated))
    }

    fun deleteTrack(index: Int) {
        val pr = _activeProject.value ?: return
        if (pr.tracks.size <= 1 || index !in pr.tracks.indices) {
            showToast("At least one subtitle track is required")
            return
        }
        val updated = pr.tracks.toMutableList().apply { removeAt(index) }
        commitProjectChange(pr.copy(tracks = updated))
        _activeTrackIndex.value = _activeTrackIndex.value.coerceAtMost(updated.lastIndex)
    }

    // Video Trimming & Clip Splitting
    fun updateVideoTrim(newStart: Double, newEnd: Double) {
        val pr = _activeProject.value ?: return
        val s = newStart.coerceIn(0.0, max(0.0, pr.duration - 0.2))
        val e = newEnd.coerceIn(s + 0.2, pr.duration)
        commitProjectChange(pr.copy(trim = VideoTrim(s, e)))
        if (_currentTime.value < s || _currentTime.value > e) {
            _currentTime.value = s
        }
    }

    fun splitVideoClipAtPlayhead() {
        val pr = _activeProject.value ?: return
        val t = _currentTime.value
        val existingClips = pr.videoClips.ifEmpty {
            listOf(
                VideoClipSegment(
                    id = "vc_base",
                    label = "Main Clip",
                    sourceUri = pr.source,
                    startInTimeline = pr.trim.start,
                    endInTimeline = pr.trim.end,
                    sourceTrimStart = pr.trim.start,
                    transition = "Cut"
                )
            )
        }
        val targetIdx = existingClips.indexOfFirst { t > it.startInTimeline + 0.15 && t < it.endInTimeline - 0.15 }
        if (targetIdx < 0) {
            showToast("Move playhead inside a video clip segment to split")
            return
        }
        val target = existingClips[targetIdx]
        val splitPoint = (t * 100.0).roundToInt() / 100.0
        val clipA = target.copy(
            id = "${target.id}_1",
            label = "${target.label} A",
            endInTimeline = splitPoint
        )
        val clipB = target.copy(
            id = "${target.id}_2",
            label = "${target.label} B",
            startInTimeline = splitPoint,
            sourceTrimStart = target.sourceTrimStart + (splitPoint - target.startInTimeline),
            transition = "Purple Flash"
        )
        val updatedClips = existingClips.toMutableList().apply {
            this[targetIdx] = clipA
            add(targetIdx + 1, clipB)
        }
        commitProjectChange(pr.copy(videoClips = updatedClips))
        showToast("Video clip split at ${String.format("%.2f", splitPoint)}s")
    }

    fun cycleClipTransition(clipId: String) {
        val pr = _activeProject.value ?: return
        val transitions = listOf("Cut", "Fade", "Whip", "Purple Flash")
        val updated = pr.videoClips.map { clip ->
            if (clip.id == clipId) {
                val next = transitions[(transitions.indexOf(clip.transition) + 1) % transitions.size]
                clip.copy(transition = next)
            } else clip
        }
        commitProjectChange(pr.copy(videoClips = updated))
    }

    // Templates & Graphic Overlays
    fun applyCinematicTemplate(templateId: String) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val curTrack = pr.tracks[idx]
        var newStyle = curTrack.style
        var newEffects = pr.effects
        val newOverlays = pr.overlays.toMutableList()
        val nowStart = _currentTime.value
        val nowEnd = (nowStart + 2.6).coerceAtMost(pr.duration)
        val ovId = "ov_${System.currentTimeMillis() % 100000}"

        when (templateId) {
            "v1_orange_headline" -> {
                newStyle = curTrack.style.copy(
                    preset = "Orange Headline",
                    font = "Vazirmatn",
                    fontSize = 30.0,
                    textColor = "#FF7A00",
                    bgColor = "#110D1A",
                    bgEnabled = true,
                    outline = 3.5,
                    animation = "Pop",
                    yPos = 0.72,
                    glow = 5.0,
                    glowColor = "#FF7A00"
                )
                newEffects = newEffects.copy(sfx = "Pop", sfxMode = "Sentence", sfxVolume = 65)
                newOverlays.add(
                    GraphicOverlay(
                        id = ovId,
                        name = "Persian Orange Hero",
                        type = "persian_orange_headline",
                        content = "فنلاند",
                        color = "#FF7A00",
                        start = nowStart,
                        end = nowEnd,
                        xPos = 0.50,
                        yPos = 0.28,
                        scale = 1.05,
                        animation = "Pop"
                    )
                )
            }
            "v1_orange_white_explainer" -> {
                newStyle = curTrack.style.copy(
                    preset = "Orange/White Explainer",
                    font = "Noto Sans Arabic",
                    fontSize = 25.0,
                    textColor = "#FFFFFF",
                    bgColor = "#1E1B2E",
                    bgEnabled = true,
                    outline = 2.0,
                    animation = "Fade",
                    yPos = 0.78
                )
                newEffects = newEffects.copy(sfx = "Click", sfxMode = "Sentence", sfxVolume = 50)
            }
            "v1_purple_flare" -> {
                newEffects = newEffects.copy(sfx = "Whoosh", sfxVolume = 60)
                newOverlays.add(
                    GraphicOverlay(
                        id = ovId,
                        name = "Purple Light Flare",
                        type = "light_leak",
                        content = "✦ PURPLE FLARE ✦",
                        color = "#D946EF",
                        start = nowStart,
                        end = (nowStart + 1.4).coerceAtMost(pr.duration),
                        animation = "Fade",
                        opacity = 0.85
                    )
                )
            }
            "v2_hand_hologram" -> {
                newEffects = newEffects.copy(sfx = "Shimmer", sfxVolume = 60)
                newOverlays.add(
                    GraphicOverlay(
                        id = ovId,
                        name = "Hand Glow Hologram",
                        type = "glow_hologram",
                        content = "◈ HOLOGRAM ◈",
                        color = "#38BDF8",
                        start = nowStart,
                        end = nowEnd,
                        xPos = 0.62,
                        yPos = 0.40,
                        scale = 1.15,
                        animation = "Pulse",
                        glow = 8.0
                    )
                )
            }
            "v2_soft_bg_words" -> {
                newEffects = newEffects.copy(sfx = "Ambient", sfxVolume = 45)
                newOverlays.add(
                    GraphicOverlay(
                        id = ovId,
                        name = "Soft Persian Words (Behind Subject)",
                        type = "soft_bg_text",
                        content = "کپشن فورج شناور",
                        color = "#94A3B8",
                        start = nowStart,
                        end = nowEnd,
                        xPos = 0.45,
                        yPos = 0.30,
                        scale = 1.0,
                        opacity = 0.45,
                        blur = 1.5,
                        isBackground = true,
                        depthLayer = 0,
                        animation = "Fade"
                    )
                )
            }
            "v2_flag_subject" -> {
                newEffects = newEffects.copy(sfx = "Riser", sfxVolume = 55)
                newOverlays.add(
                    GraphicOverlay(
                        id = ovId,
                        name = "Waving Flag Overlay",
                        type = "flag_graphic",
                        content = "⚑ MALAYSIA FLAG ⚑",
                        color = "#F59E0B",
                        start = nowStart,
                        end = nowEnd,
                        xPos = 0.35,
                        yPos = 0.32,
                        scale = 1.1,
                        isBackground = true,
                        depthLayer = 0,
                        animation = "Fade"
                    )
                )
            }
            "v2_metallic_australia" -> {
                newEffects = newEffects.copy(sfx = "Glitch", sfxVolume = 65)
                newOverlays.add(
                    GraphicOverlay(
                        id = ovId,
                        name = "Metallic Australia Reveal",
                        type = "metallic_banner",
                        content = "AUSTRALIA",
                        color = "#E2E8F0",
                        start = nowStart,
                        end = nowEnd,
                        xPos = 0.50,
                        yPos = 0.54,
                        scale = 1.0,
                        animation = "Pop"
                    )
                )
            }
            "v2_end_accent" -> {
                newEffects = newEffects.copy(sfx = "Click", sfxVolume = 45)
                newOverlays.add(
                    GraphicOverlay(
                        id = ovId,
                        name = "End Studio Accent",
                        type = "end_accent",
                        content = "CaptionForge Studio",
                        color = "#EF4444",
                        start = nowStart,
                        end = nowEnd,
                        xPos = 0.50,
                        yPos = 0.88,
                        scale = 1.0,
                        animation = "Slide down"
                    )
                )
            }
        }

        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = curTrack.copy(style = newStyle)
        }
        sfxPlayer.playCategory(newEffects.sfx, newEffects.sfxVolume)
        commitProjectChange(
            pr.copy(
                tracks = updatedTracks,
                effects = newEffects,
                overlays = newOverlays
            )
        )
        showToast("Applied template: $templateId")
    }

    fun addGraphicOverlay(type: String, name: String, content: String, color: String) {
        val pr = _activeProject.value ?: return
        val start = _currentTime.value
        val end = (start + 2.8).coerceAtMost(pr.duration)
        val isBg = type == "soft_bg_text" || type == "flag_graphic"
        val newOv = GraphicOverlay(
            id = "ov_${System.currentTimeMillis() % 100000}",
            name = name,
            type = type,
            content = content,
            color = color,
            start = start,
            end = end,
            xPos = 0.50,
            yPos = if (type == "end_accent") 0.88 else if (isBg) 0.30 else 0.48,
            isBackground = isBg,
            depthLayer = if (isBg) 0 else 2
        )
        commitProjectChange(pr.copy(overlays = pr.overlays + newOv))
        _selectedOverlayId.value = newOv.id
        showToast("Added layer \"$name\"")
    }

    fun toggleOverlayVisibility(overlayId: String) {
        val pr = _activeProject.value ?: return
        val updated = pr.overlays.map {
            if (it.id == overlayId) it.copy(visible = !it.visible) else it
        }
        commitProjectChange(pr.copy(overlays = updated))
    }

    fun updateOverlay(updatedOverlay: GraphicOverlay) {
        val pr = _activeProject.value ?: return
        val updated = pr.overlays.map {
            if (it.id == updatedOverlay.id) updatedOverlay else it
        }
        commitProjectChange(pr.copy(overlays = updated))
    }

    fun deleteOverlay(overlayId: String) {
        val pr = _activeProject.value ?: return
        commitProjectChange(pr.copy(overlays = pr.overlays.filterNot { it.id == overlayId }))
        if (_selectedOverlayId.value == overlayId) {
            _selectedOverlayId.value = null
        }
    }

    fun trackMotionForOverlay(overlay: GraphicOverlay, trackType: String) {
        val pr = _activeProject.value ?: return
        viewModelScope.launch {
            _busyMessage.value = "Running $trackType motion tracker..."
            val res = repository.runMotionTracking(pr, trackType, overlay.start, overlay.end)
            _busyMessage.value = null
            res.fold(
                onSuccess = { points ->
                    val kfs = points.map { pt ->
                        OverlayKeyframe(
                            time = (pt.time - overlay.start).coerceAtLeast(0.0),
                            xPos = pt.x,
                            yPos = pt.y,
                            scale = pt.scale,
                            easing = "easeInOut",
                            color = overlay.color
                        )
                    }
                    updateOverlay(overlay.copy(keyframes = kfs))
                    showToast("Applied ${kfs.size} tracked keyframes from backend")
                },
                onFailure = {
                    // Generate smooth procedural motion path keyframes clearly labeled
                    val dur = overlay.duration
                    val fallbackKfs = listOf(
                        OverlayKeyframe(time = 0.0, xPos = 0.42, yPos = 0.44, scale = 0.95, easing = "easeInOut", color = overlay.color),
                        OverlayKeyframe(time = dur * 0.5, xPos = 0.58, yPos = 0.36, scale = 1.12, easing = "easeInOut", color = overlay.color),
                        OverlayKeyframe(time = dur, xPos = 0.50, yPos = 0.40, scale = 1.0, easing = "easeOut", color = overlay.color)
                    )
                    updateOverlay(overlay.copy(keyframes = fallbackKfs))
                    showToast("Backend offline: Generated 3 editable motion keyframes locally")
                }
            )
        }
    }

    // AI Speech Transcription
    fun triggerAiTranscription(language: String?, model: String) {
        val pr = _activeProject.value ?: return
        viewModelScope.launch {
            _busyMessage.value = "Connecting to Faster-Whisper ASR engine..."
            val res = repository.runBackendTranscription(pr, language, model) { msg ->
                _busyMessage.value = msg
            }
            _busyMessage.value = null
            res.fold(
                onSuccess = { refreshed ->
                    commitProjectChange(refreshed)
                    showToast("AI Transcription complete (${refreshed.tracks.firstOrNull()?.captions?.size ?: 0} cues)")
                },
                onFailure = { err ->
                    showToast("Backend ASR unavailable (${err.localizedMessage ?: "Offline"}). Configure FastAPI server URL or import SRT.")
                }
            )
        }
    }

    // AI Subtitle Translation
    fun triggerAiTranslation(targetLang: String, mode: String, provider: String) {
        val pr = _activeProject.value ?: return
        val curTrack = pr.tracks.getOrNull(_activeTrackIndex.value) ?: pr.tracks.firstOrNull() ?: return
        if (curTrack.captions.isEmpty()) {
            showToast("Add or generate captions on the current track first")
            return
        }
        viewModelScope.launch {
            _busyMessage.value = "Translating track to ${targetLang.uppercase()}..."
            val res = repository.runBackendTranslation(
                project = pr,
                trackId = curTrack.id,
                targetLang = targetLang,
                mode = mode,
                provider = provider
            ) { msg ->
                _busyMessage.value = msg
            }
            _busyMessage.value = null
            res.fold(
                onSuccess = { refreshed ->
                    commitProjectChange(refreshed)
                    _activeTrackIndex.value = refreshed.tracks.lastIndex
                    showToast("Translated to ${targetLang.uppercase()} via FastAPI backend")
                },
                onFailure = {
                    // Use clearly labeled offline translation fallback so bilingual RTL layout is immediately testable
                    val offlineUpdated = repository.buildOfflineFallbackTranslation(pr, curTrack, targetLang, mode)
                    commitProjectChange(offlineUpdated)
                    _activeTrackIndex.value = offlineUpdated.tracks.lastIndex
                    showToast("Backend offline: Added ${targetLang.uppercase()} offline preview track")
                }
            )
        }
    }

    // SRT Import & Export
    fun importSrtFromUri(uri: Uri) {
        val pr = _activeProject.value ?: return
        viewModelScope.launch {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Cannot read subtitle file")
                val cues = SrtParser.parseSrt(content)
                if (cues.isEmpty()) {
                    showToast("No valid SRT/VTT subtitle cues found in file")
                    return@launch
                }
                val newTrack = Track(
                    id = "tr_srt_${System.currentTimeMillis() % 10000}",
                    name = "Imported SRT (${cues.size})",
                    language = "auto",
                    color = "#10B981",
                    captions = cues
                )
                val updated = pr.copy(tracks = (pr.tracks + newTrack).take(5))
                commitProjectChange(updated)
                _activeTrackIndex.value = updated.tracks.lastIndex
                showToast("Imported ${cues.size} subtitle cues")
            } catch (e: Exception) {
                showToast("SRT import failed: ${e.localizedMessage}")
            }
        }
    }

    fun importCustomFontFromUri(uri: Uri) {
        val pr = _activeProject.value ?: return
        viewModelScope.launch {
            val res = repository.uploadCustomFontFile(uri)
            res.fold(
                onSuccess = { fontName ->
                    val updatedFonts = (pr.customFonts + fontName).distinct()
                    commitProjectChange(pr.copy(customFonts = updatedFonts))
                    updateActiveTrackStyle { it.copy(font = fontName) }
                    showToast("Registered custom font: $fontName")
                },
                onFailure = {
                    showToast("Font import error: ${it.localizedMessage}")
                }
            )
        }
    }

    fun resetExportState() {
        _exportState.value = ExportUiState()
    }

    fun exportSrtSidecar() {
        val pr = _activeProject.value ?: return
        val track = pr.tracks.getOrNull(_activeTrackIndex.value) ?: pr.tracks.firstOrNull() ?: return
        viewModelScope.launch {
            try {
                val srtText = SrtParser.exportSrt(track.captions)
                val outDir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
                val outFile = File(outDir, "${pr.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}_${track.language}.srt")
                outFile.writeText(srtText, Charsets.UTF_8)
                _exportState.value = _exportState.value.copy(
                    exportedSrtFile = outFile,
                    progressMessage = "Saved ${track.captions.size} cues to ${outFile.name}"
                )
                showToast("SRT exported (${track.captions.size} cues)")
            } catch (e: Exception) {
                showToast("SRT export error: ${e.localizedMessage}")
            }
        }
    }

    fun startVideoExport(resolution: Int) {
        val pr = _activeProject.value ?: return
        viewModelScope.launch {
            _exportState.value = ExportUiState(
                isExporting = true,
                progressMessage = if (_isBackendOnline.value) {
                    "Connecting to FastAPI FFmpeg render service..."
                } else {
                    "Starting on-device Media3 Transformer MP4 encoder..."
                }
            )
            val updatedPr = pr.copy(resolution = resolution)
            _activeProject.value = updatedPr

            val srtText = SrtParser.exportSrt(
                (updatedPr.tracks.getOrNull(_activeTrackIndex.value) ?: updatedPr.tracks.first()).captions
            )
            val outDir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
            val srtFile = File(outDir, "${updatedPr.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.srt")
            runCatching { srtFile.writeText(srtText, Charsets.UTF_8) }

            if (_isBackendOnline.value) {
                val backendRes = repository.runBackendExport(
                    project = updatedPr,
                    resolution = resolution,
                    aspectRatio = updatedPr.aspectRatio
                ) { msg ->
                    _exportState.value = _exportState.value.copy(progressMessage = msg)
                }

                if (backendRes.isSuccess) {
                    _exportState.value = ExportUiState(
                        isExporting = false,
                        progressMessage = "FFmpeg MP4 render complete (${resolution}p ${updatedPr.aspectRatio})",
                        downloadUrl = backendRes.getOrNull(),
                        exportedSrtFile = srtFile
                    )
                    showToast("Backend FFmpeg MP4 render complete!")
                    return@launch
                }
            }

            // On-Device Hardware-Accelerated Media3 Transformer MP4 Render
            val localRenderRes = onDeviceMp4Renderer.renderProjectToMp4(
                project = updatedPr,
                resolution = resolution
            ) { msg ->
                _exportState.value = _exportState.value.copy(progressMessage = msg)
            }

            localRenderRes.fold(
                onSuccess = { mp4File ->
                    _exportState.value = ExportUiState(
                        isExporting = false,
                        progressMessage = "On-Device Media3 MP4 render complete (${mp4File.length() / 1024} KB)",
                        exportedMp4File = mp4File,
                        exportedSrtFile = srtFile
                    )
                    showToast("Rendered ${mp4File.name} on-device!")
                },
                onFailure = { err ->
                    _exportState.value = ExportUiState(
                        isExporting = false,
                        exportedSrtFile = srtFile,
                        error = "On-device hardware codec unavailable (${err.localizedMessage ?: "Codec error"}) and FastAPI FFmpeg server is offline at ${_backendUrl.value}. " +
                            "Connect your Python FastAPI server for server-side FFmpeg + libass rendering, or run on an Android device with H.264 MediaCodec. " +
                            "Your SRT subtitle file (${srtFile.name}) is saved locally."
                    )
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sfxPlayer.release()
    }
}
