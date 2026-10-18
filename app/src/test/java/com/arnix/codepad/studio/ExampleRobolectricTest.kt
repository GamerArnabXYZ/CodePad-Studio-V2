package com.arnix.codepad.studio

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.arnix.codepad.studio.data.CodeProjectEntity
import com.arnix.codepad.studio.data.CodeTab
import com.arnix.codepad.studio.engine.CodeFormatter
import com.arnix.codepad.studio.engine.LogLevel
import com.arnix.codepad.studio.engine.NativeLiveEngine
import com.arnix.codepad.studio.engine.PreviewScaleMode
import com.arnix.codepad.studio.engine.SyntaxHighlighter
import com.arnix.codepad.studio.engine.TypingSoundEngine
import com.arnix.codepad.studio.engine.TypingSoundProfile
import com.arnix.codepad.studio.engine.WebSandboxEngine
import com.arnix.codepad.studio.ui.theme.DarkEditorPalette
import com.arnix.codepad.studio.ui.theme.createAppTypography
import com.arnix.codepad.studio.viewmodel.CreatorAccentPalette
import com.arnix.codepad.studio.viewmodel.EndCtaStyle
import com.arnix.codepad.studio.viewmodel.OverlaySizeScale
import com.arnix.codepad.studio.viewmodel.ViralHookStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun appContext_readsUpdatedAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("CodePad Studio", context.getString(R.string.app_name))
    }

    @Test
    fun nativeLiveEngine_parsesHtmlCssJs_andExecutesDomMutationsWithCommentsAndHex8() {
        val html = """
            <div class="card">
                <h1 id="title">Initial Title</h1>
                <p id="status">Waiting</p>
                <button onclick="boostPower()">Boost</button>
            </div>
        """.trimIndent()

        val css = """
            /* Multi-line CSS comment
               with { braces } inside that should be ignored */
            body {
                background-color: #0D111A;
            }
            .card {
                background-color: #161F30FF;
                border-radius: 16px;
                padding: 18px;
            }
            #title {
                color: #38BDF8;
                font-size: 20px;
            }
        """.trimIndent()

        val js = """
            let power = 10;
            console.log("Engine online");
            function boostPower() {
                power += 5;
                document.getElementById("title").innerText = `Power: ${'$'}{power}`;
                document.getElementById("status").style.backgroundColor = "#22C55E";
                document.body.style.backgroundColor = "#1E293B";
                console.info("Boosted power");
            }
        """.trimIndent()

        val initialSnapshot = NativeLiveEngine.evaluateProject(
            html = html,
            css = css,
            js = js
        )
        assertTrue(initialSnapshot.rootNodes.isNotEmpty())
        assertTrue(initialSnapshot.functions.containsKey("boostPower"))
        assertTrue(initialSnapshot.logs.any { it.message.contains("Engine online") })

        val mutableVars = initialSnapshot.variables.toMutableMap()
        val textOverrides = mutableMapOf<String, String>()
        val colorOverrides = mutableMapOf<String, Color>()
        val bgOverrides = mutableMapOf<String, Color>()

        val callLogs = NativeLiveEngine.invokeJsFunction(
            functionName = "boostPower",
            functions = initialSnapshot.functions,
            variables = mutableVars,
            textOverrides = textOverrides,
            colorOverrides = colorOverrides,
            bgOverrides = bgOverrides
        )

        assertEquals("15", mutableVars["power"])
        assertEquals("Power: 15", textOverrides["title"])
        assertNotNull(bgOverrides["status"])
        assertNotNull(bgOverrides["body"])
        assertTrue(callLogs.any { it.message.contains("Boosted power") })

        val updatedSnapshot = NativeLiveEngine.evaluateProject(
            html = html,
            css = css,
            js = js,
            existingVariables = mutableVars,
            domTextOverrides = textOverrides,
            domColorOverrides = colorOverrides,
            domBgOverrides = bgOverrides,
            runTopLevelJs = false
        )
        val cardNode = updatedSnapshot.rootNodes.first()
        assertNotNull(cardNode.style.backgroundColor)
        val titleNode = cardNode.children.first { it.id == "title" }
        assertEquals("Power: 15", titleNode.text)
    }

    @Test
    fun codeFormatter_handlesSingleLineRulesAndMultiClosingBracesWithoutMisindent() {
        val rawCss = "/* header */ h1 { color: #38bdf8; } .box { padding: 12px; }"
        val formattedCss = CodeFormatter.format(rawCss, CodeTab.STYLE_CSS)
        assertTrue(formattedCss.contains("h1 {"))
        assertTrue(formattedCss.contains(".box {"))

        val rawJs = """
            function run() {
            if (true) {
            console.log("ok");
            }}
        """.trimIndent()
        val formattedJs = CodeFormatter.format(rawJs, CodeTab.SCRIPT_JS)
        assertTrue(formattedJs.endsWith("}"))
    }

    @Test
    fun syntaxHighlighter_preservesUrlsBeforeInlineCommentsAndRespectsCacheBounds() {
        val line = """const api = "https://example.com/api"; // trailing comment"""
        val annotated = SyntaxHighlighter.highlight(line, CodeTab.SCRIPT_JS, DarkEditorPalette)
        assertEquals(line, annotated.text)
        val precomputed = SyntaxHighlighter.precomputeLines(line, CodeTab.SCRIPT_JS, DarkEditorPalette)
        assertEquals(1, precomputed.lines.size)
    }

    @Test
    fun typingSoundEngine_lifecycleSafeInitAndRelease() {
        TypingSoundEngine.playKeyTick(TypingSoundProfile.MECH_THOCK, 'a')
        TypingSoundEngine.playKeyTick(TypingSoundProfile.CRISP_CLICK, '\n')
        TypingSoundEngine.stopAll()
        TypingSoundEngine.release()
    }

    @Test
    fun hookAndCtaStyles_supportTransparentAndAllPresets() {
        assertTrue(ViralHookStyle.entries.contains(ViralHookStyle.TRANSPARENT_CLEAN))
        assertTrue(EndCtaStyle.entries.contains(EndCtaStyle.TRANSPARENT_BAR))
        assertEquals(CreatorAccentPalette.CYBER_CYAN_PINK, CreatorAccentPalette.fromIndex(999))
        assertEquals(OverlaySizeScale.STANDARD, OverlaySizeScale.fromIndex(999))
    }

    @Test
    fun typographyScale_andDefaultFontSizes_areCompactAndConfigurable() {
        val defaultEntity = CodeProjectEntity()
        assertEquals(11.5f, defaultEntity.editorFontSize, 0.01f)
        assertEquals(11f, defaultEntity.autoWriteFontSize, 0.01f)
        assertEquals(0.92f, defaultEntity.uiFontScale, 0.01f)

        val compactTypography = createAppTypography(0.80f)
        val largeTypography = createAppTypography(1.20f)
        assertTrue(compactTypography.bodyLarge.fontSize.value < largeTypography.bodyLarge.fontSize.value)
        assertTrue(compactTypography.titleMedium.fontSize.value < largeTypography.titleMedium.fontSize.value)
    }

    @Test
    fun webSandboxEngine_handlesFullHtmlDocument_cdnAutoDetect_scssComments_andSyntaxCompletion() {
        val singleFileSnippet = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;700&display=swap');
                    // SCSS-style single line comment that online editors allow
                    body {
                        height: 100vh;
                        overflow: hidden;
                        background-image: url("https://example.com/bg.png");
                    }
                    .btn { color: #38bdf8; }
                </style>
            </head>
            <body class="dark-body">
                <div class="container">
                    <i class="fa-solid fa-bolt"></i>
                    <canvas id="scene"></canvas>
                </div>
                <script>
                    const items = [1, 2, 3].map(x => x * 2);
                    document.querySelector('.container').classList.add('ready');
                    console.log("Items:", items);
                </script>
            </body>
            </html>
        """.trimIndent()

        assertTrue(WebSandboxEngine.hasEmbeddedStyleOrScript(singleFileSnippet))

        val split = WebSandboxEngine.splitEmbeddedHtml(singleFileSnippet)
        assertTrue(split.didSplit)
        assertTrue(split.extractedCss.contains(".btn { color: #38bdf8; }"))
        assertTrue(split.extractedJs.contains("const items = [1, 2, 3].map(x => x * 2);"))
        assertFalse(split.cleanedHtml.contains("<style>"))

        val normalizedCss = WebSandboxEngine.normalizeUserCss(split.extractedCss)
        assertTrue(normalizedCss.trimStart().startsWith("@import"))
        assertTrue(normalizedCss.contains("/* SCSS-style single line comment"))
        assertTrue(normalizedCss.contains("https://example.com/bg.png"))

        val fullDoc = WebSandboxEngine.buildFullHtmlDocument(
            rawHtmlCode = singleFileSnippet,
            rawCssCode = "",
            rawJsCode = "",
            runJs = true,
            scaleMode = PreviewScaleMode.AUTO_FIT
        )
        // Auto-detected FontAwesome CDN from fa-solid fa-bolt
        assertTrue(fullDoc.contains("font-awesome"))
        assertTrue(fullDoc.contains("window.__codepadApplyLayoutGuard"))

        // Syntax completeness checks for mid-typing vs completed ES6+ code
        assertFalse(WebSandboxEngine.isJsSyntacticallyComplete("const x = { a: 1"))
        assertFalse(WebSandboxEngine.isJsSyntacticallyComplete("function demo(a, b) { console.log('hi'"))
        assertTrue(WebSandboxEngine.isJsSyntacticallyComplete("const fn = (a, b) => ({ sum: a + b });\nconst re = /^https?:\\/\\/[^\\s]+$/;"))
    }

    @Test
    fun nativeLiveEngine_executesModernEs6CodeWithoutFalseReferenceErrors() {
        val html = """
            <div class="wrapper">
                <h2 id="heading">Hello</h2>
                <canvas id="gameCanvas" width="300" height="150"></canvas>
                <svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="40"/></svg>
            </div>
        """.trimIndent()

        val css = """
            @keyframes pulse {
                0% { opacity: 0.5; }
                100% { opacity: 1; }
            }
            .wrapper {
                padding: 16px;
                background: linear-gradient(135deg, #1e293b, #0f172a);
            }
        """.trimIndent()

        val js = """
            const canvas = document.getElementById('gameCanvas');
            const ctx = canvas.getContext('2d');
            const items = [10, 20, 30].filter(n => n > 15);
            document.querySelector('#heading').textContent = "Canvas Ready";
            window.addEventListener('resize', () => {
                console.log("Resized");
            });
            console.log("Loaded items:", items.length);
        """.trimIndent()

        val snapshot = NativeLiveEngine.evaluateProject(html = html, css = css, js = js)
        assertTrue(snapshot.logs.none { it.level == LogLevel.ERROR })
        val wrapper = snapshot.rootNodes.first()
        val heading = wrapper.children.first { it.id == "heading" }
        assertEquals("Canvas Ready", heading.text)
    }

    @Test
    fun typingSoundEngine_initializesAndPlaysAllProfilesWithoutExceptions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TypingSoundEngine.initialize(context)

        TypingSoundProfile.entries.forEach { profile ->
            TypingSoundEngine.playKeyTick(
                profile = profile,
                typedChar = 'a',
                isManualTest = true,
                volume = 0.8f,
                minGapMs = 50L,
                pitchVariation = true
            )
            // Test spacebar / deep key variation
            TypingSoundEngine.playKeyTick(
                profile = profile,
                typedChar = '\n',
                isManualTest = true,
                volume = 0.8f,
                minGapMs = 50L,
                pitchVariation = true
            )
        }
        TypingSoundEngine.stopAll()
    }

    @Test
    fun studioUiState_supportsAllRecCustomizationOptions() {
        var state = com.arnix.codepad.studio.viewmodel.StudioUiState()
        assertEquals(0.85f, state.watermarkOpacity, 0.01f)
        assertEquals(OverlaySizeScale.STANDARD, state.watermarkSizeScale)
        assertEquals(0.85f, state.typingSoundVolume, 0.01f)
        assertEquals(75L, state.typingSoundMinGapMs)
        assertTrue(state.typingSoundPitchVar)
        assertTrue(state.autoWriteShowGutter)

        state = state.copy(
            watermarkOpacity = 0.5f,
            watermarkSizeScale = OverlaySizeScale.LARGE,
            typingSoundVolume = 0.9f,
            typingSoundMinGapMs = 120L,
            typingSoundPitchVar = false,
            autoWriteShowGutter = false
        )
        assertEquals(0.5f, state.watermarkOpacity, 0.01f)
        assertEquals(OverlaySizeScale.LARGE, state.watermarkSizeScale)
        assertEquals(0.9f, state.typingSoundVolume, 0.01f)
        assertEquals(120L, state.typingSoundMinGapMs)
        assertFalse(state.typingSoundPitchVar)
        assertFalse(state.autoWriteShowGutter)
    }

    @Test
    fun splashScreen_creatorCredit_isConfigured() {
        val expectedCredit = "Made By ArnixLabzStudio"
        assertTrue(expectedCredit.contains("ArnixLabzStudio"))
    }

    @Test
    fun startupCrashResilience_soundEngineAndDatabaseSafeOnAllApis() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Initialize sound engine safely
        TypingSoundEngine.initialize(context)
        // Verify no crash on keystroke or release
        TypingSoundEngine.playKeyTick(
            profile = TypingSoundProfile.MECH_THOCK,
            typedChar = 'A',
            isManualTest = true
        )
        TypingSoundEngine.stopAll()
        TypingSoundEngine.release()

        // Verify database getOrInitProject does not crash
        val db = com.arnix.codepad.studio.data.CodePadDatabase.getInstance(context)
        val repo = com.arnix.codepad.studio.data.CodeProjectRepository(db.codeProjectDao())
        assertNotNull(repo)
    }
}

