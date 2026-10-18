package com.arnix.codepad.studio.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.codepad.studio.data.ProjectPreset
import com.arnix.codepad.studio.data.ProjectTemplates
import com.arnix.codepad.studio.engine.TypingSoundProfile
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.LocalEditorPalette
import com.arnix.codepad.studio.ui.theme.LocalUiFontScale
import com.arnix.codepad.studio.ui.theme.uiSp
import kotlin.math.roundToInt
import com.arnix.codepad.studio.viewmodel.AutoDurationTarget
import com.arnix.codepad.studio.viewmodel.CTA_PLATFORM_QUICK_PRESETS
import com.arnix.codepad.studio.viewmodel.CreatorAccentPalette
import com.arnix.codepad.studio.viewmodel.CtaDisplayTiming
import com.arnix.codepad.studio.viewmodel.CtaPlatformQuickPreset
import com.arnix.codepad.studio.viewmodel.EndCtaPosition
import com.arnix.codepad.studio.viewmodel.EndCtaStyle
import com.arnix.codepad.studio.viewmodel.OverlaySizeScale
import com.arnix.codepad.studio.viewmodel.ShortsBackdropPreset
import com.arnix.codepad.studio.viewmodel.TypingSequenceMetrics
import com.arnix.codepad.studio.viewmodel.VIRAL_HOOK_QUICK_PRESETS
import com.arnix.codepad.studio.viewmodel.ViralHookAlignment
import com.arnix.codepad.studio.viewmodel.ViralHookPosition
import com.arnix.codepad.studio.viewmodel.ViralHookQuickPreset
import com.arnix.codepad.studio.viewmodel.ViralHookStyle
import com.arnix.codepad.studio.viewmodel.WatermarkPosition
import com.arnix.codepad.studio.viewmodel.WatermarkStyle
import kotlinx.coroutines.delay

/**
 * Multi-Style Creator Handle Badge supporting 5 distinct styles and overlay/banner modes.
 */
