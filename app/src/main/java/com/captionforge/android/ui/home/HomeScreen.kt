package com.captionforge.android.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.captionforge.android.data.ProbedVideoInfo
import com.captionforge.android.model.Project
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.viewmodel.CaptionForgeViewModel
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CaptionForgeViewModel,
    onOpenEditor: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingProjects.collectAsStateWithLifecycle()
    val isBackendOnline by viewModel.isBackendOnline.collectAsStateWithLifecycle()
    val backendConfig by viewModel.backendConfig.collectAsStateWithLifecycle()
    val backendUrl by viewModel.backendUrl.collectAsStateWithLifecycle()

    var showNewProjectSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showProSheet by remember { mutableStateOf(false) }
    var pendingProbedVideo by remember { mutableStateOf<ProbedVideoInfo?>(null) }
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.probeSelectedVideo(uri) { probed ->
                pendingProbedVideo = probed
            }
        }
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AppColors.PrimaryPurple, AppColors.AccentCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "CaptionForge Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CaptionForge",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "AI Video & Motion Studio",
                            fontSize = 10.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .clickable { showServerDialog = true }
                            .testTag("backend_status_chip"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isBackendOnline) {
                            AppColors.AccentGreen.copy(alpha = 0.15f)
                        } else {
                            AppColors.SurfaceElevated
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isBackendOnline) AppColors.AccentGreen else AppColors.SurfaceBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isBackendOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Backend connectivity status",
                                tint = if (isBackendOnline) AppColors.AccentGreen else AppColors.AccentAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBackendOnline) "FastAPI Online" else "Local Studio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBackendOnline) AppColors.AccentGreen else AppColors.TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings and Capabilities",
                            tint = AppColors.TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { showProSheet = true },
                        modifier = Modifier.testTag("pro_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Studio Features",
                            tint = AppColors.PrimaryPurpleLight
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Feature Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2E1065), Color(0xFF161224))
                                )
                            )
                            .border(
                                1.5.dp,
                                AppColors.PrimaryPurple.copy(alpha = 0.55f),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { showProSheet = true }
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.PrimaryPurple.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Subtitles",
                                    tint = AppColors.PrimaryPurpleLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto subtitles, beautifully styled",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Multi-track timeline • Persian/Dari, Pashto, Urdu & Arabic RTL • Kinetic VFX & SFX",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Details",
                                tint = AppColors.PrimaryPurpleLight
                            )
                        }
                    }
                }

                // Primary CTA: New Project
                item {
                    Button(
                        onClick = { showNewProjectSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("new_project_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "New Project",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Project — Import Video",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Instant Reference Showcase Cards (Video 1 & Video 2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.openDemoShowcase(1, onOpenEditor)
                                }
                                .testTag("demo_video1_card"),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                            border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "VIDEO 1 PRESET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF7A00)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Persian Orange Hero",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Dual FA+EN tracks + Flare",
                                    fontSize = 11.sp,
                                    color = AppColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.openDemoShowcase(2, onOpenEditor)
                                }
                                .testTag("demo_video2_card"),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                            border = BorderStroke(1.dp, AppColors.AccentCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "VIDEO 2 PRESET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.AccentCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hologram & Flag VFX",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Keyframes + Metallic Title",
                                    fontSize = 11.sp,
                                    color = AppColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Recent Projects Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Projects (${projects.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        TextButton(
                            onClick = { viewModel.refreshHome() },
                            modifier = Modifier.testTag("refresh_projects_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", fontSize = 12.sp)
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.PrimaryPurple)
                        }
                    }
                } else if (projects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                            border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "No projects",
                                    tint = AppColors.TextMuted,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No projects yet",
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "Import a video from your device or open a showcase preset above.",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectRowCard(
                            project = project,
                            onClick = {
                                viewModel.openProject(project)
                                onOpenEditor()
                            },
                            onRename = { projectToRename = project },
                            onDuplicate = { viewModel.duplicateProject(project) },
                            onDelete = { projectToDelete = project }
                        )
                    }
                }
            }
        }
    }

    // Create New Project Bottom Sheet
    if (showNewProjectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewProjectSheet = false },
            containerColor = AppColors.SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Create New Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showNewProjectSheet = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        .testTag("pick_video_gallery_item"),
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AppColors.PrimaryPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Pick Video",
                                tint = AppColors.PrimaryPurpleLight
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose Video from Device Gallery",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "MP4, MOV, WebM • Zero-permission Android Photo Picker",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showNewProjectSheet = false
                            viewModel.openDemoShowcase(1, onOpenEditor)
                        }
                        .testTag("open_sample_project_item"),
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AppColors.AccentCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Demo Video",
                                tint = AppColors.AccentCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open Bundled Sample Video Project",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Real speech MP4 with bilingual Persian/English kinetic tracks",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Import & Trim Video Confirmation Bottom Sheet
    pendingProbedVideo?.let { probed ->
        var trimStart by remember(probed) { mutableStateOf(0f) }
        val maxDur = max(1f, probed.durationSec.toFloat())
        var trimEnd by remember(probed) { mutableStateOf(maxDur) }

        ModalBottomSheet(
            onDismissRequest = { pendingProbedVideo = null },
            containerColor = AppColors.SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import & Trim Video",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = { pendingProbedVideo = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = AppColors.PrimaryPurpleLight,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = probed.filename,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            val mb = String.format(Locale.US, "%.1f", probed.sizeBytes / (1024.0 * 1024.0))
                            Text(
                                text = "$mb MB • ${probed.width}x${probed.height} • Selected: ${String.format(Locale.US, "%.1f", trimEnd - trimStart)}s",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Trim Duration (${String.format(Locale.US, "%.1f", trimStart)}s – ${String.format(Locale.US, "%.1f", trimEnd)}s):",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
                RangeSlider(
                    value = trimStart..trimEnd,
                    onValueChange = { range ->
                        if (range.endInclusive - range.start >= 0.5f) {
                            trimStart = range.start
                            trimEnd = range.endInclusive
                        }
                    },
                    valueRange = 0f..maxDur
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val target = probed
                        pendingProbedVideo = null
                        viewModel.createProjectFromProbedVideo(
                            probed = target,
                            trimStart = trimStart.toDouble(),
                            trimEnd = trimEnd.toDouble(),
                            onCreated = onOpenEditor
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_import_video_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
                ) {
                    Text(
                        text = "Import & Open in Editor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Settings & Capabilities Modal Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = AppColors.SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "CaptionForge Settings & Capabilities",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        CapabilityStatusRow(
                            label = "Native Android Playback",
                            value = "Ready (Media3 ExoPlayer + GPU ColorMatrix)",
                            isReady = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.SurfaceBorder)
                        CapabilityStatusRow(
                            label = "Multi-Script RTL Typography",
                            value = "Ready (Vazirmatn, Noto Arabic, Nastaliq, Amiri)",
                            isReady = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.SurfaceBorder)
                        CapabilityStatusRow(
                            label = "AI Speech Transcription (Faster-Whisper)",
                            value = if (backendConfig?.transcription == "faster-whisper") {
                                "Ready on Backend"
                            } else {
                                "Requires FastAPI Backend"
                            },
                            isReady = backendConfig?.transcription == "faster-whisper"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.SurfaceBorder)
                        CapabilityStatusRow(
                            label = "MP4 Video Renderer",
                            value = if (backendConfig?.render == true) {
                                "Ready (FastAPI FFmpeg + On-Device Media3)"
                            } else {
                                "Ready (On-Device Media3 Transformer + SRT)"
                            },
                            isReady = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.SurfaceBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSettingsSheet = false
                                    showServerDialog = true
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Backend Server URL", fontSize = 13.sp, color = AppColors.TextPrimary)
                            Text(
                                text = backendUrl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.PrimaryPurpleLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        showSettingsSheet = false
                        showServerDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure FastAPI Backend URL")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Backend Server Configuration Dialog
    if (showServerDialog) {
        var urlInput by remember(backendUrl) { mutableStateOf(backendUrl) }
        var testStatus by remember { mutableStateOf<String?>(null) }
        var testSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            containerColor = AppColors.SurfaceElevated,
            title = {
                Text("FastAPI Backend Configuration", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CaptionForge runs locally on Android and connects to your Python FastAPI server for Faster-Whisper ASR, OpenCV tracking, and FFmpeg MP4 burning.",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = urlInput == "http://10.0.2.2:8000",
                            onClick = { urlInput = "http://10.0.2.2:8000" },
                            label = { Text("Emulator", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = urlInput == "http://127.0.0.1:8000",
                            onClick = { urlInput = "http://127.0.0.1:8000" },
                            label = { Text("USB ADB", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = urlInput == "http://192.168.1.100:8000",
                            onClick = { urlInput = "http://192.168.1.100:8000" },
                            label = { Text("LAN Wi-Fi", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Server Base URL") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input")
                    )
                    OutlinedButton(
                        onClick = {
                            testStatus = "Testing connection..."
                            viewModel.updateServerUrl(urlInput) { ok, message ->
                                testSuccess = ok
                                testStatus = message
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Connection")
                    }
                    testStatus?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = if (testSuccess) AppColors.AccentGreen else AppColors.AccentAmber
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateServerUrl(urlInput) { _, _ -> }
                        showServerDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Pro Studio Info Sheet
    if (showProSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProSheet = false },
            containerColor = AppColors.SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.PrimaryPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = AppColors.PrimaryPurpleLight,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CaptionForge Studio Architecture",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Native Jetpack Compose + Media3 ExoPlayer client\n" +
                        "• Multi-track RTL subtitles (English, Persian/Dari, Pashto, Urdu, Arabic)\n" +
                        "• Keyframe VFX overlays, Waving Flag, Hand Hologram & Metallic Reveal\n" +
                        "• 11-category royalty-free SFX synthesizer & FastAPI FFmpeg integration",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showProSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
                ) {
                    Text("Got It")
                }
            }
        }
    }

    // Rename Project Dialog
    projectToRename?.let { proj ->
        var nameText by remember(proj) { mutableStateOf(proj.name) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            containerColor = AppColors.SurfaceElevated,
            title = { Text("Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameProject(proj, nameText)
                        projectToRename = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Project Dialog
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = AppColors.SurfaceElevated,
            title = { Text("Delete Project", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${proj.name}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(proj)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentCoral)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CapabilityStatusRow(
    label: String,
    value: String,
    isReady: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isReady) AppColors.AccentGreen else AppColors.AccentAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isReady) AppColors.AccentGreen else AppColors.AccentAmber
            )
        }
    }
}

@Composable
private fun ProjectRowCard(
    project: Project,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_item_${project.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF31236A), AppColors.PrimaryPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", project.duration)}s",
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalCues = project.tracks.sumOf { it.captions.size }
                Text(
                    text = "${project.aspectRatio} • ${project.resolution}p • ${project.tracks.size} tracks ($totalCues cues) • ${project.overlays.size} VFX",
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Project Actions",
                        tint = AppColors.TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = AppColors.AccentCoral) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
