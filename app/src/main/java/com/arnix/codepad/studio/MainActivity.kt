package com.arnix.codepad.studio

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arnix.codepad.studio.data.CodePadDatabase
import com.arnix.codepad.studio.data.CodeProjectRepository
import com.arnix.codepad.studio.engine.TypingSoundEngine
import com.arnix.codepad.studio.ui.AutoWriteStudioScreen
import com.arnix.codepad.studio.ui.CodePadStudioScreen
import com.arnix.codepad.studio.ui.MoreStudioScreen
import com.arnix.codepad.studio.ui.SplashScreen
import com.arnix.codepad.studio.ui.components.AppOnboardingGuideDialog
import com.arnix.codepad.studio.ui.theme.MyApplicationTheme
import com.arnix.codepad.studio.viewmodel.StudioModule
import com.arnix.codepad.studio.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            enableEdgeToEdge()
        }
        setContent {
            CodePadStudioAppRoot()
        }
    }
}

@Composable
fun CodePadStudioAppRoot() {
    val context = LocalContext.current
    var isSplashVisible by remember { mutableStateOf(true) }

    val repository = remember {
        val db = CodePadDatabase.getInstance(context)
        CodeProjectRepository(db.codeProjectDao())
    }
    val studioViewModel: StudioViewModel = viewModel(
        factory = StudioViewModel.provideFactory(repository)
    )

    LaunchedEffect(Unit) {
        runCatching {
            TypingSoundEngine.initialize(context)
            studioViewModel.setAppContext(context)
        }
    }
    val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = isSplashVisible || uiState.isOnboardingOpen || uiState.isRecordingMode || uiState.activeModule != StudioModule.CODEPAD_IDE) {
        if (isSplashVisible) {
            isSplashVisible = false
        } else if (uiState.isOnboardingOpen) {
            studioViewModel.dismissOnboardingGuide(markCompleted = true)
        } else if (uiState.isRecordingMode) {
            studioViewModel.handleTripleTapGesture(context)
        } else {
            studioViewModel.selectModule(StudioModule.CODEPAD_IDE)
        }
    }

    val isRecordingModeActive = uiState.isRecordingMode ||
        (uiState.activeModule == StudioModule.AUTO_WRITE_STUDIO && uiState.isRecordingMode)

    LaunchedEffect(isRecordingModeActive) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isRecordingModeActive) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val showNavigationBar =
        !(uiState.activeModule == StudioModule.AUTO_WRITE_STUDIO && uiState.isRecordingMode)

    MyApplicationTheme(
        darkTheme = uiState.isDarkTheme,
        uiFontScale = uiState.uiFontScale
    ) {
        AnimatedContent(
            targetState = isSplashVisible,
            transitionSpec = {
                fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
            },
            label = "splashTransition"
        ) { splashActive ->
            if (splashActive) {
                SplashScreen(
                    onFinished = { isSplashVisible = false }
                )
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = if (isRecordingModeActive) WindowInsets(0.dp) else ScaffoldDefaults.contentWindowInsets,
                    bottomBar = {
                        if (showNavigationBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                            ) {
                                val isIde = uiState.activeModule == StudioModule.CODEPAD_IDE
                                NavigationBarItem(
                                    selected = isIde,
                                    onClick = { studioViewModel.selectModule(StudioModule.CODEPAD_IDE) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isIde) Icons.Filled.Code else Icons.Outlined.Code,
                                            contentDescription = "Editor"
                                        )
                                    },
                                    label = { Text("Editor") },
                                    modifier = Modifier.testTag("nav_editor")
                                )

                                val isAutoWrite = uiState.activeModule == StudioModule.AUTO_WRITE_STUDIO
                                NavigationBarItem(
                                    selected = isAutoWrite,
                                    onClick = { studioViewModel.selectModule(StudioModule.AUTO_WRITE_STUDIO) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isAutoWrite) {
                                                Icons.Filled.Videocam
                                            } else {
                                                Icons.Outlined.Videocam
                                            },
                                            contentDescription = "Rec"
                                        )
                                    },
                                    label = { Text("Rec") },
                                    modifier = Modifier.testTag("nav_rec")
                                )

                                val isMore = uiState.activeModule == StudioModule.MORE
                                NavigationBarItem(
                                    selected = isMore,
                                    onClick = { studioViewModel.selectModule(StudioModule.MORE) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isMore) {
                                                Icons.Filled.MoreHoriz
                                            } else {
                                                Icons.Outlined.MoreHoriz
                                            },
                                            contentDescription = "More"
                                        )
                                    },
                                    label = { Text("More") },
                                    modifier = Modifier.testTag("nav_more")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isRecordingModeActive) PaddingValues(0.dp) else innerPadding)
                    ) {
                        Crossfade(
                            targetState = uiState.activeModule,
                            animationSpec = tween(200),
                            label = "moduleCrossfade"
                        ) { module ->
                            when (module) {
                                StudioModule.CODEPAD_IDE -> {
                                    CodePadStudioScreen(
                                        uiState = uiState,
                                        viewModel = studioViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                StudioModule.AUTO_WRITE_STUDIO -> {
                                    AutoWriteStudioScreen(
                                        uiState = uiState,
                                        viewModel = studioViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                StudioModule.MORE -> {
                                    MoreStudioScreen(
                                        uiState = uiState,
                                        viewModel = studioViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                AppOnboardingGuideDialog(
                    isOpen = !isSplashVisible && uiState.isOnboardingOpen,
                    onDismiss = { markCompleted ->
                        studioViewModel.dismissOnboardingGuide(markCompleted)
                    }
                )
            }
        }
    }
}
