package com.arnix.codepad.studio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arnix.codepad.studio.engine.TypingSoundEngine
import com.arnix.codepad.studio.engine.TypingSoundProfile
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.SpaceGroteskFontFamily

/**
 * High-Fidelity Interactive App Onboarding & Guided Feature Walkthrough.
 * Teaches new creators how to use CodePad Studio step-by-step:
 * 1. Live IDE & Multi-Tab Web Editor
 * 2. Auto-Write Studio with Acoustic Mechanical Keyboard Typing Sounds
 * 3. Creator Branding, Watermarks & High-CTR Outro Cards
 * 4. Pro Gesture Shortcuts for Distraction-Free Video Capture
 */
@Composable
fun AppOnboardingGuideDialog(
    isOpen: Boolean,
    onDismiss: (markCompleted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    Dialog(
        onDismissRequest = { onDismiss(true) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090D16).copy(alpha = 0.96f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("onboarding_guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Top Action Bar: Step Counter & Skip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Step Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Text(
                                text = "STEP ${currentStep + 1} OF $totalSteps",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // Skip Button
                    IconButton(
                        onClick = { onDismiss(true) },
                        modifier = Modifier.testTag("onboarding_btn_skip")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Skip Tour",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Indicator Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalSteps) {
                        val isCurrent = i == currentStep
                        val isDone = i < currentStep
                        val barColor by animateColorAsState(
                            targetValue = when {
                                isCurrent -> Color(0xFF38BDF8)
                                isDone -> Color(0xFF6366F1)
                                else -> Color(0xFF1E293B)
                            },
                            animationSpec = tween(300),
                            label = "barColor"
                        )
                        val barWeight by animateFloatAsState(
                            targetValue = if (isCurrent) 2f else 1f,
                            animationSpec = tween(300),
                            label = "barWeight"
                        )

                        Box(
                            modifier = Modifier
                                .weight(barWeight)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Step Content Slide Carousel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { -it / 2 } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { it / 2 } + fadeOut()
                                )
                            }
                        },
                        label = "onboardingStepTransition"
                    ) { step ->
                        when (step) {
                            0 -> OnboardingStepEditor()
                            1 -> OnboardingStepAutoWrite()
                            2 -> OnboardingStepBrandingAndOutro()
                            else -> OnboardingStepGestures()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Navigation Row: Back & Next/Finish Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("onboarding_btn_back"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE2E8F0)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Previous",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                onDismiss(true)
                            }
                        },
                        modifier = Modifier
                            .weight(if (currentStep > 0) 1.5f else 1f)
                            .height(48.dp)
                            .testTag("onboarding_btn_next"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        )
                    ) {
                        if (currentStep == totalSteps - 1) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Creating",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Continue",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 1: Live Code Studio
 */
@Composable
private fun OnboardingStepEditor() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
            border = BorderStroke(1.dp, Color(0xFF223253))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pill header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TAB 1: LIVE CODEPAD IDE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Write & Test Live Web Code",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }

                Text(
                    text = "Seamlessly switch between index.html, style.css, and script.js tabs with live syntax highlighting and instant on-device execution.",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF94A3B8)
                )

                // Mock Code Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0D1424))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MockTabPill("index.html", isActive = true, activeColor = Color(0xFFF97316))
                    MockTabPill("style.css", isActive = false, activeColor = Color(0xFF38BDF8))
                    MockTabPill("script.js", isActive = false, activeColor = Color(0xFFFACC15))
                }

                // Interactive Mini Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "LIVE NATIVE PREVIEW",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                            Text(
                                text = "0 errors • 60 FPS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161F30))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "<div class=\"portfolio-card\">\n  <h1>Aarav Sharma</h1>\n  <button>Say Hello 👋</button>\n</div>",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Feature Highlights
        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Split Live Native Preview",
            desc = "Inspect your layout rendered instantly via native Compose DOM without needing network connectivity."
        )
        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Interactive Developer Console",
            desc = "Trace console.log(), warnings, runtime variable mutations, and error logs with filter tabs."
        )
        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "One-Tap Standalone Export",
            desc = "Export your code as a bundled offline HTML file, separate CSS/JS assets, or backup JSON."
        )
    }
}

/**
 * Step 2: Auto-Write Studio with Sound
 */
