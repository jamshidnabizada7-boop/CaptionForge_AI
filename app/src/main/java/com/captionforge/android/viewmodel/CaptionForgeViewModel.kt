package com.captionforge.android.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.captionforge.android.audio.SfxPlayerManager
import com.captionforge.android.audio.SfxSoundPreset
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
import com.captionforge.android.model.StylePresetCatalog
import com.captionforge.android.model.StylePresetItem
import com.captionforge.android.model.SubtitleStyle
import com.captionforge.android.model.Track
import com.captionforge.android.model.VideoClipSegment
import com.captionforge.android.model.VideoEffects
import com.captionforge.android.model.VideoTrim
import com.captionforge.android.model.WordTiming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
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

    private val _selectedWordIndex = MutableStateFlow(0)
    val selectedWordIndex: StateFlow<Int> = _selectedWordIndex.asStateFlow()

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
    private var lastTriggeredWordKey: String? = null

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
            lastTriggeredWordKey = null
            return
        }

        if (!_isPlaying.value) {
            _sfxPulse.value = false
            return
        }

        val activeTrack = pr.tracks.getOrNull(_activeTrackIndex.value) ?: pr.tracks.firstOrNull()
        val hitCue = activeTrack?.captions?.firstOrNull { cue ->
            clamped >= cue.start && clamped <= cue.end
        }

        if (hitCue != null) {
            // 1. Check word-synchronized SFX first
            val hitWord = hitCue.words.firstOrNull { w ->
                abs(clamped - w.start) < 0.06 && w.sfx != "None" && w.sfx.isNotBlank()
            }
            if (hitWord != null) {
                val wordKey = "${hitCue.id}_${hitWord.start}"
                if (wordKey != lastTriggeredWordKey) {
                    lastTriggeredWordKey = wordKey
                    _sfxPulse.value = true
                    if (!_isMuted.value) {
                        sfxPlayer.playPreset(hitWord.sfx, pr.effects.sfxVolume)
                    }
                }
            }

            // 2. Check cue entrance SFX or global sentence SFX
            if (abs(clamped - hitCue.start) < 0.06 && hitCue.id != lastTriggeredCueId) {
                lastTriggeredCueId = hitCue.id
                val cueSfx = if (hitCue.entranceSfx != "None" && hitCue.entranceSfx.isNotBlank()) {
                    hitCue.entranceSfx
                } else {
                    pr.effects.sfx
                }
                if (cueSfx != "None") {
                    _sfxPulse.value = true
                    if (!_isMuted.value) {
                        sfxPlayer.playPreset(cueSfx, pr.effects.sfxVolume)
                    }
                }
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
        _selectedWordIndex.value = 0
    }

    fun selectWordIndex(index: Int) {
        _selectedWordIndex.value = index.coerceAtLeast(0)
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

    /**
     * Applies a StylePresetItem either to the entire active track OR to only the currently selected cue.
     */
    fun applyStylePreset(preset: StylePresetItem, applyToEntireTrack: Boolean = true) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        if (pr.tracks.isEmpty()) return
        val currentTrack = pr.tracks[idx]
        if (currentTrack.locked) {
            showToast("Track \"${currentTrack.name}\" is locked")
            return
        }
        val selCueId = _selectedCueId.value
        if (!applyToEntireTrack && selCueId != null) {
            val updatedCues = currentTrack.captions.map { c ->
                if (c.id == selCueId) c.copy(customStyle = preset.style) else c
            }
            val updatedTracks = pr.tracks.toMutableList().apply {
                this[idx] = currentTrack.copy(captions = updatedCues)
            }
            commitProjectChange(pr.copy(tracks = updatedTracks))
            showToast("Applied \"${preset.name}\" to selected caption")
        } else {
            // Clear per-cue overrides when applying to the entire track so all cues adopt the preset
            val updatedCues = currentTrack.captions.map { it.copy(customStyle = null) }
            val updatedTracks = pr.tracks.toMutableList().apply {
                this[idx] = currentTrack.copy(style = preset.style, captions = updatedCues)
            }
            val newEffects = if (preset.defaultSfx != "None") {
                pr.effects.copy(sfx = preset.defaultSfx)
            } else pr.effects
            if (preset.defaultSfx != "None") {
                sfxPlayer.playPreset(preset.defaultSfx, newEffects.sfxVolume)
            }
            commitProjectChange(pr.copy(tracks = updatedTracks, effects = newEffects))
            showToast("Applied \"${preset.name}\" to track")
        }
    }

    fun saveCurrentStyleAsCustomPreset(customName: String) {
        val pr = _activeProject.value ?: return
        val clean = customName.trim().ifBlank { "Custom Style ${pr.customStylePresets.size + 1}" }
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val curStyle = pr.tracks.getOrNull(idx)?.style ?: SubtitleStyle()
        val namedStyle = curStyle.copy(preset = clean)
        val updatedPresets = (pr.customStylePresets.filterNot { it.preset.equals(clean, ignoreCase = true) } + namedStyle)
        commitProjectChange(pr.copy(customStylePresets = updatedPresets))
        showToast("Saved style preset \"$clean\"")
    }

    fun deleteCustomStylePreset(presetName: String) {
        val pr = _activeProject.value ?: return
        val updated = pr.customStylePresets.filterNot { it.preset.equals(presetName, ignoreCase = true) }
        commitProjectChange(pr.copy(customStylePresets = updated))
        showToast("Removed preset \"$presetName\"")
    }

    fun selectFontForActiveTrack(fontName: String, fontWeight: Int? = null) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val currentTrack = pr.tracks.getOrNull(idx) ?: return
        if (currentTrack.locked) {
            showToast("Track is locked")
            return
        }
        val newStyle = currentTrack.style.copy(
            font = fontName,
            fontWeight = fontWeight ?: currentTrack.style.fontWeight
        )
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = currentTrack.copy(style = newStyle)
        }
        val updatedRecents = (listOf(fontName) + pr.recentFonts.filterNot { it.equals(fontName, ignoreCase = true) }).take(8)
        commitProjectChange(pr.copy(tracks = updatedTracks, recentFonts = updatedRecents))
    }

    fun toggleFavoriteFont(fontName: String) {
        val pr = _activeProject.value ?: return
        val exists = pr.favoriteFonts.any { it.equals(fontName, ignoreCase = true) }
        val updated = if (exists) {
            pr.favoriteFonts.filterNot { it.equals(fontName, ignoreCase = true) }
        } else {
            pr.favoriteFonts + fontName
        }
        commitProjectChange(pr.copy(favoriteFonts = updated))
    }

    fun toggleFavoriteStylePreset(presetId: String) {
        val pr = _activeProject.value ?: return
        val exists = pr.favoriteStylePresets.any { it.equals(presetId, ignoreCase = true) }
        val updated = if (exists) {
            pr.favoriteStylePresets.filterNot { it.equals(presetId, ignoreCase = true) }
        } else {
            pr.favoriteStylePresets + presetId
        }
        commitProjectChange(pr.copy(favoriteStylePresets = updated))
    }

    fun toggleFavoriteSfx(presetId: String) {
        val pr = _activeProject.value ?: return
        val exists = pr.favoriteSfx.contains(presetId)
        val updated = if (exists) pr.favoriteSfx - presetId else pr.favoriteSfx + presetId
        commitProjectChange(pr.copy(favoriteSfx = updated))
    }

    fun updateCuePositionFromCanvas(newX: Double, newY: Double) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val currentTrack = pr.tracks.getOrNull(idx) ?: return
        if (currentTrack.locked) return
        val rx = (newX * 100.0).roundToInt() / 100.0
        val ry = (newY * 100.0).roundToInt() / 100.0
        val newStyle = currentTrack.style.copy(xPos = rx, yPos = ry)
        val selId = _selectedCueId.value
        val updatedCaptions = if (selId != null) {
            currentTrack.captions.map { c ->
                if (c.id == selId) {
                    val baseCueStyle = c.customStyle ?: newStyle
                    c.copy(customStyle = baseCueStyle.copy(xPos = rx, yPos = ry))
                } else c
            }
        } else {
            currentTrack.captions
        }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = currentTrack.copy(style = newStyle, captions = updatedCaptions)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
    }

    fun updateOverlayPositionFromCanvas(overlayId: String, newX: Double, newY: Double) {
        val pr = _activeProject.value ?: return
        val updated = pr.overlays.map { ov ->
            if (ov.id == overlayId) {
                ov.copy(
                    xPos = (newX * 100.0).roundToInt() / 100.0,
                    yPos = (newY * 100.0).roundToInt() / 100.0
                )
            } else ov
        }
        commitProjectChange(pr.copy(overlays = updated))
    }

    fun updateVideoEffects(transform: (VideoEffects) -> VideoEffects) {
        val pr = _activeProject.value ?: return
        val newEffects = transform(pr.effects)
        commitProjectChange(pr.copy(effects = newEffects))
    }

    fun auditionAndSelectSfx(categoryOrPreset: String) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx)
        val targetCue = track?.captions?.firstOrNull { it.id == _selectedCueId.value }
            ?: track?.captions?.firstOrNull { _currentTime.value in it.start..it.end }
            ?: track?.captions?.firstOrNull()

        if (pr.effects.sfxMode.equals("Word", ignoreCase = true) && targetCue != null && track != null && !track.locked) {
            val baseWords = if (targetCue.words.isNotEmpty()) {
                targetCue.words
            } else {
                SrtParser.buildWordTimings(targetCue.text, targetCue.start, targetCue.end)
            }
            if (baseWords.isNotEmpty()) {
                val wIdx = _selectedWordIndex.value.coerceIn(0, baseWords.lastIndex)
                val updatedWords = baseWords.toMutableList().apply {
                    this[wIdx] = this[wIdx].copy(sfx = categoryOrPreset)
                }
                val updatedCue = targetCue.copy(words = updatedWords)
                val updatedCues = track.captions.map { if (it.id == updatedCue.id) updatedCue else it }
                val updatedTracks = pr.tracks.toMutableList().apply {
                    this[idx] = track.copy(captions = updatedCues)
                }
                commitProjectChange(pr.copy(tracks = updatedTracks, effects = pr.effects.copy(sfx = categoryOrPreset)))
                if (categoryOrPreset != "None") {
                    sfxPlayer.playPreset(categoryOrPreset, pr.effects.sfxVolume)
                    showToast("Assigned \"$categoryOrPreset\" to word \"${updatedWords[wIdx].text}\"")
                } else {
                    showToast("Cleared SFX on word \"${updatedWords[wIdx].text}\"")
                }
                return
            }
        }

        if (targetCue != null && track != null && !track.locked && _selectedCueId.value != null) {
            val updatedCue = targetCue.copy(entranceSfx = categoryOrPreset)
            val updatedCues = track.captions.map { if (it.id == updatedCue.id) updatedCue else it }
            val updatedTracks = pr.tracks.toMutableList().apply {
                this[idx] = track.copy(captions = updatedCues)
            }
            commitProjectChange(pr.copy(tracks = updatedTracks, effects = pr.effects.copy(sfx = categoryOrPreset)))
        } else {
            updateVideoEffects { it.copy(sfx = categoryOrPreset) }
        }
        if (categoryOrPreset != "None") {
            val vol = _activeProject.value?.effects?.sfxVolume ?: 65
            sfxPlayer.playPreset(categoryOrPreset, vol)
        }
    }

    fun updateWordInCue(cueId: String, wordIndex: Int, transform: (WordTiming) -> WordTiming) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx) ?: return
        if (track.locked) {
            showToast("Track is locked")
            return
        }
        val cue = track.captions.firstOrNull { it.id == cueId } ?: return
        val baseWords = if (cue.words.isNotEmpty()) {
            cue.words
        } else {
            SrtParser.buildWordTimings(cue.text, cue.start, cue.end)
        }
        if (wordIndex !in baseWords.indices) return
        val updatedWords = baseWords.toMutableList().apply {
            this[wordIndex] = transform(this[wordIndex])
        }
        val rebuiltText = updatedWords.joinToString(" ") { it.text }.trim().ifBlank { cue.text }
        val newCueStart = minOf(cue.start, updatedWords.minOf { it.start }).coerceAtLeast(0.0)
        val newCueEnd = maxOf(cue.end, updatedWords.maxOf { it.end }).coerceAtMost(pr.duration)
        val updatedCue = cue.copy(
            text = rebuiltText,
            start = newCueStart,
            end = max(newCueStart + 0.1, newCueEnd),
            words = updatedWords
        )
        val updatedCues = track.captions.map { if (it.id == cueId) updatedCue else it }.sortedBy { it.start }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = track.copy(captions = updatedCues)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
    }

    fun addSfxClipAtPlayhead(categoryOrPreset: String) {
        val pr = _activeProject.value ?: return
        val preset = sfxPlayer.findPreset(if (categoryOrPreset == "None") "Soft Pop" else categoryOrPreset)
        if (preset != null) {
            addSfxPresetClipAtPlayhead(preset)
            return
        }
        val cat = if (categoryOrPreset == "None") "Soft Pop" else categoryOrPreset
        val clip = AudioClip(
            id = "aud_${UUID.randomUUID().toString().take(6)}",
            name = "$cat Hit",
            category = cat,
            source = "sfx/${cat.lowercase().replace(" ", "_")}.wav",
            start = _currentTime.value,
            duration = 0.6,
            volume = 1.0,
            ducking = true
        )
        sfxPlayer.playCategory(cat, pr.effects.sfxVolume)
        commitProjectChange(pr.copy(audioClips = pr.audioClips + clip))
        showToast("Added $cat audio clip at ${String.format(Locale.US, "%.1f", _currentTime.value)}s")
    }

    fun addSfxPresetClipAtPlayhead(preset: SfxSoundPreset) {
        val pr = _activeProject.value ?: return
        val clip = AudioClip(
            id = "aud_${UUID.randomUUID().toString().take(6)}",
            name = preset.name,
            category = preset.category,
            source = "sfx/${preset.filename}",
            start = (_currentTime.value * 100.0).roundToInt() / 100.0,
            duration = preset.durationSec,
            volume = if (preset.isMusicLoop) 0.55 else 0.95,
            fadeIn = if (preset.isMusicLoop) 0.25 else 0.02,
            fadeOut = if (preset.isMusicLoop) 0.35 else 0.08,
            ducking = true,
            loop = preset.isMusicLoop,
            isMusic = preset.isMusicLoop
        )
        sfxPlayer.playPreset(preset.id, pr.effects.sfxVolume)
        commitProjectChange(pr.copy(audioClips = pr.audioClips + clip))
        showToast("Added \"${preset.name}\" at ${String.format(Locale.US, "%.1f", _currentTime.value)}s")
    }

    fun importUserAudioFromUri(uri: Uri) {
        val pr = _activeProject.value ?: return
        viewModelScope.launch {
            val res = repository.importUserAudioFile(uri)
            res.fold(
                onSuccess = { (name, file, durSec) ->
                    val clip = AudioClip(
                        id = "aud_usr_${UUID.randomUUID().toString().take(6)}",
                        name = name,
                        category = "Imported Audio",
                        source = file.absolutePath,
                        start = (_currentTime.value * 100.0).roundToInt() / 100.0,
                        duration = durSec.coerceAtMost(pr.duration),
                        volume = 0.85,
                        fadeIn = 0.10,
                        fadeOut = 0.15,
                        ducking = true,
                        isMusic = durSec > 3.0
                    )
                    sfxPlayer.playFileOrPreset(file.absolutePath, 80)
                    commitProjectChange(pr.copy(audioClips = pr.audioClips + clip))
                    showToast("Imported audio \"$name\" (${String.format(Locale.US, "%.1f", durSec)}s)")
                },
                onFailure = {
                    showToast("Audio import failed: ${it.localizedMessage}")
                }
            )
        }
    }

    fun updateAudioClip(updatedClip: AudioClip) {
        val pr = _activeProject.value ?: return
        val updated = pr.audioClips.map { if (it.id == updatedClip.id) updatedClip else it }
        commitProjectChange(pr.copy(audioClips = updated))
    }

    fun duplicateAudioClip(clipId: String) {
        val pr = _activeProject.value ?: return
        val orig = pr.audioClips.firstOrNull { it.id == clipId } ?: return
        val copy = orig.copy(
            id = "aud_${UUID.randomUUID().toString().take(6)}",
            name = "${orig.name} (2)",
            start = (orig.start + orig.duration + 0.1).coerceAtMost(max(0.0, pr.duration - 0.3))
        )
        commitProjectChange(pr.copy(audioClips = pr.audioClips + copy))
        showToast("Duplicated audio clip \"${orig.name}\"")
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
                    words = SrtParser.buildWordTimings(newText.trim(), validStart, validEnd, c.words)
                )
            } else c
        }.sortedBy { it.start }
        val updatedTracks = pr.tracks.toMutableList().apply {
            this[idx] = track.copy(captions = updatedCues)
        }
        commitProjectChange(pr.copy(tracks = updatedTracks))
    }

    fun updateCueFull(updatedCue: CaptionCue) {
        val pr = _activeProject.value ?: return
        val idx = _activeTrackIndex.value.coerceIn(0, max(0, pr.tracks.lastIndex))
        val track = pr.tracks.getOrNull(idx) ?: return
        if (track.locked) {
            showToast("Track is locked")
            return
        }
        val validStart = updatedCue.start.coerceIn(0.0, pr.duration)
        val validEnd = updatedCue.end.coerceIn(validStart + 0.1, pr.duration)
        val finalCue = updatedCue.copy(start = validStart, end = validEnd)
        val updatedCues = track.captions.map { c ->
            if (c.id == finalCue.id) finalCue else c
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
        showToast("Cue split at ${String.format(Locale.US, "%.2f", splitTime)}s")
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
        showToast("Video clip split at ${String.format(Locale.US, "%.2f", splitPoint)}s")
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

    fun reorderVideoClips(fromIndex: Int, toIndex: Int) {
        val pr = _activeProject.value ?: return
        if (fromIndex !in pr.videoClips.indices || toIndex !in pr.videoClips.indices || fromIndex == toIndex) return
        val reordered = pr.videoClips.toMutableList().apply {
            val moved = removeAt(fromIndex)
            add(toIndex, moved)
        }
        commitProjectChange(recalculateVideoClipTimeline(pr, reordered))
    }

    fun trimVideoClipSegment(clipId: String, newSourceTrimStart: Double, newDurationSec: Double) {
        val pr = _activeProject.value ?: return
        val validDuration = newDurationSec.coerceAtLeast(0.2)
        val validTrimStart = newSourceTrimStart.coerceAtLeast(0.0)
        val updated = pr.videoClips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(
                    sourceTrimStart = (validTrimStart * 100.0).roundToInt() / 100.0,
                    endInTimeline = ((clip.startInTimeline + validDuration) * 100.0).roundToInt() / 100.0
                )
            } else clip
        }
        commitProjectChange(recalculateVideoClipTimeline(pr, updated))
    }

    private fun recalculateVideoClipTimeline(project: Project, clips: List<VideoClipSegment>): Project {
        if (clips.isEmpty()) return project
        var cursor = project.trim.start.coerceAtLeast(0.0)
        val reflowed = clips.map { clip ->
            val dur = (clip.endInTimeline - clip.startInTimeline).coerceAtLeast(0.2)
            val start = (cursor * 100.0).roundToInt() / 100.0
            val end = ((cursor + dur) * 100.0).roundToInt() / 100.0
            cursor = end
            clip.copy(startInTimeline = start, endInTimeline = end)
        }
        val newTrimEnd = reflowed.last().endInTimeline
        return project.copy(
            videoClips = reflowed,
            trim = project.trim.copy(end = newTrimEnd),
            duration = max(project.duration, newTrimEnd)
        )
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
                newStyle = StylePresetCatalog.findByNameOrId("orange_persian_kinetic")!!.style
                newEffects = newEffects.copy(sfx = "Punchy Pop", sfxMode = "Both", sfxVolume = 65)
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
                        yPos = 0.24,
                        scale = 1.05,
                        animation = "Pop"
                    )
                )
            }
            "v1_orange_white_explainer" -> {
                newStyle = StylePresetCatalog.findByNameOrId("white_bold_colored_emphasis")!!.style
                newEffects = newEffects.copy(sfx = "Text Click", sfxMode = "Sentence", sfxVolume = 55)
            }
            "v1_purple_flare" -> {
                newEffects = newEffects.copy(sfx = "Fast Whoosh", sfxVolume = 60)
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
                newEffects = newEffects.copy(sfx = "Magical Shimmer", sfxVolume = 60)
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
                newEffects = newEffects.copy(sfx = "Atmospheric Ambience", sfxVolume = 45)
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
                newEffects = newEffects.copy(sfx = "Metallic Impact", sfxVolume = 65)
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
                newEffects = newEffects.copy(sfx = "Logo Reveal", sfxVolume = 55)
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
        sfxPlayer.playPreset(newEffects.sfx, newEffects.sfxVolume)
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
            _busyMessage.value = "Running speech recognition (ASR)..."
            val res = repository.runBackendTranscription(
                project = pr,
                language = language,
                model = model,
                attemptNetworkBackend = _isBackendOnline.value
            ) { msg ->
                _busyMessage.value = msg
            }
            _busyMessage.value = null
            res.fold(
                onSuccess = { refreshed ->
                    commitProjectChange(refreshed)
                    _activeTrackIndex.value = 0
                    val track = refreshed.tracks.firstOrNull()
                    _selectedCueId.value = track?.captions?.firstOrNull()?.id
                    _selectedWordIndex.value = 0
                    showToast("ASR Transcription complete (${track?.transcriptionSource ?: "ASR"}: ${track?.captions?.size ?: 0} cues)")
                },
                onFailure = {
                    val offlineSegmented = repository.buildOfflineSpeechTranscription(pr, language)
                    commitProjectChange(offlineSegmented)
                    _activeTrackIndex.value = 0
                    val track = offlineSegmented.tracks.firstOrNull()
                    _selectedCueId.value = track?.captions?.firstOrNull()?.id
                    _selectedWordIndex.value = 0
                    val count = track?.captions?.size ?: 0
                    if (track?.isDemoTrack == true) {
                        showToast("ASR offline: Loaded $count [DEMO] sample subtitles (not real speech recognition)")
                    } else {
                        showToast("Transcribed $count captions via ${track?.transcriptionSource ?: "local ASR"}")
                    }
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
            val res = if (_isBackendOnline.value) {
                repository.runBackendTranslation(
                    project = pr,
                    trackId = curTrack.id,
                    targetLang = targetLang,
                    mode = mode,
                    provider = provider
                ) { msg ->
                    _busyMessage.value = msg
                }
            } else {
                Result.failure(IllegalStateException("Backend offline"))
            }
            _busyMessage.value = null
            res.fold(
                onSuccess = { refreshed ->
                    commitProjectChange(refreshed)
                    _activeTrackIndex.value = refreshed.tracks.lastIndex
                    showToast("Translated to ${targetLang.uppercase()} via FastAPI backend")
                },
                onFailure = {
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
                    val updatedRecents = (listOf(fontName) + pr.recentFonts).distinct().take(8)
                    commitProjectChange(pr.copy(customFonts = updatedFonts, recentFonts = updatedRecents))
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
            _exportState.value = _exportState.value.copy(exportedSrtFile = srtFile)

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