@Composable
fun CreatorWatermarkBadge(
    handle: String,
    style: WatermarkStyle,
    isOverlay: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    opacity: Float = 1.0f,
    sizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD
) {
    val palette = LocalEditorPalette.current
    val scale = sizeScale.scaleFactor
    val baseOverlayAlpha = if (isOverlay) 0.42f else 1.0f
    val overlayAlpha = (baseOverlayAlpha * opacity).coerceIn(0.1f, 1.0f)

    when (style) {
        WatermarkStyle.PILL_BADGE -> {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = overlayAlpha),
                tonalElevation = if (isOverlay) 0.dp else 2.dp,
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = ((if (isOverlay) 14 else 9) * scale).dp,
                        vertical = ((if (isOverlay) 6 else 3) * scale).dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((5 * scale).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = overlayAlpha),
                        modifier = Modifier.size(((if (isOverlay) 16 else 12) * scale).dp)
                    )
                    Text(
                        text = handle,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = ((if (isOverlay) 13 else 9) * scale).uiSp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = overlayAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WatermarkStyle.GLASS_OUTLINE -> {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(50),
                color = Color(0xFF0F172A).copy(alpha = if (isOverlay) 0.35f else 0.78f),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF38BDF8).copy(alpha = if (isOverlay) 0.45f else 0.8f)
                ),
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isOverlay) 14.dp else 10.dp,
                        vertical = if (isOverlay) 6.dp else 3.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8).copy(alpha = overlayAlpha),
                        modifier = Modifier.size(if (isOverlay) 15.dp else 11.dp)
                    )
                    Text(
                        text = handle,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isOverlay) 13.uiSp else 9.uiSp,
                        color = Color.White.copy(alpha = if (isOverlay) 0.65f else 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WatermarkStyle.CREATOR_GRADIENT -> {
            val gradientBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF6366F1).copy(alpha = if (isOverlay) 0.45f else 0.95f),
                    Color(0xFFEC4899).copy(alpha = if (isOverlay) 0.45f else 0.95f)
                )
            )
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(50))
                    .background(gradientBrush)
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = if (isOverlay) 16.dp else 10.dp,
                        vertical = if (isOverlay) 6.dp else 3.dp
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (isOverlay) 0.75f else 1f),
                        modifier = Modifier.size(if (isOverlay) 15.dp else 12.dp)
                    )
                    Text(
                        text = handle,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isOverlay) 13.uiSp else 9.uiSp,
                        color = Color.White.copy(alpha = if (isOverlay) 0.75f else 1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WatermarkStyle.CYBER_TERMINAL -> {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF090D16).copy(alpha = if (isOverlay) 0.42f else 0.88f),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF10B981).copy(alpha = if (isOverlay) 0.48f else 0.85f)
                ),
                modifier = modifier
            ) {
                Text(
                    text = ">_ $handle",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isOverlay) 12.5.uiSp else 9.uiSp,
                    color = Color(0xFF10B981).copy(alpha = if (isOverlay) 0.75f else 0.98f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = if (isOverlay) 12.dp else 8.dp,
                        vertical = if (isOverlay) 5.dp else 3.dp
                    )
                )
            }
        }

        WatermarkStyle.MINIMAL_MONO -> {
            Text(
                text = handle,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (isOverlay) 17.uiSp else 9.5.uiSp,
                color = palette.plainText.copy(alpha = if (isOverlay) 0.24f else 0.78f),
                modifier = modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Fully Customizable Animated Like ❤️ • Comment 💬 • Subscribe 🔔 Bar & Outro Overlay.
 * Supports:
 * - 6 Visual Styles (Subscribe Card, Floating Pill Bar, Transparent Floating Bar, Floating Action Chips, Vertical Side Rail, Cyber Neon Bar)
 * - Dedicated Transparent Background toggle (`transparentBg`) on ANY style
 * - Border outline toggle (`showBorder`), Handle visibility toggle (`showHandle`)
 * - 6 Accent Color Palettes & 3 Size Scales (Compact, Standard, Bold Large)
 * - Custom button labels (Like/Follow/Subscribe counts & text)
 */
@Composable
fun CreatorEndCtaAnimationOverlay(
    creatorHandle: String,
    ctaMessage: String,
    style: EndCtaStyle,
    transparentBg: Boolean = false,
    showBorder: Boolean = true,
    showHandle: Boolean = true,
    accentIndex: Int = 1,
    sizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD,
    showLike: Boolean = true,
    showComment: Boolean = true,
    showSubscribe: Boolean = true,
    likeLabel: String = "Like",
    commentLabel: String = "Comment",
    subscribeLabel: String = "SUBSCRIBE",
    subscribedLabel: String = "SUBSCRIBED ✓",
    loopAnimation: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sequential animation stage: 0 = initial, 1 = Liked, 2 = Commented, 3 = Subscribed!
    var animStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(loopAnimation) {
        while (true) {
            animStep = 0
            delay(220L)
            animStep = 1 // Auto-Like pulse
            delay(380L)
            animStep = 2 // Auto-Comment pop
            delay(420L)
            animStep = 3 // Auto-Subscribe activation
            if (!loopAnimation) break
            delay(3800L)
        }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (animStep == 1) 1.26f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "likeScale"
    )
    val commentScale by animateFloatAsState(
        targetValue = if (animStep == 2) 1.22f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "commentScale"
    )
    val subScale by animateFloatAsState(
        targetValue = if (animStep >= 3) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "subScale"
    )

    val isLiked = animStep >= 1
    val isCommentActive = animStep >= 2
    val isSubscribed = animStep >= 3

    val accent = CreatorAccentPalette.fromIndex(accentIndex)
    val primaryAccent = Color(accent.primaryHex)
    val secondaryAccent = Color(accent.secondaryHex)
    val successAccent = Color(accent.successHex)
    val scale = sizeScale.scaleFactor * LocalUiFontScale.current * 0.90f

    val isEffectiveTransparent = transparentBg ||
        style == EndCtaStyle.TRANSPARENT_BAR ||
        style == EndCtaStyle.MINIMAL_CHIPS

    val textShadowStyle = if (isEffectiveTransparent) {
        TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.85f),
                offset = Offset(0f, 2f),
                blurRadius = 6f
            )
        )
    } else {
        TextStyle.Default
    }

    val safeLikeText = likeLabel.ifBlank { "Like" }
    val safeCommentText = commentLabel.ifBlank { "Comment" }
    val safeSubText = subscribeLabel.ifBlank { "SUBSCRIBE" }
    val safeSubbedText = subscribedLabel.ifBlank { "SUBSCRIBED ✓" }

    when (style) {
        EndCtaStyle.SUBSCRIBE_CARD -> {
            val containerBg = if (isEffectiveTransparent) {
                Color.Transparent
            } else {
                Color(0xFF0F172A).copy(alpha = 0.92f)
            }
            val borderStroke = if (showBorder) {
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(primaryAccent.copy(alpha = 0.7f), secondaryAccent.copy(alpha = 0.7f))
                    )
                )
            } else {
                null
            }

            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape((16 * scale).dp),
                color = containerBg,
                border = borderStroke,
                shadowElevation = if (isEffectiveTransparent) 0.dp else 8.dp,
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("end_cta_animation_overlay")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = (14 * scale).dp,
                            vertical = (11 * scale).dp
                        ),
                    verticalArrangement = Arrangement.spacedBy((9 * scale).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (showHandle || ctaMessage.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (showHandle) {
                                    Text(
                                        text = creatorHandle,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (13 * scale).sp,
                                        color = Color.White,
                                        style = textShadowStyle
                                    )
                                }
                                if (ctaMessage.isNotBlank()) {
                                    Text(
                                        text = ctaMessage,
                                        fontSize = (11 * scale).sp,
                                        color = if (isEffectiveTransparent) Color.White else Color(0xFFCBD5E1),
                                        style = textShadowStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (showSubscribe) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSubscribed) successAccent else primaryAccent,
                                modifier = Modifier.scale(subScale)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = (12 * scale).dp,
                                        vertical = (6 * scale).dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSubscribed) {
                                            Icons.Default.NotificationsActive
                                        } else {
                                            Icons.Outlined.NotificationsNone
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size((14 * scale).dp)
                                    )
                                    Text(
                                        text = if (isSubscribed) safeSubbedText else safeSubText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (11 * scale).sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (showLike || showComment) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((10 * scale).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showLike) {
                                CtaActionPillButton(
                                    icon = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    text = safeLikeText,
                                    isActive = isLiked,
                                    activeColor = secondaryAccent,
                                    isTransparentBg = isEffectiveTransparent,
                                    scale = scale,
                                    modifier = Modifier.scale(likeScale)
                                )
                            }

                            if (showComment) {
                                CtaActionPillButton(
                                    icon = Icons.Outlined.ChatBubbleOutline,
                                    text = safeCommentText,
                                    isActive = isCommentActive,
                                    activeColor = primaryAccent,
                                    isTransparentBg = isEffectiveTransparent,
                                    scale = scale,
                                    modifier = Modifier.scale(commentScale)
                                )
                            }
                        }
                    }
                }
            }
        }

        EndCtaStyle.BOTTOM_BAR,
        EndCtaStyle.TRANSPARENT_BAR,
        EndCtaStyle.CYBER_NEON_BAR -> {
            val isCyber = style == EndCtaStyle.CYBER_NEON_BAR
            val shape = if (isCyber) RoundedCornerShape((10 * scale).dp) else RoundedCornerShape(50)
            val containerBg = when {
                isEffectiveTransparent -> Color.Transparent
                isCyber -> Color(0xFF060913).copy(alpha = 0.92f)
                else -> Color(0xFF0F172A).copy(alpha = 0.90f)
            }
            val borderStroke = if (showBorder) {
                if (isCyber) {
                    BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(primaryAccent, secondaryAccent))
                    )
                } else {
                    BorderStroke(1.dp, primaryAccent.copy(alpha = if (isEffectiveTransparent) 0.65f else 0.5f))
                }
            } else {
                null
            }

            Surface(
                onClick = onDismiss,
                shape = shape,
                color = containerBg,
                border = borderStroke,
                shadowElevation = if (isEffectiveTransparent) 0.dp else 6.dp,
                modifier = modifier.testTag("end_cta_animation_overlay")
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = (14 * scale).dp,
                        vertical = (8 * scale).dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)
                ) {
                    if (showHandle) {
                        Text(
                            text = if (isCyber) ">_ $creatorHandle" else creatorHandle,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (11 * scale).sp,
                            color = if (isCyber) primaryAccent else Color.White,
                            style = textShadowStyle
                        )
                    }

                    if (showLike) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.scale(likeScale)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isLiked) secondaryAccent else Color.White,
                                modifier = Modifier.size((15 * scale).dp)
                            )
                            Text(
                                text = safeLikeText,
                                fontSize = (11 * scale).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLiked) secondaryAccent else Color.White,
                                style = textShadowStyle
                            )
                        }
                    }

                    if (showComment) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.scale(commentScale)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (isCommentActive) primaryAccent else Color.White,
                                modifier = Modifier.size((14 * scale).dp)
                            )
                            Text(
                                text = safeCommentText,
                                fontSize = (11 * scale).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCommentActive) primaryAccent else Color.White,
                                style = textShadowStyle
                            )
                        }
                    }

                    if (showSubscribe) {
                        Surface(
                            shape = if (isCyber) RoundedCornerShape(6.dp) else RoundedCornerShape(50),
                            color = if (isSubscribed) successAccent else primaryAccent,
                            modifier = Modifier.scale(subScale)
                        ) {
                            Text(
                                text = if (isSubscribed) safeSubbedText else safeSubText,
                                fontSize = (10 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (accent == CreatorAccentPalette.MONOCHROME_PRO && !isSubscribed) {
                                    Color(0xFF0F172A)
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.padding(
                                    horizontal = (9 * scale).dp,
                                    vertical = (4 * scale).dp
                                )
                            )
                        }
                    }
                }
            }
        }

        EndCtaStyle.MINIMAL_CHIPS -> {
            // 100% Transparent outer container with floating individual pills!
            Row(
                modifier = modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("end_cta_animation_overlay"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                if (showHandle) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (transparentBg) Color.Transparent else Color(0xFF0F172A).copy(alpha = 0.72f),
                        border = if (showBorder) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null
                    ) {
                        Text(
                            text = creatorHandle,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (11 * scale).sp,
                            color = Color.White,
                            style = textShadowStyle,
                            modifier = Modifier.padding(horizontal = (10 * scale).dp, vertical = (5 * scale).dp)
                        )
                    }
                }

                if (showLike) {
                    CtaActionPillButton(
                        icon = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        text = safeLikeText,
                        isActive = isLiked,
                        activeColor = secondaryAccent,
                        isTransparentBg = transparentBg,
                        scale = scale,
                        modifier = Modifier.scale(likeScale)
                    )
                }

                if (showComment) {
                    CtaActionPillButton(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        text = safeCommentText,
                        isActive = isCommentActive,
                        activeColor = primaryAccent,
                        isTransparentBg = transparentBg,
                        scale = scale,
                        modifier = Modifier.scale(commentScale)
                    )
                }

                if (showSubscribe) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSubscribed) successAccent else primaryAccent,
                        modifier = Modifier.scale(subScale)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = (11 * scale).dp,
                                vertical = (5 * scale).dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSubscribed) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsNone
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size((13 * scale).dp)
                            )
                            Text(
                                text = if (isSubscribed) safeSubbedText else safeSubText,
                                fontSize = (10 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        EndCtaStyle.SIDE_RAIL -> {
            val railBg = if (isEffectiveTransparent) {
                Color.Transparent
            } else {
                Color(0xFF0F172A).copy(alpha = 0.88f)
            }
            val railBorderMod = if (showBorder) {
                Modifier.border(
                    width = 1.dp,
                    color = primaryAccent.copy(alpha = if (isEffectiveTransparent) 0.55f else 0.28f),
                    shape = RoundedCornerShape((20 * scale).dp)
                )
            } else {
                Modifier
            }

            Column(
                modifier = modifier
                    .clip(RoundedCornerShape((20 * scale).dp))
                    .background(railBg)
                    .then(railBorderMod)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = (10 * scale).dp, vertical = (12 * scale).dp)
                    .testTag("end_cta_animation_overlay"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
            ) {
                if (showLike) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(likeScale)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) secondaryAccent else Color.White,
                            modifier = Modifier.size((22 * scale).dp)
                        )
                        Text(
                            text = safeLikeText,
                            fontSize = (9 * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            style = textShadowStyle
                        )
                    }
                }

                if (showComment) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(commentScale)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = if (isCommentActive) primaryAccent else Color.White,
                            modifier = Modifier.size((20 * scale).dp)
                        )
                        Text(
                            text = safeCommentText,
                            fontSize = (9 * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            style = textShadowStyle
                        )
                    }
                }

                if (showSubscribe) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(subScale)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((28 * scale).dp)
                                .clip(CircleShape)
                                .background(if (isSubscribed) successAccent else primaryAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSubscribed) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsNone
                                },
                                contentDescription = "Subscribe",
                                tint = Color.White,
                                modifier = Modifier.size((16 * scale).dp)
                            )
                        }
                        Text(
                            text = if (isSubscribed) safeSubbedText else safeSubText,
                            fontSize = (9 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = textShadowStyle
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CtaActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isActive: Boolean,
    activeColor: Color,
    isTransparentBg: Boolean,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = when {
            isTransparentBg && isActive -> activeColor.copy(alpha = 0.25f)
            isTransparentBg -> Color.Black.copy(alpha = 0.25f)
            isActive -> activeColor.copy(alpha = 0.22f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor else Color.White.copy(alpha = 0.32f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = (10 * scale).dp,
                vertical = (4 * scale).dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size((14 * scale).dp)
            )
            Text(
                text = text,
                fontSize = (11 * scale).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) activeColor else Color.White,
                style = if (isTransparentBg) {
                    TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 5f
                        )
                    )
                } else {
                    TextStyle.Default
                }
            )
        }
    }
}

