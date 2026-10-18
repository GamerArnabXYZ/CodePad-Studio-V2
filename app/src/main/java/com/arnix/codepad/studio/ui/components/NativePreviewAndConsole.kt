package com.arnix.codepad.studio.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arnix.codepad.studio.engine.ComputedNodeStyle
import com.arnix.codepad.studio.engine.ConsoleLogEntry
import com.arnix.codepad.studio.engine.LogLevel
import com.arnix.codepad.studio.engine.NativeDomNode
import com.arnix.codepad.studio.engine.PreviewScaleMode
import com.arnix.codepad.studio.engine.WebSandboxEngine
import com.arnix.codepad.studio.ui.theme.ConsoleErrorRed
import com.arnix.codepad.studio.ui.theme.ConsoleInfoBlue
import com.arnix.codepad.studio.ui.theme.ConsoleWarnAmber
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.StatusSavedGreenLight
import com.arnix.codepad.studio.ui.theme.uiSp
import kotlin.math.roundToInt

/**
 * Interactive WebView subclass that prevents parent scroll containers from stealing
 * touch events on <canvas>, sliders, buttons, and scrollable web containers.
 */
private class SandboxWebView(context: Context) : WebView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }
}

private class CodePadJsBridge(
    private val mainHandler: Handler,
    private val onLog: (LogLevel, String, String, String?) -> Unit,
    private val onClear: () -> Unit
) {
    @JavascriptInterface
    fun postLog(levelStr: String, message: String, source: String, stackTrace: String) {
        val level = when (levelStr.uppercase()) {
            "INFO" -> LogLevel.INFO
            "WARN" -> LogLevel.WARN
            "ERROR" -> LogLevel.ERROR
            "SYSTEM" -> LogLevel.SYSTEM
            else -> LogLevel.LOG
        }
        mainHandler.post {
            onLog(level, message, source, stackTrace.takeIf { it.isNotBlank() })
        }
    }

    @JavascriptInterface
    fun clearConsole() {
        mainHandler.post {
            onClear()
        }
    }
}

private class WebViewLoadTracker(
    var lastStructuralSignature: String = "__UNSET__",
    var lastCss: String = "__UNSET__",
    var isShellLoaded: Boolean = false
)

/**
 * Real Chromium WebView + Fallback Native DOM Live Preview Panel.
 * - Executes full HTML5, CSS3 (Flexbox, Grid, Keyframes, Variables, Gradients), Canvas, SVG, and ES6+ JS.
 * - Streams real V8 console logs & runtime/syntax errors directly to the Integrated Console Panel.
 * - Supports 60fps zero-flicker live CSS/DOM updates during Auto Write Studio and live editing.
 * - Supports interactive 1:1 vertical finger dragging on the top handle / header bar to resize.
 */
