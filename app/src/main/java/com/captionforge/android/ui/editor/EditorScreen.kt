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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.captionforge.android.model.AudioClip
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.SrtParser
import com.captionforge.android.model.Track
import com.captionforge.android.model.VideoEffects
import com.captionforge.android.network.BackendClient
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.viewmodel.CaptionForgeViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@kotlin.OptIn(ExperimentalLayoutApi::class)
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
    val selectedWordIndex by viewModel.selectedWordIndex.collectAsStateWithLifecycle()
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
    val activePlayheadCue = currentTrack.captions.firstOrNull { currentTime >= it.start && currentTime <= it.end }
    val targetCueForQuickAction = selectedCue ?: activePlayheadCue ?: currentTrack.captions.firstOrNull()
    val selectedOverlay = currentProject.overlays.firstOrNull { it.id == selectedOverlayId }

    var cueToEdit by remember { mutableStateOf<CaptionCue?>(null) }
    var overlayToEdit by remember { mutableStateOf<GraphicOverlay?>(null) }
    var audioClipToEdit by remember { mutableStateOf<AudioClip?>(null) }
    var showAddOverlaySheet by remember { mutableStateOf(false) }
    var showTrackManagerSheet by remember { mutableStateOf(false) }
    var showTranscriptionDialog by remember { mutableStateOf(false) }
    var showTranslateSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }
    var showResolutionMenu by remember { mutableStateOf(false) }
    var showMagicScriptOverlay by remember { mutableStateOf(false) }

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

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importUserAudioFromUri(uri)
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
        containerColor = Color(0xFF09090E),
        topBar = {
            // Magic Editor Style Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF09090E))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular dark Back Button
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            viewModel.setPlaying(false)
                            onNavigateBack()
                        }
                        .testTag("editor_back_button"),
                    shape = CircleShape,
                    color = Color(0xFF171721),
                    border = BorderStroke(1.dp, Color(0xFF262636))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Projects",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Project Title + "✔ Saved" Green Indicator underneath
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentProject.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Saved",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Circular Undo & Redo Buttons
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171721))
                        .testTag("editor_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color.White else Color(0xFF52526B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171721))
                        .testTag("editor_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) Color.White else Color(0xFF52526B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Aspect Ratio Chip
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showAspectMenu = true }
                            .testTag("aspect_ratio_selector"),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF171721),
                        border = BorderStroke(1.dp, Color(0xFF262636))
                    ) {
                        Text(
                            text = currentProject.aspectRatio,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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

                Spacer(modifier = Modifier.width(5.dp))

                // Resolution Pill ("720p ⌄")
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showResolutionMenu = true }
                            .testTag("resolution_selector"),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF171721),
                        border = BorderStroke(1.dp, Color(0xFF262636))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentProject.resolution}p",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                        }
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

                Spacer(modifier = Modifier.width(6.dp))

                // White Pill "[↑] Export" Button with PRO badge (matching Magic Editor)
                Box {
                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                viewModel.resetExportState()
                                showExportSheet = true
                            }
                            .testTag("editor_export_button"),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = Color(0xFF09090E),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF09090E)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-5).dp),
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFF635BFF)
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Center Video Stage + Left Vertical "Aa" Font Size Slider + Right Quick Action Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(4.5f)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Vertical "Aa" Font Size Pill Slider (exact match to Magic Editor Video 1 & 2)
                VerticalFontSizeSliderPill(
                    fontSize = currentTrack.style.fontSize.toFloat(),
                    onFontSizeChange = { newSize ->
                        viewModel.updateActiveTrackStyle { it.copy(fontSize = newSize.toDouble()) }
                    },
                    modifier = Modifier.padding(end = 6.dp)
                )

                // Center Rounded 9:16 Video Stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(targetAspect, matchHeightConstraintsFirst = true)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF12101B))
                            .border(
                                width = if (sfxPulse) 2.dp else 1.dp,
                                color = if (sfxPulse) Color(0xFF635BFF) else Color(0xFF232334),
                                shape = RoundedCornerShape(18.dp)
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

                        // Live Multi-Track Subtitle & Keyframe VFX Overlay Canvas with Direct Drag-to-Position
                        SubtitleOverlayCanvas(
                            project = currentProject,
                            currentTime = currentTime,
                            selectedCueId = selectedCueId,
                            selectedOverlayId = selectedOverlayId,
                            sfxPulse = sfxPulse,
                            onSelectCue = { track, cue ->
                                val trackIdx = currentProject.tracks.indexOfFirst { it.id == track.id }
                                if (trackIdx >= 0) viewModel.selectTrack(trackIdx)
                                viewModel.selectCue(cue.id)
                            },
                            onSelectOverlay = { ov ->
                                viewModel.selectOverlay(ov.id)
                            },
                            onDragCuePosition = { newX, newY ->
                                viewModel.updateCuePositionFromCanvas(newX, newY)
                            },
                            onDragOverlayPosition = { overlayId, newX, newY ->
                                viewModel.updateOverlayPositionFromCanvas(overlayId, newX, newY)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Interactive "Magic Script" Word-by-Word Transcript Overlay
                        if (showMagicScriptOverlay) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0A0915).copy(alpha = 0.94f))
                                    .border(1.dp, Color(0xFF635BFF).copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SCRIPT • TAP WORD TO SEEK & STYLE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AppColors.AccentCyan
                                        )
                                        Text(
                                            text = "Close ✕",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.TextSecondary,
                                            modifier = Modifier.clickable { showMagicScriptOverlay = false }
                                        )
                                    }

                                    currentTrack.captions.forEach { cue ->
                                        val isCueActive = currentTime >= cue.start && currentTime <= cue.end
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isCueActive) Color(0xFF635BFF).copy(alpha = 0.22f) else AppColors.SurfaceElevated,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCueActive) AppColors.AccentCyan else AppColors.SurfaceBorder
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.2f", cue.start)}s – ${String.format(Locale.US, "%.2f", cue.end)}s",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        color = AppColors.TextSecondary
                                                    )
                                                    Text(
                                                        text = "Edit Words ✎",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AppColors.AccentOrange,
                                                        modifier = Modifier.clickable {
                                                            viewModel.selectCue(cue.id)
                                                            cueToEdit = cue
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val wordsToShow = if (cue.words.isNotEmpty()) cue.words else {
                                                        SrtParser.buildWordTimings(cue.text, cue.start, cue.end)
                                                    }
                                                    wordsToShow.forEach { w ->
                                                        val isWordActive = currentTime >= w.start && currentTime <= w.end
                                                        val hasCustomColor = !w.customColor.isNullOrBlank()
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = when {
                                                                isWordActive -> AppColors.AccentOrange
                                                                hasCustomColor -> Color(0xFF635BFF).copy(alpha = 0.35f)
                                                                else -> AppColors.Surface
                                                            },
                                                            border = BorderStroke(
                                                                1.dp,
                                                                if (w.sfx != "None") AppColors.AccentGreen else AppColors.SurfaceBorder
                                                            ),
                                                            modifier = Modifier.clickable {
                                                                viewModel.selectCue(cue.id)
                                                                viewModel.updatePlaybackTime(w.start, fromUserSeek = true)
                                                                runCatching { exoPlayer.seekTo((w.start * 1000.0).roundToLong()) }
                                                            }
                                                        ) {
                                                            Text(
                                                                text = if (w.sfx != "None") "${w.text} ♪" else w.text,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isWordActive || w.scaleBoost > 1.05) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Vertical Floating Action Pills (Tracks, AI Captions, Script)
                Column(
                    modifier = Modifier.padding(start = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showTrackManagerSheet = true }
                            .testTag("quick_tracks_button"),
                        shape = CircleShape,
                        color = Color(0xFF171721),
                        border = BorderStroke(1.dp, Color(0xFF262636))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Tracks",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${currentProject.tracks.size}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showTranscriptionDialog = true }
                            .testTag("ai_captions_button"),
                        shape = CircleShape,
                        color = Color(0xFF635BFF).copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color(0xFF635BFF))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Captions",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showMagicScriptOverlay = !showMagicScriptOverlay }
                            .testTag("magic_script_button"),
                        shape = CircleShape,
                        color = if (showMagicScriptOverlay) AppColors.AccentOrange else Color(0xFF171721),
                        border = BorderStroke(1.dp, if (showMagicScriptOverlay) Color.White else Color(0xFF262636))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Script",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Dedicated Transport & Cue Action Row (Directly between Video Stage and Timeline, matching Magic Editor Video 1 & 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF09090E))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: White Play/Pause Circle + Dark Speaker/Mute Circle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.setPlaying(!isPlaying) }
                            .testTag("editor_play_btn"),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF09090E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.toggleMute() }
                            .testTag("editor_mute_btn"),
                        shape = CircleShape,
                        color = Color(0xFF171721),
                        border = BorderStroke(1.dp, Color(0xFF262636))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Center: Crisp Timecode "00:16.7 / 00:34"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTransportTime(currentTime, includeTenths = true),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = " / ${formatTransportTime(currentProject.duration, includeTenths = false)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                // Right: Cue Edit (✎), Split, Merge & Trash Delete (🗑) circular buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                targetCueForQuickAction?.let { cue ->
                                    viewModel.selectCue(cue.id)
                                    cueToEdit = cue
                                }
                            }
                            .testTag("edit_selected_cue_btn"),
                        shape = CircleShape,
                        color = Color(0xFF171721),
                        border = BorderStroke(1.dp, Color(0xFF262636))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Cue",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (selectedCue != null) {
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.splitSelectedCueAtPlayhead() }
                                .testTag("split_selected_cue_btn"),
                            shape = CircleShape,
                            color = Color(0xFF171721),
                            border = BorderStroke(1.dp, Color(0xFF262636))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = "Split Cue",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.mergeSelectedCueWithNext() }
                                .testTag("merge_selected_cue_btn"),
                            shape = CircleShape,
                            color = Color(0xFF171721),
                            border = BorderStroke(1.dp, Color(0xFF262636))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MergeType,
                                    contentDescription = "Merge Cue",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (selectedCue != null) {
                                    viewModel.deleteSelectedCue()
                                } else if (targetCueForQuickAction != null) {
                                    viewModel.selectCue(targetCueForQuickAction.id)
                                    viewModel.deleteSelectedCue()
                                }
                            }
                            .testTag("delete_selected_cue_btn"),
                        shape = CircleShape,
                        color = Color(0xFF2A1218),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Cue",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // 3. Chunky Rounded Purple Pill Timeline (Magic Editor Style)
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
                onEditAudioClip = { clip ->
                    audioClipToEdit = clip
                },
                onAddCueAtPlayhead = { viewModel.addCaptionAtPlayhead() },
                onSplitVideoClip = { viewModel.splitVideoClipAtPlayhead() },
                onCycleClipTransition = { viewModel.cycleClipTransition(it) },
                onOpenTrackManager = { showTrackManagerSheet = true },
                selectedWordIndex = selectedWordIndex,
                onSelectWord = { cue, wordIdx ->
                    viewModel.selectCue(cue.id)
                    viewModel.selectWordIndex(wordIdx)
                },
                onTrimCue = { cue, newStart, newEnd ->
                    viewModel.updateCue(cue.id, cue.text, newStart, newEnd)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.2f)
            )

            // 4. Bottom Tools & Trays Dock
            ToolsTraysComposable(
                activeTool = activeTool,
                project = currentProject,
                activeTrack = currentTrack,
                selectedCue = selectedCue,
                selectedOverlay = selectedOverlay,
                viewModel = viewModel,
                onOpenTranslateSheet = { showTranslateSheet = true },
                onOpenAddOverlaySheet = { showAddOverlaySheet = true },
                onOpenEditOverlayDialog = { overlayToEdit = it },
                onOpenEditCueDialog = { cueToEdit = it },
                onOpenEditAudioClipDialog = { audioClipToEdit = it },
                onPickCustomFont = { fontPickerLauncher.launch(arrayOf("*/*")) },
                onPickSrtFile = { srtPickerLauncher.launch(arrayOf("*/*")) },
                onPickUserAudioFile = { audioPickerLauncher.launch(arrayOf("audio/*")) }
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
            },
            onSaveFullCue = { updatedCue ->
                viewModel.updateCueFull(updatedCue)
            }
        )
    }

    audioClipToEdit?.let { clip ->
        EditAudioClipDialog(
            clip = clip,
            onDismiss = { audioClipToEdit = null },
            onSave = { updated -> viewModel.updateAudioClip(updated) },
            onDelete = { id -> viewModel.deleteAudioClip(id) }
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

@Composable
private fun VerticalFontSizeSliderPill(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minSize = 14f
    val maxSize = 56f
    val fraction = ((fontSize - minSize) / (maxSize - minSize)).coerceIn(0f, 1f)
    var trackHeightPx by remember { mutableFloatStateOf(1f) }

    Surface(
        modifier = modifier
            .width(38.dp)
            .height(190.dp)
            .testTag("vertical_font_size_slider"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF151520),
        border = BorderStroke(1.dp, Color(0xFF262636))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Aa",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            // Vertical Track
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(24.dp)
                    .padding(vertical = 8.dp)
                    .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val newFrac = 1f - (offset.y / trackHeightPx).coerceIn(0f, 1f)
                            onFontSizeChange(minSize + newFrac * (maxSize - minSize))
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                            val newFrac = 1f - (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                            onFontSizeChange(minSize + newFrac * (maxSize - minSize))
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background rail
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2D2D42))
                )

                // Active fill rail from bottom
                Box(
                    modifier = Modifier
                        .fillMaxHeight(fraction)
                        .width(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF635BFF))
                )

                // Circular white thumb
                val thumbOffsetY = -((trackHeightPx * fraction).roundToInt() - 7)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, thumbOffsetY) }
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color(0xFF635BFF), CircleShape)
                )
            }

            Text(
                text = "${fontSize.roundToInt()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

private fun formatTransportTime(sec: Double, includeTenths: Boolean): String {
    val safe = sec.coerceAtLeast(0.0)
    val mins = (safe / 60).toInt()
    val secs = (safe % 60).toInt()
    val tenths = ((safe - safe.toInt()) * 10).toInt()
    return if (includeTenths) {
        String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
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