/**
 * Fully Customizable Zero-Editing Viral Hook Title Banner.
 * Supports:
 * - 6 Visual Styles (Neon Pill Header, Glass Hook Card, Transparent Floating, Cyber Terminal Box, Gradient Ribbon, Clean Minimal)
 * - Dedicated Transparent Background toggle (`transparentBg`) on ANY style
 * - Border outline toggle (`showBorder`) & Badge visibility toggle (`showBadge`)
 * - 3 Alignment modes (Left Align, Centered, Badge Left • Title Right)
 * - 6 Accent Color Palettes & 3 Size Scales (Compact, Standard, Bold Large)
 */
@Composable
fun ViralHookHeaderBanner(
    badgeText: String,
    titleText: String,
    style: ViralHookStyle,
    alignment: ViralHookAlignment = ViralHookAlignment.START,
    transparentBg: Boolean = false,
    showBorder: Boolean = true,
    showBadge: Boolean = true,
    accentIndex: Int = 0,
    sizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = CreatorAccentPalette.fromIndex(accentIndex)
    val primaryColor = Color(accent.primaryHex)
    val secondaryColor = Color(accent.secondaryHex)
    val scale = sizeScale.scaleFactor * LocalUiFontScale.current * 0.90f

    val isEffectiveTransparent = transparentBg || style == ViralHookStyle.TRANSPARENT_CLEAN
    val effectiveBadge = if (showBadge) badgeText else ""

    val textShadowStyle = if (isEffectiveTransparent) {
        TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.85f),
                offset = Offset(0f, 2f),
                blurRadius = 6f
            )
        )
    } else {
        TextStyle.Default
    }

    when (style) {
        ViralHookStyle.NEON_PILL,
        ViralHookStyle.TRANSPARENT_CLEAN -> {
            val containerBg = if (isEffectiveTransparent) {
                Color.Transparent
            } else {
                Color(0xFF0F172A).copy(alpha = 0.88f)
            }
            val borderStroke = if (showBorder) {
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                )
            } else {
                null
            }

            Surface(
                onClick = onClick,
                shape = RoundedCornerShape((14 * scale).dp),
                color = containerBg,
                border = borderStroke,
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("viral_hook_banner")
            ) {
                HookRowContent(
                    badgeText = effectiveBadge,
                    titleText = titleText,
                    alignment = alignment,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    scale = scale,
                    textShadowStyle = textShadowStyle
                )
            }
        }

        ViralHookStyle.GLASS_BANNER -> {
            val containerBg = if (isEffectiveTransparent) {
                Color.Transparent
            } else {
                Color(0xFF090D16).copy(alpha = 0.82f)
            }
            val borderStroke = if (showBorder) {
                BorderStroke(1.dp, primaryColor.copy(alpha = 0.38f))
            } else {
                null
            }
            val horizAlignment = when (alignment) {
                ViralHookAlignment.START -> Alignment.Start
                ViralHookAlignment.CENTER -> Alignment.CenterHorizontally
                ViralHookAlignment.SPACE_BETWEEN -> Alignment.CenterHorizontally
            }

            Surface(
                onClick = onClick,
                shape = RoundedCornerShape((14 * scale).dp),
                color = containerBg,
                border = borderStroke,
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("viral_hook_banner")
            ) {
                if (alignment == ViralHookAlignment.SPACE_BETWEEN && effectiveBadge.isNotBlank()) {
                    HookRowContent(
                        badgeText = effectiveBadge,
                        titleText = titleText,
                        alignment = alignment,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        scale = scale,
                        textShadowStyle = textShadowStyle
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = (14 * scale).dp, vertical = (7 * scale).dp),
                        horizontalAlignment = horizAlignment,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (effectiveBadge.isNotBlank()) {
                            Text(
                                text = effectiveBadge.uppercase(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (10 * scale).sp,
                                color = primaryColor,
                                style = textShadowStyle,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleSmall.merge(textShadowStyle),
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scale).sp,
                            color = Color.White,
                            textAlign = if (alignment == ViralHookAlignment.START) TextAlign.Start else TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        ViralHookStyle.CYBER_TERMINAL -> {
            val containerBg = if (isEffectiveTransparent) {
                Color.Transparent
            } else {
                Color(0xFF050811).copy(alpha = 0.92f)
            }
            val borderStroke = if (showBorder) {
                BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.85f))
            } else {
                null
            }

            Surface(
                onClick = onClick,
                shape = RoundedCornerShape((8 * scale).dp),
                color = containerBg,
                border = borderStroke,
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("viral_hook_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (12 * scale).dp, vertical = (7 * scale).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = when (alignment) {
                        ViralHookAlignment.START -> Arrangement.Start
                        ViralHookAlignment.CENTER -> Arrangement.Center
                        ViralHookAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
                    }
                ) {
                    if (effectiveBadge.isNotBlank()) {
                        Text(
                            text = "[ $effectiveBadge ]",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10 * scale).sp,
                            color = secondaryColor,
                            style = textShadowStyle,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = ">_ $titleText",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (12 * scale).sp,
                        color = primaryColor,
                        style = textShadowStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        ViralHookStyle.GRADIENT_RIBBON -> {
            val bgModifier = if (isEffectiveTransparent) {
                Modifier.background(Color.Transparent)
            } else {
                Modifier.background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.92f),
                            secondaryColor.copy(alpha = 0.92f)
                        )
                    )
                )
            }
            val borderMod = if (showBorder) {
                Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.38f),
                    shape = RoundedCornerShape((14 * scale).dp)
                )
            } else {
                Modifier
            }

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((14 * scale).dp))
                    .then(bgModifier)
                    .then(borderMod)
                    .clickable(onClick = onClick)
                    .testTag("viral_hook_banner")
            ) {
                HookRowContent(
                    badgeText = effectiveBadge,
                    titleText = titleText,
                    alignment = alignment,
                    primaryColor = if (isEffectiveTransparent) primaryColor else Color(0xFF0F172A),
                    secondaryColor = if (isEffectiveTransparent) secondaryColor else Color(0xFF1E293B),
                    scale = scale,
                    textShadowStyle = textShadowStyle
                )
            }
        }

        ViralHookStyle.MINIMAL_HEADER -> {
            val rowArrangement = when (alignment) {
                ViralHookAlignment.START -> Arrangement.Start
                ViralHookAlignment.CENTER -> Arrangement.Center
                ViralHookAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
            }
            val borderMod = if (showBorder) {
                Modifier.border(
                    width = 1.dp,
                    color = primaryColor.copy(alpha = if (isEffectiveTransparent) 0.55f else 0.35f),
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                Modifier
            }

            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isEffectiveTransparent) Color.Transparent else Color(0xFF0F172A).copy(alpha = 0.45f)
                    )
                    .then(borderMod)
                    .clickable(onClick = onClick)
                    .padding(horizontal = (10 * scale).dp, vertical = (5 * scale).dp)
                    .testTag("viral_hook_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = rowArrangement
            ) {
                if (alignment == ViralHookAlignment.SPACE_BETWEEN && effectiveBadge.isNotBlank()) {
                    Text(
                        text = effectiveBadge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (11 * scale).sp,
                        color = primaryColor,
                        style = textShadowStyle,
                        maxLines = 1
                    )
                    Text(
                        text = titleText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (12 * scale).sp,
                        color = Color.White,
                        style = textShadowStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = if (effectiveBadge.isNotBlank()) "$effectiveBadge  •  $titleText" else titleText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (12 * scale).sp,
                        color = if (isEffectiveTransparent) Color.White else MaterialTheme.colorScheme.onBackground,
                        style = textShadowStyle,
                        textAlign = if (alignment == ViralHookAlignment.START) TextAlign.Start else TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HookRowContent(
    badgeText: String,
    titleText: String,
    alignment: ViralHookAlignment,
    primaryColor: Color,
    secondaryColor: Color,
    scale: Float,
    textShadowStyle: TextStyle
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (12 * scale).dp, vertical = (8 * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (alignment) {
            ViralHookAlignment.START -> Arrangement.spacedBy((10 * scale).dp)
            ViralHookAlignment.CENTER -> Arrangement.spacedBy((10 * scale).dp, Alignment.CenterHorizontally)
            ViralHookAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
        }
    ) {
        if (badgeText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    )
                    .padding(horizontal = (9 * scale).dp, vertical = (3 * scale).dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * scale).sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        val titleModifier = when (alignment) {
            ViralHookAlignment.START -> Modifier.weight(1f)
            ViralHookAlignment.CENTER -> Modifier
            ViralHookAlignment.SPACE_BETWEEN -> Modifier
        }

        Text(
            text = titleText,
            style = MaterialTheme.typography.titleSmall.merge(textShadowStyle),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (13 * scale).sp,
            color = Color.White,
            textAlign = when (alignment) {
                ViralHookAlignment.START -> TextAlign.Start
                ViralHookAlignment.CENTER -> TextAlign.Center
                ViralHookAlignment.SPACE_BETWEEN -> TextAlign.End
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = titleModifier
        )
    }
}

/**
 * Sleek 3-2-1 Pre-Recording Countdown Overlay so creators can start their screen recorder cleanly.
 * Automatically disappears at 0 before the first character is typed.
 */
@Composable
fun RecordingCountdownOverlay(
    secondsLeft: Int,
    isStartingDelay: Boolean = false,
    onSkipNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isStartingDelay) Color(0xFFEF4444) else Color(0xFF38BDF8)
    val headerColor = if (isStartingDelay) Color(0xFFEF4444) else Color(0xFF38BDF8)

    Surface(
        onClick = onSkipNow,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, borderColor),
        shadowElevation = 12.dp,
        modifier = modifier.testTag("recording_countdown_overlay")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isStartingDelay) "● REC READY" else "GET READY TO RECORD",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = headerColor
            )
            Text(
                text = if (isStartingDelay) "STARTING..." else secondsLeft.toString(),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isStartingDelay) 24.sp else 44.sp,
                color = Color.White
            )
            Text(
                text = if (isStartingDelay) "Breathing pause... typing begins now" else "Tap anywhere to start immediately",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * 1-Tap Ready-Made Viral Web Dev Shorts Templates Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViralTemplatesQuickSheet(
    currentProjectName: String,
    onSelectPreset: (ProjectPreset) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ready-Made Viral Web Dev Templates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "1-tap load complete HTML + CSS + JS projects with pre-written Viral Hook Titles for instant Shorts recording.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProjectTemplates.PRESETS.forEach { preset ->
                val isCurrent = preset.name == currentProjectName
                val totalChars = preset.html.length + preset.css.length + preset.js.length
                Surface(
                    onClick = { onSelectPreset(preset) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("viral_template_${preset.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.hookBadge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$totalChars chars • HTML+CSS+JS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Hook: \"${preset.hookTitle}\"",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioCustomizeSheet(
    creatorHandle: String,
    autoDurationTarget: AutoDurationTarget,
    sequenceMetrics: TypingSequenceMetrics,
    watermarkPosition: WatermarkPosition,
    watermarkStyle: WatermarkStyle,
    showViralHookBanner: Boolean,
    viralHookBadge: String,
    viralHookTitle: String,
    viralHookStyle: ViralHookStyle,
    viralHookPosition: ViralHookPosition = ViralHookPosition.TOP_STAGE,
    viralHookAlignment: ViralHookAlignment = ViralHookAlignment.START,
    viralHookTransparentBg: Boolean = false,
    viralHookShowBorder: Boolean = true,
    viralHookShowBadge: Boolean = true,
    viralHookAccentIndex: Int = 0,
    viralHookSizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD,
    typingSoundProfile: TypingSoundProfile,
    useRecordingCountdown: Boolean,
    keepPreviewOpenInRecord: Boolean,
    showEndCtaAnimation: Boolean,
    endCtaStyle: EndCtaStyle,
    endCtaPosition: EndCtaPosition = EndCtaPosition.CODE_BOTTOM_OVERLAY,
    ctaDisplayTiming: CtaDisplayTiming = CtaDisplayTiming.AT_END_100,
    ctaTransparentBg: Boolean = false,
    ctaShowBorder: Boolean = true,
    ctaShowHandle: Boolean = true,
    ctaAccentIndex: Int = 1,
    ctaSizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD,
    endCtaMessage: String,
    ctaShowLike: Boolean,
    ctaShowComment: Boolean,
    ctaShowSubscribe: Boolean,
    ctaLikeLabel: String = "Like",
    ctaCommentLabel: String = "Comment",
    ctaSubscribeLabel: String = "SUBSCRIBE",
    ctaSubscribedLabel: String = "SUBSCRIBED ✓",
    isAutoChainTabs: Boolean,
    autoRevealPreview: Boolean,
    selectedBackdropIndex: Int,
    onSelectAutoDurationTarget: (AutoDurationTarget) -> Unit,
    onUpdateHandle: (String) -> Unit,
    onSelectWatermarkPosition: (WatermarkPosition) -> Unit,
    onSelectWatermarkStyle: (WatermarkStyle) -> Unit,
    onToggleShowViralHookBanner: () -> Unit,
    onUpdateViralHookBadge: (String) -> Unit,
    onUpdateViralHookTitle: (String) -> Unit,
    onSelectViralHookStyle: (ViralHookStyle) -> Unit,
    onSelectViralHookPosition: (ViralHookPosition) -> Unit = {},
    onSelectViralHookAlignment: (ViralHookAlignment) -> Unit = {},
    onToggleViralHookTransparentBg: () -> Unit = {},
    onToggleViralHookShowBorder: () -> Unit = {},
    onToggleViralHookShowBadge: () -> Unit = {},
    onSelectViralHookAccentIndex: (Int) -> Unit = {},
    onSelectViralHookSizeScale: (OverlaySizeScale) -> Unit = {},
    onApplyViralHookQuickPreset: (ViralHookQuickPreset) -> Unit = {},
    onSelectTypingSoundProfile: (TypingSoundProfile) -> Unit,
    onToggleUseRecordingCountdown: () -> Unit,
    onToggleKeepPreviewOpenInRecord: () -> Unit,
    onToggleShowEndCta: () -> Unit,
    onSelectEndCtaStyle: (EndCtaStyle) -> Unit,
    onSelectEndCtaPosition: (EndCtaPosition) -> Unit = {},
    onSelectCtaDisplayTiming: (CtaDisplayTiming) -> Unit = {},
    onToggleCtaTransparentBg: () -> Unit = {},
    onToggleCtaShowBorder: () -> Unit = {},
    onToggleCtaShowHandle: () -> Unit = {},
    onSelectCtaAccentIndex: (Int) -> Unit = {},
    onSelectCtaSizeScale: (OverlaySizeScale) -> Unit = {},
    onUpdateEndCtaMessage: (String) -> Unit,
    onToggleCtaLike: () -> Unit,
    onToggleCtaComment: () -> Unit,
    onToggleCtaSubscribe: () -> Unit,
    onUpdateCtaLikeLabel: (String) -> Unit = {},
    onUpdateCtaCommentLabel: (String) -> Unit = {},
    onUpdateCtaSubscribeLabel: (String) -> Unit = {},
    onUpdateCtaSubscribedLabel: (String) -> Unit = {},
    onApplyCtaPlatformPreset: (CtaPlatformQuickPreset) -> Unit = {},
    onPreviewEndCta: () -> Unit,
    onToggleAutoChain: () -> Unit,
    onToggleAutoRevealPreview: () -> Unit,
    onSelectBackdrop: (Int) -> Unit,
    autoWriteFontSize: Float = 11f,
    editorFontSize: Float = 11.5f,
    uiFontScale: Float = 0.92f,
    onUpdateAutoWriteFontSize: (Float) -> Unit = {},
    onUpdateEditorFontSize: (Float) -> Unit = {},
    onUpdateUiFontScale: (Float) -> Unit = {},
    onApplyFontSizePreset: (editorSize: Float, autoWriteSize: Float, uiScale: Float) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Zero-Editing Creator Studio Setup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 0A. Typography & Font Sizes Control Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Font Size & UI Scale Controls",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = autoWriteFontSize <= 10f && uiFontScale <= 0.85f,
                            onClick = { onApplyFontSizePreset(10f, 9.5f, 0.82f) },
                            label = { Text("Extra Small") }
                        )
                        FilterChip(
                            selected = autoWriteFontSize in 10.5f..11.5f && uiFontScale in 0.88f..0.95f,
                            onClick = { onApplyFontSizePreset(11.5f, 11f, 0.92f) },
                            label = { Text("Compact (Default)") }
                        )
                        FilterChip(
                            selected = autoWriteFontSize in 12f..13.5f && uiFontScale in 0.96f..1.05f,
                            onClick = { onApplyFontSizePreset(13f, 12.5f, 1.0f) },
                            label = { Text("Medium") }
                        )
                        FilterChip(
                            selected = autoWriteFontSize >= 14f || uiFontScale >= 1.1f,
                            onClick = { onApplyFontSizePreset(15f, 14f, 1.12f) },
                            label = { Text("Large") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Write Code Font Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${autoWriteFontSize.roundToInt()} px",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.uiSp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = autoWriteFontSize,
                        onValueChange = onUpdateAutoWriteFontSize,
                        valueRange = 8f..22f,
                        steps = 13,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_autowrite_font_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App & Overlay UI Scale",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(uiFontScale * 100).roundToInt()}%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.uiSp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = uiFontScale,
                        onValueChange = onUpdateUiFontScale,
                        valueRange = 0.75f..1.30f,
                        steps = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_ui_font_scale_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IDE Editor Font Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${editorFontSize.roundToInt()} px",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.uiSp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = editorFontSize,
                        onValueChange = onUpdateEditorFontSize,
                        valueRange = 8f..22f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 0. Auto Speed Optimization (Shorts Duration Fit: 15s, 20s, 30s, 45s, 1m)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Auto Speed Optimization (Shorts Fit)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automatically paces typing speed so your full code fits inside the target Short duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoDurationTarget.entries.forEach { target ->
                            FilterChip(
                                selected = target == autoDurationTarget,
                                onClick = { onSelectAutoDurationTarget(target) },
                                label = { Text(target.fullLabel) }
                            )
                        }
                    }

                    Text(
                        text = buildString {
                            append("Total Code: ${sequenceMetrics.totalChars} chars (${sequenceMetrics.effectiveSteps} steps) • ")
                            append("Auto Lv.${sequenceMetrics.recommendedSpeedLevel} • ")
                            append("Est. Video: ~${sequenceMetrics.estimatedTotalSeconds}s")
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 0.5 Built-in ASMR Mechanical Typing Sound Effects
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ASMR Typing Sound Effects (No Audio Editing Needed)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypingSoundProfile.entries.forEach { sound ->
                        FilterChip(
                            selected = sound == typingSoundProfile,
                            onClick = { onSelectTypingSoundProfile(sound) },
                            label = { Text(sound.label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // =========================================================================
            // 1. VIRAL HOOK BANNER OVERLAY (FULL CUSTOMIZATIONS + TRANSPARENT BG)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Viral Hook Banner Overlay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize style, transparent BG, position, alignment, colors & size",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = showViralHookBanner,
                    onCheckedChange = { onToggleShowViralHookBanner() },
                    modifier = Modifier.testTag("toggle_viral_hook_banner")
                )
            }

            if (showViralHookBanner) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Live Preview Box for Hook Banner
                        Text(
                            text = "LIVE HOOK BANNER PREVIEW",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0B1120), Color(0xFF1E293B))
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            ViralHookHeaderBanner(
                                badgeText = viralHookBadge,
                                titleText = viralHookTitle.ifBlank { "Your Viral Hook Title" },
                                style = viralHookStyle,
                                alignment = viralHookAlignment,
                                transparentBg = viralHookTransparentBg,
                                showBorder = viralHookShowBorder,
                                showBadge = viralHookShowBadge,
                                accentIndex = viralHookAccentIndex,
                                sizeScale = viralHookSizeScale,
                                onClick = {}
                            )
                        }

                        // Quick Toggles: Transparent BG, Border Outline, Show Badge
                        Text(
                            text = "Background & Appearance Toggles",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = viralHookTransparentBg,
                                onClick = onToggleViralHookTransparentBg,
                                label = { Text("Transparent BG ✨") },
                                modifier = Modifier.testTag("hook_transparent_bg_chip")
                            )
                            FilterChip(
                                selected = viralHookShowBorder,
                                onClick = onToggleViralHookShowBorder,
                                label = { Text("Border Outline") }
                            )
                            FilterChip(
                                selected = viralHookShowBadge,
                                onClick = onToggleViralHookShowBadge,
                                label = { Text("Show Badge Pill") }
                            )
                        }

                        // Hook Visual Style
                        Text(
                            text = "Hook Banner Style (6 Styles)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ViralHookStyle.entries.forEach { hStyle ->
                                FilterChip(
                                    selected = hStyle == viralHookStyle,
                                    onClick = { onSelectViralHookStyle(hStyle) },
                                    label = { Text(hStyle.label) }
                                )
                            }
                        }

                        // Hook Banner Position
                        Text(
                            text = "Hook Banner Position",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ViralHookPosition.entries.forEach { pos ->
                                FilterChip(
                                    selected = pos == viralHookPosition,
                                    onClick = { onSelectViralHookPosition(pos) },
                                    label = { Text(pos.label) }
                                )
                            }
                        }

                        // Hook Text Alignment & Size Scale
                        Text(
                            text = "Alignment & Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ViralHookAlignment.entries.forEach { align ->
                                FilterChip(
                                    selected = align == viralHookAlignment,
                                    onClick = { onSelectViralHookAlignment(align) },
                                    label = { Text(align.label) }
                                )
                            }
                            OverlaySizeScale.entries.forEach { scaleItem ->
                                FilterChip(
                                    selected = scaleItem == viralHookSizeScale,
                                    onClick = { onSelectViralHookSizeScale(scaleItem) },
                                    label = { Text("Size: ${scaleItem.label}") }
                                )
                            }
                        }

                        // Hook Accent Color Palette
                        Text(
                            text = "Hook Color Theme",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CreatorAccentPalette.entries.forEach { palette ->
                                FilterChip(
                                    selected = palette.index == viralHookAccentIndex,
                                    onClick = { onSelectViralHookAccentIndex(palette.index) },
                                    label = { Text(palette.label) }
                                )
                            }
                        }

                        // 1-Tap Quick Viral Hook Text Presets
                        Text(
                            text = "1-Tap Ready-Made Viral Hooks",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VIRAL_HOOK_QUICK_PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = viralHookTitle == preset.title && viralHookBadge == preset.badge,
                                    onClick = { onApplyViralHookQuickPreset(preset) },
                                    label = { Text(preset.label) }
                                )
                            }
                        }

                        if (viralHookShowBadge) {
                            OutlinedTextField(
                                value = viralHookBadge,
                                onValueChange = onUpdateViralHookBadge,
                                label = { Text("Hook Badge (e.g. 🔥 HTML • CSS • JS)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = viralHookTitle,
                            onValueChange = onUpdateViralHookTitle,
                            label = { Text("Viral Hook Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // =========================================================================
            // 2. CREATOR HANDLE WATERMARK
            // =========================================================================
            OutlinedTextField(
                value = creatorHandle,
                onValueChange = onUpdateHandle,
                label = { Text("Creator Handle / Watermark") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Handle Position (Top, Bottom, Overlay, Footer)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WatermarkPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = pos == watermarkPosition,
                            onClick = { onSelectWatermarkPosition(pos) },
                            label = { Text(pos.label) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Handle Visual Style",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WatermarkStyle.entries.forEach { style ->
                        FilterChip(
                            selected = style == watermarkStyle,
                            onClick = { onSelectWatermarkStyle(style) },
                            label = { Text(style.label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // =========================================================================
            // 3. LIKE • COMMENT • SUBSCRIBE BAR & OUTRO (FULL CUSTOMIZATIONS + TRANSPARENT BG)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Like • Comment • Subscribe Bar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize layout, transparent BG, position, timing, colors & button text",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = showEndCtaAnimation,
                    onCheckedChange = { onToggleShowEndCta() },
                    modifier = Modifier.testTag("toggle_end_cta_bar")
                )
            }

            if (showEndCtaAnimation) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Live Animated Preview Box for Like • Comment • Subscribe Bar
                        Text(
                            text = "LIVE LIKE • COMMENT • SUBSCRIBE BAR PREVIEW",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0B1120), Color(0xFF1E293B))
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CreatorEndCtaAnimationOverlay(
                                creatorHandle = creatorHandle,
                                ctaMessage = endCtaMessage,
                                style = endCtaStyle,
                                transparentBg = ctaTransparentBg,
                                showBorder = ctaShowBorder,
                                showHandle = ctaShowHandle,
                                accentIndex = ctaAccentIndex,
                                sizeScale = ctaSizeScale,
                                showLike = ctaShowLike,
                                showComment = ctaShowComment,
                                showSubscribe = ctaShowSubscribe,
                                likeLabel = ctaLikeLabel,
                                commentLabel = ctaCommentLabel,
                                subscribeLabel = ctaSubscribeLabel,
                                subscribedLabel = ctaSubscribedLabel,
                                loopAnimation = true,
                                onDismiss = {}
                            )
                        }

                        // Background & Frame Toggles (Transparent BG, Border Frame, Show @Handle)
                        Text(
                            text = "Background & Frame Options",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = ctaTransparentBg,
                                onClick = onToggleCtaTransparentBg,
                                label = { Text("Transparent BG ✨") },
                                modifier = Modifier.testTag("cta_transparent_bg_chip")
                            )
                            FilterChip(
                                selected = ctaShowBorder,
                                onClick = onToggleCtaShowBorder,
                                label = { Text("Border Frame") }
                            )
                            FilterChip(
                                selected = ctaShowHandle,
                                onClick = onToggleCtaShowHandle,
                                label = { Text("Show @Handle") }
                            )
                        }

                        // Bar Visual Layout (6 Styles)
                        Text(
                            text = "Bar Style / Layout (6 Styles)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EndCtaStyle.entries.forEach { ctaStyle ->
                                FilterChip(
                                    selected = ctaStyle == endCtaStyle,
                                    onClick = { onSelectEndCtaStyle(ctaStyle) },
                                    label = { Text(ctaStyle.label) }
                                )
                            }
                        }

                        // Bar Position Selector
                        Text(
                            text = "Bar Position on Screen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EndCtaPosition.entries.forEach { pos ->
                                FilterChip(
                                    selected = pos == endCtaPosition,
                                    onClick = { onSelectEndCtaPosition(pos) },
                                    label = { Text(pos.label) }
                                )
                            }
                        }

                        // Display Timing (At End 100%, Always On, Last 20%, From 50%)
                        Text(
                            text = "When to Show Bar (Timing)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CtaDisplayTiming.entries.forEach { timing ->
                                FilterChip(
                                    selected = timing == ctaDisplayTiming,
                                    onClick = { onSelectCtaDisplayTiming(timing) },
                                    label = { Text(timing.label) }
                                )
                            }
                        }

                        // Accent Color & Size Scale
                        Text(
                            text = "Bar Color Theme & Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CreatorAccentPalette.entries.forEach { palette ->
                                FilterChip(
                                    selected = palette.index == ctaAccentIndex,
                                    onClick = { onSelectCtaAccentIndex(palette.index) },
                                    label = { Text(palette.label) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OverlaySizeScale.entries.forEach { scaleItem ->
                                FilterChip(
                                    selected = scaleItem == ctaSizeScale,
                                    onClick = { onSelectCtaSizeScale(scaleItem) },
                                    label = { Text("Size: ${scaleItem.label}") }
                                )
                            }
                        }

                        // Visible Buttons & 1-Tap Platform Presets
                        Text(
                            text = "Visible Action Buttons & Platform Presets",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = ctaShowLike,
                                onClick = onToggleCtaLike,
                                label = { Text("Like ❤️") }
                            )
                            FilterChip(
                                selected = ctaShowComment,
                                onClick = onToggleCtaComment,
                                label = { Text("Comment 💬") }
                            )
                            FilterChip(
                                selected = ctaShowSubscribe,
                                onClick = onToggleCtaSubscribe,
                                label = { Text("Subscribe 🔔") }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CTA_PLATFORM_QUICK_PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = ctaSubscribeLabel == preset.subscribeLabel && ctaLikeLabel == preset.likeLabel,
                                    onClick = { onApplyCtaPlatformPreset(preset) },
                                    label = { Text("Preset: ${preset.label}") }
                                )
                            }
                        }

                        // Custom Button Text Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (ctaShowLike) {
                                OutlinedTextField(
                                    value = ctaLikeLabel,
                                    onValueChange = onUpdateCtaLikeLabel,
                                    label = { Text("Like Text") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (ctaShowComment) {
                                OutlinedTextField(
                                    value = ctaCommentLabel,
                                    onValueChange = onUpdateCtaCommentLabel,
                                    label = { Text("Comment Text") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (ctaShowSubscribe) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = ctaSubscribeLabel,
                                    onValueChange = onUpdateCtaSubscribeLabel,
                                    label = { Text("Subscribe Button") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ctaSubscribedLabel,
                                    onValueChange = onUpdateCtaSubscribedLabel,
                                    label = { Text("After Subscribe") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = endCtaMessage,
                            onValueChange = onUpdateEndCtaMessage,
                            label = { Text("Custom Call-to-Action Subtitle (Optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(
                            onClick = onPreviewEndCta,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Overlay on Studio Screen Now")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 5. Video Stage Backdrop
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Video Stage Backdrop",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortsBackdropPreset.entries.forEach { preset ->
                        val selected = preset.index == selectedBackdropIndex
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectBackdrop(preset.index) },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3-2-1 Countdown Before Recording",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gives you 3 seconds to start your screen recorder before typing begins at 0%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = useRecordingCountdown,
                    onCheckedChange = { onToggleUseRecordingCountdown() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Split Live Output in Record Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automatically keeps the bottom Live Output open during recording so viewers see real-time UI updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = keepPreviewOpenInRecord,
                    onCheckedChange = { onToggleKeepPreviewOpenInRecord() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Chain All 3 Files",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Smoothly slides index.html → style.css → script.js in one continuous video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isAutoChainTabs,
                    onCheckedChange = { onToggleAutoChain() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Open Bottom Preview at 100%",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Expands the bottom live output section when typing finishes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = autoRevealPreview,
                    onCheckedChange = { onToggleAutoRevealPreview() }
                )
            }
        }
    }
}