@Composable
private fun OnboardingStepAutoWrite() {
    val scrollState = rememberScrollState()
    var isSoundTestTriggered by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
            border = BorderStroke(1.dp, Color(0xFF223253))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TAB 2: AUTO-WRITE & REC STUDIO",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Record Viral Shorts & Reels",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }

                Text(
                    text = "Transform your code into hypnotic coding videos! The engine types your code character-by-character with synthesized mechanical keyboard acoustics.",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF94A3B8)
                )

                // Interactive Mechanical Sound Tester
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isSoundTestTriggered = true
                                TypingSoundEngine.playKeyTick(
                                    profile = TypingSoundProfile.MECH_THOCK,
                                    typedChar = 'K',
                                    isManualTest = true
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Test Audio",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Tap to Test Thock Sound 🔊",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "16-bit PCM Realtime Acoustic Synthesis",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "PLAY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Target Duration Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DurationBadge("15s Blitz", isRecommended = false)
                    DurationBadge("30s Reel", isRecommended = false)
                    DurationBadge("60s Shorts", isRecommended = true)
                }
            }
        }

        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Acoustic Keyboard Profiles",
            desc = "Choose between Deep Mech Thock, Crisp Click, or Soft ASMR with customizable volume and pitch variation."
        )
        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Auto-Duration Optimizer",
            desc = "Select 15s, 30s, or 60s targets. The optimizer calculates punctuation weightings and auto-paces typing precisely."
        )
        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Auto-Chain Multi-Tabs",
            desc = "Automatically sequence HTML -> CSS -> JS tabs in one continuous cinematic take."
        )
    }
}

/**
 * Step 3: Creator Branding & Outros
 */
@Composable
private fun OnboardingStepBrandingAndOutro() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
            border = BorderStroke(1.dp, Color(0xFF223253))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFA855F7).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CUSTOMIZATION & BRANDING",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC084FC),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Watermark & Outro CTA Cards",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }

                Text(
                    text = "Brand your videos professionally. Display your social media handle, add viral hook cards to hook viewers, and trigger high-converting outro animations.",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF94A3B8)
                )

                // Mock Creator Watermark & Viral Hook Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Viral Hook Mock
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF090D16),
                            border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🔥 HTML • CSS • JS",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Build a Portfolio Card",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF8FAFC)
                                )
                            }
                        }

                        // Outro Subscribe Mock
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1B4B),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "@codewitharnix",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDC2626)
                                ) {
                                    Text(
                                        text = "SUBSCRIBE",
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Creator Watermark Handle",
            desc = "Place your @handle in the window footer, stage top, or overlay pill with customizable opacity and styling."
        )
        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Viral Hook Headers",
            desc = "Add neon badges, titles, and tags that grab viewer attention during the first 3 critical seconds of the video."
        )
        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "End CTA Outro Animations",
            desc = "Display animated Subscribe, Like & Comment prompts to dramatically boost retention and channel growth."
        )
    }
}

/**
 * Step 4: Pro Gestures & Studio Controls
 */
@Composable
private fun OnboardingStepGestures() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C31)),
            border = BorderStroke(1.dp, Color(0xFF223253))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PRO GESTURE SHORTCUTS",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Distraction-Free Video Capture",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                }

                Text(
                    text = "Use these master gesture controls during screen recording for the cleanest possible videos without touching annoying buttons on screen:",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF94A3B8)
                )

                // Gesture Cards Grid
                GestureShortcutRow(
                    gesture = "SINGLE TAP",
                    badgeColor = Color(0xFF38BDF8),
                    actionTitle = "Play / Pause Typewriter",
                    actionDesc = "Tap code area anytime to freeze or resume auto-writing."
                )
                GestureShortcutRow(
                    gesture = "DOUBLE TAP",
                    badgeColor = Color(0xFFA855F7),
                    actionTitle = "Hide / Show Controls",
                    actionDesc = "Instantly clears all buttons & bottom bar for 100% clean video."
                )
                GestureShortcutRow(
                    gesture = "TRIPLE TAP",
                    badgeColor = Color(0xFFF59E0B),
                    actionTitle = "Exit Stage Recording",
                    actionDesc = "Exits full-stage recording mode safely back to CodePad IDE."
                )
            }
        }

        FeatureHighlightItem(
            icon = Icons.Default.CheckCircle,
            title = "Replay Tour Any Time",
            desc = "Need a refresher later? Tap the Help Tour icon in the top header or in Tab 3 (More Studio) anytime!"
        )
    }
}

@Composable
private fun MockTabPill(
    name: String,
    isActive: Boolean,
    activeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) Color(0xFF1E293B) else Color.Transparent,
        border = if (isActive) BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else Color(0xFF64748B))
            )
            Text(
                text = name,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) Color(0xFFF8FAFC) else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun DurationBadge(
    text: String,
    isRecommended: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isRecommended) Color(0xFF6366F1).copy(alpha = 0.25f) else Color(0xFF1E293B),
        border = BorderStroke(
            1.dp,
            if (isRecommended) Color(0xFF818CF8) else Color(0xFF334155)
        )
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isRecommended) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun FeatureHighlightItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111827).copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
            Text(
                text = desc,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun GestureShortcutRow(
    gesture: String,
    badgeColor: Color,
    actionTitle: String,
    actionDesc: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = gesture,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = actionTitle,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                )
                Text(
                    text = actionDesc,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