@Composable
fun NativeDomPreviewPanel(
    domNodes: List<NativeDomNode>,
    bodyStyle: ComputedNodeStyle,
    onDomButtonClick: (String) -> Unit,
    onRefreshRunJs: () -> Unit,
    htmlCode: String = "",
    cssCode: String = "",
    jsCode: String = "",
    runJs: Boolean = true,
    isProgressiveAutoWrite: Boolean = false,
    previewScaleMode: PreviewScaleMode = PreviewScaleMode.AUTO_FIT,
    previewReloadNonce: Long = 0L,
    onCycleScaleMode: (() -> Unit)? = null,
    onToggleExpandHeight: (() -> Unit)? = null,
    onWebConsoleLog: ((LogLevel, String, String, String?) -> Unit)? = null,
    onWebConsoleClear: (() -> Unit)? = null,
    onWebViewAttachedChanged: ((Boolean) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    isRecordingMode: Boolean = false,
    autoScrollPreview: Boolean = false,
    maxBodyHeight: Dp = 175.dp,
    onResizeDeltaDp: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val resizeDraggableState = rememberDraggableState { deltaPx ->
        if (onResizeDeltaDp != null) {
            val deltaDp = with(density) { (-deltaPx).toDp().value }
            onResizeDeltaDp(deltaDp)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("native_live_preview_panel"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // Top Finger-Drag Resize Handle + Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .then(
                        if (onResizeDeltaDp != null) {
                            Modifier
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = resizeDraggableState
                                )
                                .testTag("preview_resize_drag_handle")
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (onResizeDeltaDp != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                        )
                    }
                }

                // Compact Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusSavedGreenLight)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Live Preview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isRecordingMode) "Live Output" else "Live Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (onResizeDeltaDp != null && !isRecordingMode) {
                            Text(
                                text = "↕ ${maxBodyHeight.value.roundToInt()}dp",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.uiSp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isRecordingMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (onCycleScaleMode != null) {
                                Surface(
                                    onClick = onCycleScaleMode,
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                                    border = BorderStroke(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("preview_scale_mode_button")
                                ) {
                                    Text(
                                        text = previewScaleMode.label,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.uiSp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (onToggleExpandHeight != null) {
                                IconButton(
                                    onClick = onToggleExpandHeight,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("expand_preview_height_button")
                                ) {
                                    val isExpanded = maxBodyHeight.value >= 300f
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                        contentDescription = if (isExpanded) "Compact Preview Height" else "Expand Preview Height",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onRefreshRunJs,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("rerun_js_preview_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload Preview",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (onClose != null) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("close_preview_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Preview",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Drag ↕ to Resize • 2× Tap to Hide",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.uiSp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Preview Viewport Area
            val heightModifier = if (onResizeDeltaDp != null) {
                Modifier.height(maxBodyHeight)
            } else {
                Modifier.heightIn(min = 90.dp, max = maxBodyHeight)
            }

            val isInspection = LocalInspectionMode.current
            val hasRawCode = htmlCode.isNotBlank() || cssCode.isNotBlank() || jsCode.isNotBlank()

            if (!isInspection && hasRawCode) {
                ChromiumWebSandboxViewport(
                    htmlCode = htmlCode,
                    cssCode = cssCode,
                    jsCode = jsCode,
                    runJs = runJs,
                    isProgressiveAutoWrite = isProgressiveAutoWrite,
                    previewScaleMode = previewScaleMode,
                    previewReloadNonce = previewReloadNonce,
                    autoScrollPreview = autoScrollPreview,
                    onWebConsoleLog = onWebConsoleLog,
                    onWebConsoleClear = onWebConsoleClear,
                    onWebViewAttachedChanged = onWebViewAttachedChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(heightModifier)
                )
            } else {
                FallbackComposeDomViewport(
                    domNodes = domNodes,
                    bodyStyle = bodyStyle,
                    onDomButtonClick = onDomButtonClick,
                    autoScrollPreview = autoScrollPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(heightModifier)
                )
            }
        }
    }
}

/**
 * Hardware-accelerated Chromium WebView sandbox viewport.
 * Can also be mounted invisibly (0.dp) when the preview panel is collapsed in IDE mode so
 * JavaScript V8 execution and Console logging stay continuously active.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChromiumWebSandboxViewport(
    htmlCode: String,
    cssCode: String,
    jsCode: String,
    runJs: Boolean,
    isProgressiveAutoWrite: Boolean,
    previewScaleMode: PreviewScaleMode,
    previewReloadNonce: Long,
    autoScrollPreview: Boolean,
    onWebConsoleLog: ((LogLevel, String, String, String?) -> Unit)?,
    onWebConsoleClear: (() -> Unit)?,
    onWebViewAttachedChanged: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnLog by rememberUpdatedState(onWebConsoleLog)
    val currentOnClear by rememberUpdatedState(onWebConsoleClear)
    val currentOnAttached by rememberUpdatedState(onWebViewAttachedChanged)
    val loadTracker = remember { WebViewLoadTracker() }

    val webView = remember {
        runCatching {
            val mainHandler = Handler(Looper.getMainLooper())
            SandboxWebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Avoid hardware/software rendering glitches on emulators without dedicated GPU render node
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(AndroidColor.WHITE)
                isFocusable = true
                isFocusableInTouchMode = true
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    mediaPlaybackRequiresUserGesture = true
                    loadsImagesAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                    textZoom = 100
                }

                addJavascriptInterface(
                    CodePadJsBridge(
                        mainHandler = mainHandler,
                        onLog = { level, msg, src, stack ->
                            currentOnLog?.invoke(level, msg, src, stack)
                        },
                        onClear = {
                            currentOnClear?.invoke()
                        }
                    ),
                    "CodePadBridge"
                )

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        // Keep hash jumps and about:blank inside the sandbox; block external navigation away
                        return !(url.startsWith("https://appassets.androidplatform.net") ||
                            url.startsWith("about:blank") ||
                            url.startsWith("data:") ||
                            url.contains("#"))
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        loadTracker.isShellLoaded = true
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val cm = consoleMessage ?: return true
                        val msg = cm.message() ?: return true
                        // Catch parser-level SyntaxErrors or uncaught browser errors not already routed by CodePadBridge
                        if (cm.messageLevel() == ConsoleMessage.MessageLevel.ERROR &&
                            !msg.contains("ResizeObserver loop") &&
                            msg != "Script error."
                        ) {
                            val line = cm.lineNumber()
                            val src = if (line > 0) "script.js:$line" else "script.js"
                            currentOnLog?.invoke(LogLevel.ERROR, msg, src, "$msg\n    at $src")
                        }
                        return true
                    }

                    override fun onJsAlert(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        currentOnLog?.invoke(LogLevel.INFO, "[alert] ${message.orEmpty()}", "window.alert", null)
                        result?.confirm()
                        return true
                    }

                    override fun onJsConfirm(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        currentOnLog?.invoke(LogLevel.INFO, "[confirm] ${message.orEmpty()} -> true", "window.confirm", null)
                        result?.confirm()
                        return true
                    }

                    override fun onJsPrompt(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        defaultValue: String?,
                        result: JsPromptResult?
                    ): Boolean {
                        val reply = defaultValue.orEmpty()
                        currentOnLog?.invoke(LogLevel.INFO, "[prompt] ${message.orEmpty()} -> $reply", "window.prompt", null)
                        result?.confirm(reply)
                        return true
                    }
                }
            }
        }.getOrNull()
    }

    DisposableEffect(webView) {
        if (webView != null) {
            currentOnAttached?.invoke(true)
        }
        onDispose {
            currentOnAttached?.invoke(false)
            webView?.let { wv ->
                runCatching {
                    wv.stopLoading()
                    wv.evaluateJavascript("if(window.__codepadCleanupTimers) window.__codepadCleanupTimers();", null)
                    wv.loadUrl("about:blank")
                    wv.removeAllViews()
                    wv.destroy()
                }
            }
        }
    }

    // Apply scale mode changes immediately without full reload
    LaunchedEffect(webView, previewScaleMode) {
        val wv = webView ?: return@LaunchedEffect
        if (loadTracker.isShellLoaded) {
            wv.evaluateJavascript(
                "if(window.__codepadApplyLayoutGuard) window.__codepadApplyLayoutGuard('${previewScaleMode.name}');",
                null
            )
        }
    }

    // Coordinate 60fps progressive DOM/CSS updates vs full HTML+JS loads
    LaunchedEffect(webView, htmlCode, cssCode, jsCode, runJs, isProgressiveAutoWrite, previewReloadNonce) {
        val wv = webView ?: return@LaunchedEffect
        val (effectiveHtml, effectiveCss, effectiveJs) = WebSandboxEngine.resolveEffectiveCode(
            htmlCode = htmlCode,
            cssCode = cssCode,
            jsCode = jsCode
        )
        val normalizedCss = WebSandboxEngine.normalizeUserCss(effectiveCss)
        val effectiveRunJs = runJs && effectiveJs.isNotBlank() && WebSandboxEngine.isJsSyntacticallyComplete(effectiveJs)

        // Structural signature determines whether a full document load vs fast DOM/CSS injection is needed
        val structuralSignature = if (isProgressiveAutoWrite && !effectiveRunJs && !effectiveHtml.contains("<script", ignoreCase = true) && !effectiveHtml.contains("<link", ignoreCase = true)) {
            "PROGRESSIVE_SHELL::$previewReloadNonce"
        } else {
            "$effectiveHtml|||$effectiveJs|||$effectiveRunJs|||$previewReloadNonce"
        }

        if (loadTracker.isShellLoaded && structuralSignature == loadTracker.lastStructuralSignature) {
            // Fast path: Only CSS (or progressive HTML+CSS in AutoWrite) changed — update in <1ms without page flash!
            if (isProgressiveAutoWrite && !effectiveRunJs) {
                val jsBodyLiteral = WebSandboxEngine.toJsStringLiteral(effectiveHtml)
                val jsCssLiteral = WebSandboxEngine.toJsStringLiteral(normalizedCss)
                val scrollCmd = if (autoScrollPreview) {
                    "window.scrollTo({ top: document.body.scrollHeight, behavior: 'auto' });"
                } else ""
                wv.evaluateJavascript(
                    "if(window.__codepadUpdateDomAndCss){window.__codepadUpdateDomAndCss($jsBodyLiteral,$jsCssLiteral);$scrollCmd}",
                    null
                )
                loadTracker.lastCss = normalizedCss
            } else if (normalizedCss != loadTracker.lastCss) {
                val jsCssLiteral = WebSandboxEngine.toJsStringLiteral(normalizedCss)
                wv.evaluateJavascript(
                    "if(window.__codepadUpdateCssOnly) window.__codepadUpdateCssOnly($jsCssLiteral);",
                    null
                )
                loadTracker.lastCss = normalizedCss
            }
        } else {
            // Full document load with real Chromium V8 execution
            loadTracker.lastStructuralSignature = structuralSignature
            loadTracker.lastCss = normalizedCss
            val fullDocument = WebSandboxEngine.buildFullHtmlDocument(
                rawHtmlCode = htmlCode,
                rawCssCode = cssCode,
                rawJsCode = jsCode,
                runJs = runJs,
                scaleMode = previewScaleMode
            )
            wv.loadDataWithBaseURL(
                "https://appassets.androidplatform.net/",
                fullDocument,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    if (webView != null) {
        AndroidView(
            factory = { webView },
            modifier = modifier
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .testTag("chromium_webview_preview")
        )
    }
}

@Composable
private fun FallbackComposeDomViewport(
    domNodes: List<NativeDomNode>,
    bodyStyle: ComputedNodeStyle,
    onDomButtonClick: (String) -> Unit,
    autoScrollPreview: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = bodyStyle.backgroundColor ?: MaterialTheme.colorScheme.background
    val previewScrollState = rememberScrollState()

    LaunchedEffect(autoScrollPreview) {
        if (!autoScrollPreview) return@LaunchedEffect
        while (true) {
            if (!previewScrollState.isScrollInProgress) {
                val target = previewScrollState.maxValue
                val current = previewScrollState.value
                val diff = target - current
                if (diff > 0) {
                    androidx.compose.runtime.withFrameNanos { }
                    val step = kotlin.math.max(2f, diff * 0.28f).coerceAtMost(diff.toFloat())
                    previewScrollState.dispatchRawDelta(step)
                } else {
                    kotlinx.coroutines.delay(32L)
                }
            } else {
                kotlinx.coroutines.delay(32L)
            }
        }
    }

    Column(
        modifier = modifier
            .background(bg)
            .verticalScroll(previewScrollState)
            .padding((bodyStyle.paddingDp.coerceIn(10f, 16f)).dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (domNodes.isEmpty()) {
            Text(
                text = "No elements to preview yet. Add HTML in index.html.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            domNodes.forEach { node ->
                RenderNativeDomNode(
                    node = node,
                    inheritedTextColor = bodyStyle.textColor ?: MaterialTheme.colorScheme.onSurface,
                    onDomButtonClick = onDomButtonClick
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderNativeDomNode(
    node: NativeDomNode,
    inheritedTextColor: Color,
    onDomButtonClick: (String) -> Unit
) {
    val style = node.style
    val textColor = style.textColor ?: inheritedTextColor
    val bgColor = style.backgroundColor ?: Color.Transparent
    val borderColor = style.borderColor
    val cornerRadius = style.borderRadiusDp.coerceIn(0f, 20f).dp
    val pad = style.paddingDp.coerceIn(0f, 16f).dp

    when (node.tag) {
        "button" -> {
            Button(
                onClick = {
                    node.onClickFunction?.let { fn -> onDomButtonClick(fn) }
                },
                shape = RoundedCornerShape(if (style.borderRadiusDp > 0f) cornerRadius else 8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = style.backgroundColor ?: MaterialTheme.colorScheme.primary,
                    contentColor = style.textColor ?: MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("dom_btn_${node.id.ifEmpty { node.uid }}")
            ) {
                Text(
                    text = node.text.ifEmpty { "Button" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (style.fontSizeSp ?: 11f).uiSp
                )
            }
        }

        "progress" -> {
            val fraction = ((node.progressValue ?: 50f) / node.progressMax.coerceAtLeast(1f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        "hr" -> {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }

        "h1", "h2", "h3", "h4", "h5", "h6", "p", "span", "label", "strong", "b", "em", "i", "small", "code" -> {
            val defaultFontSize = when (node.tag) {
                "h1" -> 15f
                "h2" -> 13.5f
                "h3" -> 12.5f
                "h4", "h5", "h6" -> 11.5f
                "span", "small", "code" -> 10f
                else -> 11.5f
            }
            val defaultBold = node.tag.startsWith("h") ||
                node.tag == "strong" ||
                node.tag == "b" ||
                style.isBold ||
                "badge" in node.classes
            Box(
                modifier = Modifier
                    .then(
                        if (bgColor != Color.Transparent) {
                            Modifier
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(bgColor)
                        } else Modifier
                    )
                    .then(
                        if (borderColor != null) {
                            Modifier.border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
                        } else Modifier
                    )
                    .then(
                        if (node.onClickFunction != null) {
                            Modifier.clickable { onDomButtonClick(node.onClickFunction) }
                        } else Modifier
                    )
                    .padding(pad)
            ) {
                Text(
                    text = node.text,
                    color = textColor,
                    fontSize = ((style.fontSizeSp ?: defaultFontSize) * 0.92f).uiSp,
                    lineHeight = (((style.fontSizeSp ?: defaultFontSize) * 0.92f) * 1.32f).uiSp,
                    fontWeight = if (defaultBold) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        else -> {
            val isActionRow = "action-row" in node.classes || "metrics-grid" in node.classes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(bgColor)
                    .then(
                        if (borderColor != null) {
                            Modifier.border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(cornerRadius)
                            )
                        } else Modifier
                    )
                    .then(
                        if (node.onClickFunction != null) {
                            Modifier.clickable { onDomButtonClick(node.onClickFunction) }
                        } else Modifier
                    )
                    .padding(if (pad > 0.dp) pad else 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (node.text.isNotBlank()) {
                    Text(
                        text = node.text,
                        color = textColor,
                        fontSize = ((style.fontSizeSp ?: 11.5f) * 0.92f).uiSp
                    )
                }
                if (isActionRow) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        node.children.forEach { child ->
                            RenderNativeDomNode(
                                node = child,
                                inheritedTextColor = textColor,
                                onDomButtonClick = onDomButtonClick
                            )
                        }
                    }
                } else {
                    node.children.forEach { child ->
                        RenderNativeDomNode(
                            node = child,
                            inheritedTextColor = textColor,
                            onDomButtonClick = onDomButtonClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Material 3 Integrated Console Panel
 */
@Composable
fun IntegratedConsolePanel(
    logs: List<ConsoleLogEntry>,
    activeFilter: LogLevel?,
    onSelectFilter: (LogLevel?) -> Unit,
    onSimulateError: () -> Unit,
    onClearConsole: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLogs = remember(logs, activeFilter) {
        if (activeFilter == null) logs else logs.filter { it.level == activeFilter }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("integrated_console_panel"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // Compact 38.dp Header with inline filters & action icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = "Console",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Console (${filteredLogs.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSimulateError,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inject_test_error_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Test Stack Trace Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearConsole,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_console_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Console",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_console_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Console",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = activeFilter == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = activeFilter == LogLevel.LOG,
                    onClick = { onSelectFilter(if (activeFilter == LogLevel.LOG) null else LogLevel.LOG) },
                    label = { Text("Logs", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = activeFilter == LogLevel.WARN,
                    onClick = { onSelectFilter(if (activeFilter == LogLevel.WARN) null else LogLevel.WARN) },
                    label = { Text("Warnings", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = activeFilter == LogLevel.ERROR,
                    onClick = { onSelectFilter(if (activeFilter == LogLevel.ERROR) null else LogLevel.ERROR) },
                    label = { Text("Errors", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }

            // Console Entries
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 85.dp, max = 140.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        Text(
                            text = "No console output for this filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                } else {
                    items(filteredLogs, key = { it.id }) { entry ->
                        ConsoleLogItemRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleLogItemRow(entry: ConsoleLogEntry) {
    var expanded by remember(entry.id) { mutableStateOf(entry.level == LogLevel.ERROR) }
    val badgeColor = when (entry.level) {
        LogLevel.LOG -> StatusSavedGreenLight
        LogLevel.INFO -> ConsoleInfoBlue
        LogLevel.WARN -> ConsoleWarnAmber
        LogLevel.ERROR -> ConsoleErrorRed
        LogLevel.SYSTEM -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (entry.level == LogLevel.ERROR) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            )
            .clickable(enabled = entry.stackTrace != null) { expanded = !expanded }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = entry.message,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.uiSp,
                    color = if (entry.level == LogLevel.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.sourceLocation,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.uiSp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.stackTrace != null) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Stack Trace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (entry.stackTrace != null) {
            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = entry.stackTrace,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.uiSp,
                        lineHeight = 12.5.uiSp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
