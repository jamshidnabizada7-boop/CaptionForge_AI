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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.captionforge.android.data.ProbedVideoInfo
import com.captionforge.android.model.Project
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.BebasNeueFontFamily
import com.captionforge.android.ui.theme.LalezarFontFamily
import com.captionforge.android.ui.theme.VazirmatnFontFamily
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
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF121026), AppColors.Background)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Signature "MAGIC EDITOR" Stacked Rounded Indigo Badge (inspired by reference icon)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4338CA), Color(0xFF6366F1), Color(0xFF9333EA))
                                )
                            )
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.28f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "MAGIC",
                                fontFamily = BebasNeueFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE0E7FF),
                                lineHeight = 10.sp
                            )
                            Text(
                                text = "EDITOR",
                                fontFamily = BebasNeueFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                lineHeight = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CaptionForge",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppColors.AccentOrange.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, AppColors.AccentOrange.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.AccentOrange,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "AI Kinetic Subtitles & Motion VFX Studio",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .clickable { showServerDialog = true }
                            .testTag("backend_status_chip"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isBackendOnline) {
                            AppColors.AccentGreen.copy(alpha = 0.14f)
                        } else {
                            AppColors.SurfaceElevated
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isBackendOnline) AppColors.AccentGreen.copy(alpha = 0.7f) else AppColors.SurfaceBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isBackendOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Backend connectivity status",
                                tint = if (isBackendOnline) AppColors.AccentGreen else AppColors.AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBackendOnline) "AI Cloud" else "On-Device",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBackendOnline) AppColors.AccentGreen else Color.White
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
                            tint = AppColors.AccentAmber
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.12f),
                            radius = size.width * 0.7f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF6B00).copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.15f, size.height * 0.42f),
                            radius = size.width * 0.6f
                        )
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Flagship "Magic Editor" Hero Studio Card + Primary CTA
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF281C66),
                                        Color(0xFF171336),
                                        Color(0xFF0F0D21)
                                    )
                                )
                            )
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFFA855F7),
                                        Color(0xFFFF6B00).copy(alpha = 0.65f)
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MAGIC AI CAPTION & VFX STUDIO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.AccentCyan
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Viral Kinetic Captions & 3D Titles",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "3-Tier RTL • 26 Pro Fonts • 60 Studio SFX",
                                        fontSize = 11.sp,
                                        color = AppColors.TextSecondary
                                    )
                                }

                                // Live Floating Kinetic Preview Pill
                                Surface(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable { showProSheet = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0A0914).copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, AppColors.AccentOrange.copy(alpha = 0.7f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "ویزای تحصیلی",
                                            fontFamily = VazirmatnFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "فنلاند",
                                            fontFamily = LalezarFontFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AppColors.AccentOrange
                                        )
                                    }
                                }
                            }

                            // Primary Gradient CTA Button ("New Project")
                            Button(
                                onClick = { showNewProjectSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("new_project_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.MagicIndigo
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AppColors.MagicButtonGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "New Project",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "New Magic Project — Import Video",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Instant Reference Showcase Cards (Video 1 & Video 2)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Featured Cinematic Templates",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "TAP TO EDIT LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.AccentCyan
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reference Video 1 Showcase Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.openDemoShowcase(1, onOpenEditor)
                                    }
                                    .testTag("demo_video1_card"),
                                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                                border = BorderStroke(1.2.dp, AppColors.AccentOrange.copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AppColors.AccentOrange.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "REF VIDEO #1",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AppColors.AccentOrange,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "9:16 • 1080p",
                                            fontSize = 9.sp,
                                            color = AppColors.TextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Mini visual stage preview
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(74.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF2B1206), Color(0xFF0E0B1A))
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                AppColors.AccentOrange.copy(alpha = 0.35f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "ویزای تحصیلی",
                                                fontFamily = VazirmatnFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "فنلاند",
                                                fontFamily = LalezarFontFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AppColors.AccentOrange
                                            )
                                            Text(
                                                text = "بدون مدرک زبان",
                                                fontFamily = VazirmatnFontFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.AccentAmber
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "3-Tier Persian Kinetic",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "3D Orange Hero + Word SFX",
                                        fontSize = 11.sp,
                                        color = AppColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Reference Video 2 Showcase Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.openDemoShowcase(2, onOpenEditor)
                                    }
                                    .testTag("demo_video2_card"),
                                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                                border = BorderStroke(1.2.dp, AppColors.AccentCyan.copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AppColors.AccentCyan.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "REF VIDEO #2",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AppColors.AccentCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "VFX • Tracked",
                                            fontSize = 9.sp,
                                            color = AppColors.TextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Mini visual stage preview
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(74.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF072235), Color(0xFF100C24))
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                AppColors.AccentCyan.copy(alpha = 0.35f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "◈ HOLOGRAM VFX ◈",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AppColors.AccentCyan
                                            )
                                            Text(
                                                text = "AUSTRALIA",
                                                fontFamily = BebasNeueFontFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFE2E8F0),
                                                letterSpacing = 2.sp
                                            )
                                            Text(
                                                text = "Subject Depth + Flag",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppColors.PrimaryPurpleLight
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Subject Depth & VFX",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Behind-Subject Text + Holo",
                                        fontSize = 11.sp,
                                        color = AppColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Recent Projects Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Recent Studio Projects",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = AppColors.SurfaceElevated
                            ) {
                                Text(
                                    text = "${projects.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.PrimaryPurpleLight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
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
                            Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            border = BorderStroke(1.dp, AppColors.SurfaceBorder),
                            shape = RoundedCornerShape(18.dp)
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
                                    text = "Tap 'New Magic Project' or choose a Featured Template above.",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary,
                                    textAlign = TextAlign.Center
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
                    text = "Create New Magic Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showNewProjectSheet = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        .testTag("pick_video_gallery_item"),
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.PrimaryPurple.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppColors.MagicBrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Pick Video",
                                tint = Color.White
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
                        .clip(RoundedCornerShape(14.dp))
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppColors.AccentOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Demo Video",
                                tint = AppColors.AccentOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open 3-Tier Persian Kinetic Showcase",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Reference Video 1 style with word-level SFX & 3D orange headline",
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
                        text = "Import & Open in Magic Editor",
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
                    text = "MagicCap • Studio Capabilities",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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
                            label = "26 Multi-Script RTL & Latin Fonts",
                            value = "Ready (FA, PS, UR, AR, EN + Custom .TTF)",
                            isReady = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.SurfaceBorder)
                        CapabilityStatusRow(
                            label = "60 Studio Sound Effects & Music",
                            value = "Ready (23 Categories + 32-Bar Waveforms)",
                            isReady = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.SurfaceBorder)
                        CapabilityStatusRow(
                            label = "AI Speech Transcription (Faster-Whisper)",
                            value = if (backendConfig?.transcription == "faster-whisper") {
                                "Ready on Backend"
                            } else {
                                "Connect FastAPI Server"
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
                        text = "MagicCap runs locally on Android and connects to your Python FastAPI server for Faster-Whisper ASR, OpenCV tracking, and FFmpeg MP4 burning.",
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
                        .background(AppColors.MagicBrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "MagicCap • Pro Studio Engine",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Interactive Magic Script Mode: Tap any word to seek, highlight, or attach SFX\n" +
                        "• 3-Tier Persian/Dari, Pashto, Urdu, Arabic & English Kinetic Typography\n" +
                        "• 26 Bundled Open-Source Fonts + Custom .TTF/.OTF Font Importer\n" +
                        "• 60 Studio Sound Effects & Loops across 23 categories with 32-bar waveforms\n" +
                        "• Subject-Occluded Depth Titles, Hand/Face Tracking & Dual MP4 Export",
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
private fun QuickFeaturePill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141229).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
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
        shape = RoundedCornerShape(16.dp),
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
                    .size(width = 54.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3B28CC), AppColors.PrimaryPurple, Color(0xFFEC4899))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", project.duration)}s",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalCues = project.tracks.sumOf { it.captions.size }
                Text(
                    text = "${project.aspectRatio} • ${project.resolution}p • ${project.tracks.size} tracks ($totalCues cues) • ${project.audioClips.size} SFX",
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    project.tracks.take(3).forEach { tr ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppColors.SurfaceElevated,
                            border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                        ) {
                            Text(
                                text = tr.language.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.AccentCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (project.overlays.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppColors.PrimaryPurple.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${project.overlays.size} VFX Layers",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.PrimaryPurpleLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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
