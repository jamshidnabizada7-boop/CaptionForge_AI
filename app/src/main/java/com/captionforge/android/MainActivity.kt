package com.captionforge.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.captionforge.android.ui.editor.EditorScreen
import com.captionforge.android.ui.home.HomeScreen
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.CaptionForgeTheme
import com.captionforge.android.viewmodel.CaptionForgeViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaptionForgeTheme {
                CaptionForgeApp()
            }
        }
    }
}

@Composable
fun CaptionForgeApp(
    viewModel: CaptionForgeViewModel = viewModel()
) {
    var currentRoute by rememberSaveable { mutableStateOf("home") }
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val busyMessage by viewModel.busyMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3200)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        when {
            currentRoute == "editor" && activeProject != null -> {
                EditorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentRoute = "home" }
                )
            }
            else -> {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenEditor = { currentRoute = "editor" }
                )
            }
        }

        // Global Busy Overlay for video probing / AI jobs
        busyMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.SurfaceElevated
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.PrimaryPurple)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Toast / Snackbar Banner
        toastMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = AppColors.SurfaceElevated,
                contentColor = Color.White,
                action = {
                    TextButton(onClick = { viewModel.clearToast() }) {
                        Text("OK", color = AppColors.PrimaryPurpleLight)
                    }
                }
            ) {
                Text(text = msg, fontSize = 12.sp)
            }
        }
    }
}
