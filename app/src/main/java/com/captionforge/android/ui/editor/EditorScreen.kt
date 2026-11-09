package com.captionforge.android.ui.editor

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.Track
import com.captionforge.android.model.VideoEffects
import com.captionforge.android.network.BackendClient
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.viewmodel.CaptionForgeViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    viewModel: CaptionForgeViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        viewModel.setPlaying(false)
        onNavigateBack()
    }

    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    val activeTrackIndex by viewModel.activeTrackIndex.collectAsStateWithLifecycle()
    val selectedCueId by viewModel.selectedCueId.collectAsStateWithLifecycle()
    val selectedOverlayId by viewModel.selectedOverlayId.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val sfxPulse by viewModel.sfxPulse.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val isBackendOnline by viewModel.isBackendOnline.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    val currentProject = project ?: return
    val currentTrack = currentProject.tracks.getOrNull(activeTrackIndex)
        ?: currentProject.tracks.firstOrNull()
        ?: Track(id = "tr_fallback", name = "Original")

    val selectedCue = currentTrack.captions.firstOrNull { it.id == selectedCueId }

    var cueToEdit by remember { mutableStateOf<CaptionCue?>(null) }
    var overlayToEdit by remember { mutableStateOf<GraphicOverlay?>(null) }
    var showAddOverlaySheet by remember { mutableStateOf(false) }
    var showTrackManagerSheet by remember { mutableStateOf(false) }
    var showTranscriptionDialog by remember { mutableStateOf(false) }
    var showTranslateSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }
    var showResolutionMenu by remember { mutableStateOf(false) }

    val srtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSrtFromUri(uri)
        }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomFontFromUri(uri)
        }
    }

    val context = LocalContext.current

    // Native Media3 ExoPlayer Setup
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Load video source into ExoPlayer when project changes
    DisposableEffect(currentProject.id, currentProject.source) {
        val uriString = when {
            currentProject.source.startsWith("asset://") ||
                currentProject.source.startsWith("file://") ||
                currentProject.source.startsWith("content://") ||
                currentProject.source.startsWith("http://") ||
                currentProject.source.startsWith("https://") -> currentProject.source
            else -> BackendClient.getVideoStreamUrl(currentProject.id)
        }
        runCatching {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
            exoPlayer.prepare()
            exoPlayer.seekTo((currentProject.trim.start * 1000.0).roundToLong())
        }
        onDispose {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Sync mute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Sync play/pause & 30fps timeline playhead loop
    LaunchedEffect(isPlaying, currentProject.trim) {
        if (isPlaying) {
            runCatching {
                if (exoPlayer.playbackState == Player.STATE_IDLE) {
                    exoPlayer.prepare()
                }
                val startMs = (currentProject.trim.start * 1000.0).roundToLong()
                val curMs = (viewModel.currentTime.value * 1000.0).roundToLong()
                if (curMs >= (currentProject.trim.end * 1000.0).roundToLong() || curMs < startMs) {
                    exoPlayer.seekTo(startMs)
                    viewModel.updatePlaybackTime(currentProject.trim.start, fromUserSeek = true)
                }
                exoPlayer.play()
            }

            while (viewModel.isPlaying.value) {
                val playerTimeSec = if (exoPlayer.isPlaying && exoPlayer.duration > 0) {
                    exoPlayer.currentPosition / 1000.0
                } else {
                    viewModel.currentTime.value + 0.033
                }

                if (playerTimeSec >= currentProject.trim.end) {
                    exoPlayer.pause()
                    val resetMs = (currentProject.trim.start * 1000.0).roundToLong()
                    exoPlayer.seekTo(resetMs)
                    viewModel.updatePlaybackTime(currentProject.trim.start, fromUserSeek = true)
                    viewModel.setPlaying(false)
                    break
                } else {
                    viewModel.updatePlaybackTime(playerTimeSec, fromUserSeek = false)
                }
                delay(33L)
            }
        } else {
            runCatching { exoPlayer.pause() }
        }
    }

    val targetAspect = when (currentProject.aspectRatio) {
        "16:9" -> 16f / 9f
        "1:1" -> 1f
        else -> 9f / 16f
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = AppColors.Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.setPlaying(false)
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Projects",
                        tint = Color.White
                    )
                }

                Text(
                    text = currentProject.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("editor_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color.White else AppColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("editor_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) Color.White else AppColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Aspect Ratio Selector Chip
                Box {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAspectMenu = true }
                            .testTag("aspect_ratio_selector"),
                        color = AppColors.SurfaceElevated,
                        border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                    ) {
                        Text(
                            text = currentProject.aspectRatio,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showAspectMenu,
                        onDismissRequest = { showAspectMenu = false }
                    ) {
                        listOf("9:16", "1:1", "16:9").forEach { ratio ->
                            DropdownMenuItem(
                                text = { Text(ratio) },
                                onClick = {
                                    showAspectMenu = false
                                    viewModel.updateAspectRatio(ratio)
                                }
                            )
                        }
                    }
                }

                // Resolution Selector Chip
                Box {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showResolutionMenu = true }
                            .testTag("resolution_selector"),
                        color = AppColors.SurfaceElevated,
                        border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                    ) {
                        Text(
                            text = "${currentProject.resolution}p",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showResolutionMenu,
                        onDismissRequest = { showResolutionMenu = false }
                    ) {
                        listOf(480, 720, 1080).forEach { res ->
                            DropdownMenuItem(
                                text = { Text("${res}p") },
                                onClick = {
                                    showResolutionMenu = false
                                    viewModel.updateResolution(res)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = {
                        viewModel.resetExportState()
                        showExportSheet = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("editor_export_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
                ) {
                    Text("Export", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Center Video Preview Canvas with Real ExoPlayer + Live Subtitle & VFX Compositor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(4.6f)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(targetAspect, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF120E1E))
                        .border(
                            width = if (sfxPulse) 2.5.dp else 1.5.dp,
                            color = if (sfxPulse) AppColors.AccentCyan else AppColors.SurfaceBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    // Native ExoPlayer Surface with Live GPU ColorMatrix Filter
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                player = exoPlayer
                            }
                        },
                        update = { playerView ->
                            playerView.player = exoPlayer
                            applyVideoEffectsColorMatrix(playerView, currentProject.effects)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Timecode & Active Audio/Voice Badges
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", currentTime)}s / ${String.format(Locale.US, "%.2f", currentProject.duration)}s",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (currentProject.effects.sfx != "None") {
                            Text(
                                text = "SFX: ${currentProject.effects.sfx} (${currentProject.effects.sfxMode})",
                                fontSize = 9.sp,
                                color = if (sfxPulse) AppColors.AccentCyan else AppColors.TextSecondary
                            )
                        }
                        if (currentProject.effects.clearVoice != "Off") {
                            Text(
                                text = "Clear Voice: ${currentProject.effects.clearVoice}",
                                fontSize = 9.sp,
                                color = AppColors.AccentGreen
                            )
                        }
                    }

                    // Live Multi-Track Subtitle & Keyframe VFX Overlay Canvas
                    SubtitleOverlayCanvas(
                        tracks = currentProject.tracks,
                        overlays = currentProject.overlays,
                        currentTime = currentTime,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top-Right Floating Quick Actions: Tracks & AI Captions
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTrackManagerSheet = true }
                                .testTag("quick_tracks_button"),
                            color = AppColors.SurfaceElevated.copy(alpha = 0.88f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = AppColors.PrimaryPurpleLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentProject.tracks.size} Tracks",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTranscriptionDialog = true }
                                .testTag("ai_captions_button"),
                            color = AppColors.PrimaryPurple.copy(alpha = 0.90f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI Captions",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Bottom-Right Floating Playback Controls (Mute & Play/Pause)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.toggleMute() },
                            containerColor = AppColors.SurfaceElevated.copy(alpha = 0.88f),
                            contentColor = Color.White,
                            modifier = Modifier.testTag("editor_mute_btn")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        SmallFloatingActionButton(
                            onClick = { viewModel.setPlaying(!isPlaying) },
                            containerColor = Color.White,
                            contentColor = AppColors.Background,
                            modifier = Modifier.testTag("editor_play_btn")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. Contextual Action Bar when a Subtitle Cue is Selected
            if (selectedCue != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cue: \"${selectedCue.text}\"",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { cueToEdit = selectedCue },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("edit_selected_cue_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Cue", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.splitSelectedCueAtPlayhead() },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("split_selected_cue_btn")
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Split Cue", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.mergeSelectedCueWithNext() },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("merge_selected_cue_btn")
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = "Merge Next", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.deleteSelectedCue() },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("delete_selected_cue_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Cue", tint = AppColors.AccentCoral, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.selectCue(null) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Deselect", tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 3. Multi-Track Interactive Scrubber Timeline
            TimelineComposable(
                project = currentProject,
                currentTime = currentTime,
                activeTrackIndex = activeTrackIndex,
                selectedCueId = selectedCueId,
                selectedOverlayId = selectedOverlayId,
                onSeek = { sec ->
                    viewModel.updatePlaybackTime(sec, fromUserSeek = true)
                    runCatching { exoPlayer.seekTo((sec * 1000.0).roundToLong()) }
                },
                onSelectTrack = { viewModel.selectTrack(it) },
                onSelectCue = { cue ->
                    viewModel.selectCue(cue.id)
                    viewModel.updatePlaybackTime(cue.start, fromUserSeek = true)
                    runCatching { exoPlayer.seekTo((cue.start * 1000.0).roundToLong()) }
                },
                onEditCue = { cueToEdit = it },
                onSelectOverlay = { ov ->
                    viewModel.selectOverlay(ov.id)
                    overlayToEdit = ov
                },
                onAddCueAtPlayhead = { viewModel.addCaptionAtPlayhead() },
                onSplitVideoClip = { viewModel.splitVideoClipAtPlayhead() },
                onCycleClipTransition = { viewModel.cycleClipTransition(it) },
                onOpenTrackManager = { showTrackManagerSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3.4f)
            )

            // 4. Bottom Tools & Trays Dock
            ToolsTraysComposable(
                project = currentProject,
                activeTool = activeTool,
                currentTrack = currentTrack,
                effects = currentProject.effects,
                onSelectTool = { viewModel.selectTool(it) },
                onUpdateStyle = { viewModel.updateActiveTrackStyle(it) },
                onUpdateEffects = { viewModel.updateVideoEffects(it) },
                onApplyTemplate = { viewModel.applyCinematicTemplate(it) },
                onAddOverlayModal = { showAddOverlaySheet = true },
                onEditOverlay = { overlayToEdit = it },
                onToggleOverlayVisibility = { viewModel.toggleOverlayVisibility(it) },
                onDeleteOverlay = { viewModel.deleteOverlay(it) },
                onTrackOverlayMotion = { viewModel.trackMotionForOverlay(it, "face") },
                onOpenTranslateModal = { showTranslateSheet = true },
                onImportSrt = { srtPickerLauncher.launch(arrayOf("*/*")) },
                onExportSrt = { viewModel.exportSrtSidecar() },
                onImportCustomFont = { fontPickerLauncher.launch(arrayOf("*/*")) },
                onAuditionSfx = { viewModel.auditionAndSelectSfx(it) },
                onAddSfxClipAtPlayhead = { viewModel.addSfxClipAtPlayhead(it) },
                onUpdateTrim = { s, e -> viewModel.updateVideoTrim(s, e) },
                onSplitClipAtPlayhead = { viewModel.splitVideoClipAtPlayhead() }
            )
        }
    }

    // Dialogs & Sheets
    cueToEdit?.let { cue ->
        EditCueDialog(
            cue = cue,
            onDismiss = { cueToEdit = null },
            onSave = { newText, newStart, newEnd ->
                viewModel.updateCue(cue.id, newText, newStart, newEnd)
            }
        )
    }

    overlayToEdit?.let { ov ->
        EditOverlayAndKeyframesDialog(
            overlay = ov,
            currentTime = currentTime,
            onDismiss = { overlayToEdit = null },
            onSave = { updated -> viewModel.updateOverlay(updated) }
        )
    }

    if (showAddOverlaySheet) {
        AddOverlayBottomSheet(
            onDismiss = { showAddOverlaySheet = false },
            onAddLayer = { type, name, content, color ->
                viewModel.addGraphicOverlay(type, name, content, color)
            }
        )
    }

    if (showTrackManagerSheet) {
        TrackManagerBottomSheet(
            project = currentProject,
            activeTrackIndex = activeTrackIndex,
            onSelectTrack = { viewModel.selectTrack(it) },
            onAddTrack = { name, lang -> viewModel.addTrack(name, lang) },
            onToggleVisibility = { viewModel.toggleTrackVisibility(it) },
            onToggleLock = { viewModel.toggleTrackLock(it) },
            onDeleteTrack = { viewModel.deleteTrack(it) },
            onDismiss = { showTrackManagerSheet = false }
        )
    }

    if (showTranscriptionDialog) {
        AutoAiTranscriptionDialog(
            isBackendOnline = isBackendOnline,
            onDismiss = { showTranscriptionDialog = false },
            onConfirmTranscribe = { lang, model ->
                viewModel.triggerAiTranscription(lang, model)
            }
        )
    }

    if (showTranslateSheet) {
        TranslateBottomSheet(
            isBackendOnline = isBackendOnline,
            onDismiss = { showTranslateSheet = false },
            onTranslate = { lang, mode, provider ->
                viewModel.triggerAiTranslation(lang, mode, provider)
            }
        )
    }

    if (showExportSheet) {
        ExportBottomSheet(
            project = currentProject,
            exportState = exportState,
            onSelectResolution = { viewModel.updateResolution(it) },
            onExportSrt = { viewModel.exportSrtSidecar() },
            onStartMp4Export = { res -> viewModel.startVideoExport(res) },
            onDismiss = { showExportSheet = false }
        )
    }
}

private fun applyVideoEffectsColorMatrix(view: View, effects: VideoEffects) {
    val b = effects.brightness.toFloat()
    val c = effects.contrast.toFloat()
    val s = effects.saturation.toFloat()

    val isIdentity = kotlin.math.abs(b) < 0.01f &&
        kotlin.math.abs(c - 1f) < 0.01f &&
        kotlin.math.abs(s - 1f) < 0.01f

    if (isIdentity) {
        view.setLayerType(View.LAYER_TYPE_NONE, null)
        return
    }

    val invSat = 1f - s
    val r = 0.213f * invSat
    val g = 0.715f * invSat
    val bl = 0.072f * invSat
    val t = ((1f - c) / 2f * 255f) + (b * 255f)

    val matrixValues = floatArrayOf(
        (r + s) * c, g * c, bl * c, 0f, t,
        r * c, (g + s) * c, bl * c, 0f, t,
        r * c, g * c, (bl + s) * c, 0f, t,
        0f, 0f, 0f, 1f, 0f
    )
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(matrixValues))
    }
    view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
}
