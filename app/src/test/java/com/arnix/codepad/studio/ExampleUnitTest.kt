package com.arnix.codepad.studio

import com.arnix.codepad.studio.data.CodeTab
import com.arnix.codepad.studio.data.ProjectTemplates
import com.arnix.codepad.studio.engine.LogLevel
import com.arnix.codepad.studio.engine.NativeLiveEngine
import com.arnix.codepad.studio.engine.SyntaxHighlighter
import com.arnix.codepad.studio.engine.TypingSoundProfile
import com.arnix.codepad.studio.ui.theme.DarkEditorPalette
import com.arnix.codepad.studio.viewmodel.AutoDurationTarget
import com.arnix.codepad.studio.viewmodel.AutoSpeedOptimizer
import com.arnix.codepad.studio.viewmodel.CreatorAccentPalette
import com.arnix.codepad.studio.viewmodel.CtaDisplayTiming
import com.arnix.codepad.studio.viewmodel.EndCtaPosition
import com.arnix.codepad.studio.viewmodel.EndCtaStyle
import com.arnix.codepad.studio.viewmodel.StudioUiState
import com.arnix.codepad.studio.viewmodel.ViralHookAlignment
import com.arnix.codepad.studio.viewmodel.ViralHookPosition
import com.arnix.codepad.studio.viewmodel.ViralHookStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun largeCodePrecomputedHighlighting_slicesInO1WithExactAccuracy() {
        val largeCss = buildString {
            for (i in 1..400) {
                append(".card-$i {\n  background: #1e293b;\n  padding: 16px;\n  color: #38bdf8;\n}\n")
            }
        }
        val precomputed = SyntaxHighlighter.precomputeLines(
            code = largeCss,
            tab = CodeTab.STYLE_CSS,
            palette = DarkEditorPalette
        )

        // 400 blocks * 5 lines + trailing empty line = 2001 lines
        assertTrue(precomputed.lines.size >= 2000)

        // Test slicing at start, middle, and full length
        val (completedStart, activeStart) = precomputed.sliceAt(0)
        assertEquals(0, completedStart.size)
        assertEquals("", activeStart.text)

        val midChar = largeCss.length / 2
        val (completedMid, activeMid) = precomputed.sliceAt(midChar)
        val reconstructedMid = buildString {
            completedMid.forEach {
                append(it.text)
                append('\n')
            }
            append(activeMid.text)
        }
        assertEquals(largeCss.substring(0, midChar), reconstructedMid)

        val (completedFull, activeFull) = precomputed.sliceAt(largeCss.length)
        assertEquals(precomputed.lines.size - 1, completedFull.size)
        assertEquals(precomputed.lines.last().text, activeFull.text)
    }

    @Test
    fun autoSpeedOptimizer_fitsLargeSaaSDashboardIntoTargetDurations() {
        val saasPreset = ProjectTemplates.PRESETS.last()
        for (target in listOf(
            AutoDurationTarget.SEC_15,
            AutoDurationTarget.SEC_20,
            AutoDurationTarget.SEC_30,
            AutoDurationTarget.SEC_45,
            AutoDurationTarget.SEC_60
        )) {
            val metrics = AutoSpeedOptimizer.analyze(
                htmlCode = saasPreset.html,
                cssCode = saasPreset.css,
                jsCode = saasPreset.js,
                activeTab = CodeTab.INDEX_HTML,
                isAutoChain = true,
                showEndCta = true,
                target = target,
                manualSpeedLevel = 14,
                manualTurbo = false
            )
            assertTrue(
                "Expected estimated seconds (${metrics.estimatedTotalSeconds}) <= ${target.seconds}",
                metrics.estimatedTotalSeconds <= target.seconds
            )
            assertTrue(metrics.effectiveSteps > 100)
        }
    }

    @Test
    fun allSixViralTemplates_evaluateWithoutEngineErrorsAndHaveHooks() {
        assertEquals(6, ProjectTemplates.PRESETS.size)
        ProjectTemplates.PRESETS.forEach { preset ->
            assertTrue("Preset ${preset.name} should have a non-blank viral hook", preset.hookTitle.isNotBlank() && preset.hookBadge.isNotBlank())
            val snapshot = NativeLiveEngine.evaluateProject(
                html = preset.html,
                css = preset.css,
                js = preset.js,
                runTopLevelJs = true
            )
            val errors = snapshot.logs.filter { it.level == LogLevel.ERROR }
            assertTrue(
                "Expected zero errors for preset '${preset.name}', but got: ${errors.map { it.message }}",
                errors.isEmpty()
            )
            assertTrue(
                "Expected parsed DOM nodes for preset '${preset.name}'",
                snapshot.rootNodes.isNotEmpty()
            )
        }
        assertEquals(4, TypingSoundProfile.entries.size)
        assertEquals(6, ViralHookStyle.entries.size)
        assertEquals(4, ViralHookPosition.entries.size)
        assertEquals(3, ViralHookAlignment.entries.size)
        assertEquals(6, EndCtaStyle.entries.size)
        assertEquals(5, EndCtaPosition.entries.size)
        assertEquals(4, CtaDisplayTiming.entries.size)
        assertEquals(6, CreatorAccentPalette.entries.size)
    }

    @Test
    fun ctaDisplayTimingAndOverallProgress_computesAccuratelyAcrossSequence() {
        val baseState = StudioUiState(
            htmlCode = "1234567890",
            cssCode = "1234567890",
            jsCode = "1234567890",
            isAutoChainTabs = true,
            autoWriteTab = CodeTab.STYLE_CSS,
            typedCharCount = 5, // 10 (HTML) + 5 (CSS) = 15 / 30 = 50%
            showEndCtaAnimation = true,
            isEndCtaActive = false
        )

        assertEquals(0.5f, baseState.overallSequenceProgress, 0.01f)
        assertFalse(baseState.copy(ctaDisplayTiming = CtaDisplayTiming.AT_END_100).shouldDisplayCtaOverlay)
        assertTrue(baseState.copy(ctaDisplayTiming = CtaDisplayTiming.ALWAYS_VISIBLE).shouldDisplayCtaOverlay)
        assertTrue(baseState.copy(ctaDisplayTiming = CtaDisplayTiming.FROM_50_PERCENT).shouldDisplayCtaOverlay)
        assertFalse(baseState.copy(ctaDisplayTiming = CtaDisplayTiming.LAST_20_PERCENT).shouldDisplayCtaOverlay)

        val nearEndState = baseState.copy(
            autoWriteTab = CodeTab.SCRIPT_JS,
            typedCharCount = 6 // 10 + 10 + 6 = 26 / 30 = 86.6%
        )
        assertTrue(nearEndState.copy(ctaDisplayTiming = CtaDisplayTiming.LAST_20_PERCENT).shouldDisplayCtaOverlay)
    }

    @Test
    fun recordingLayoutModesAndPresets_behaveCorrectly() {
        // 1. All 4 built-in recording layout modes requested by user
        assertEquals(4, com.arnix.codepad.studio.model.RecordingLayoutMode.entries.size)
        val modes = com.arnix.codepad.studio.model.RecordingLayoutMode.entries.map { it.id }
        assertTrue(modes.contains("editor_only"))
        assertTrue(modes.contains("editor_top_preview_bottom"))
        assertTrue(modes.contains("preview_top_editor_bottom"))
        assertTrue(modes.contains("editor_then_full_preview_at_end"))

        // 2. Ultra-customizable split ratio presets sum to 100%
        com.arnix.codepad.studio.model.SplitRatioPreset.entries.forEach { preset ->
            assertEquals(1.0f, preset.editorWeight + preset.previewWeight, 0.01f)
        }

        // 3. Default RecordingLayoutConfig behaves gracefully
        val defaultConfig = com.arnix.codepad.studio.model.RecordingLayoutConfig()
        assertEquals(com.arnix.codepad.studio.model.RecordingLayoutMode.EDITOR_TOP_PREVIEW_BOTTOM, defaultConfig.layoutMode)
        assertEquals(0.52f, defaultConfig.editorWeight, 0.01f)
        assertEquals(0.48f, defaultConfig.previewWeight, 0.01f)

        // 4. Custom split ratio clamps preview weight appropriately
        val customConfig = defaultConfig.copy(editorWeight = 0.75f)
        assertEquals(0.25f, customConfig.previewWeight, 0.01f)

        // 5. Divider styles and reveal transitions
        assertEquals(4, com.arnix.codepad.studio.model.RecDividerStyle.entries.size)
        assertEquals(3, com.arnix.codepad.studio.model.RecRevealTransition.entries.size)

        // 6. Post-typing preview showcase duration auto-balance & fixed overrides
        assertEquals(4200L, defaultConfig.getEffectiveShowcaseDurationMs(AutoDurationTarget.SEC_15))
        assertEquals(8000L, defaultConfig.getEffectiveShowcaseDurationMs(AutoDurationTarget.SEC_30))
        assertEquals(15000L, defaultConfig.getEffectiveShowcaseDurationMs(AutoDurationTarget.SEC_60))

        val fixedShowcaseConfig = defaultConfig.copy(previewShowcaseSeconds = 6)
        assertEquals(6000L, fixedShowcaseConfig.getEffectiveShowcaseDurationMs(AutoDurationTarget.SEC_15))
        assertEquals(6000L, fixedShowcaseConfig.getEffectiveShowcaseDurationMs(AutoDurationTarget.SEC_30))

        // 7. AutoSpeedOptimizer fits typing AND preview showcase within target duration
        val saasPreset = ProjectTemplates.PRESETS.first()
        val metricsWithShowcase = AutoSpeedOptimizer.analyze(
            htmlCode = saasPreset.html,
            cssCode = saasPreset.css,
            jsCode = saasPreset.js,
            activeTab = CodeTab.INDEX_HTML,
            isAutoChain = true,
            showEndCta = true,
            target = AutoDurationTarget.SEC_30,
            manualSpeedLevel = 14,
            manualTurbo = false,
            hasPreviewShowcase = true,
            showcaseDurationMs = 8000L
        )
        assertTrue(
            "Estimated seconds with showcase (${metricsWithShowcase.estimatedTotalSeconds}) must be <= 30s",
            metricsWithShowcase.estimatedTotalSeconds <= 30
        )
    }
}

