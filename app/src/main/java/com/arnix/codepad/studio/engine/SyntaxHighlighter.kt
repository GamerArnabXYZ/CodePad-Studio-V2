package com.arnix.codepad.studio.engine

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.arnix.codepad.studio.data.CodeTab
import com.arnix.codepad.studio.ui.theme.EditorColorPalette
import com.arnix.codepad.studio.ui.theme.LightEditorPalette

/**
 * Pre-computed per-line syntax-highlighted structure for O(1) per-character Auto Write rendering
 * even on very large / multi-hundred-line files.
 */
@Immutable
class PrecomputedHighlightedCode(
    val fullText: String,
    val lines: List<AnnotatedString>,
    val lineStartOffsets: IntArray
) {
    /**
     * Finds the 0-based active line index for [typedCharCount] in O(log N) time via binary search.
     */
    fun findActiveLineIndex(typedCharCount: Int): Int {
        if (lines.isEmpty()) return 0
        val clamped = typedCharCount.coerceIn(0, fullText.length)
        val idx = lineStartOffsets.binarySearch(clamped)
        return if (idx >= 0) {
            idx.coerceIn(0, lines.lastIndex)
        } else {
            val insertionPoint = -idx - 1
            (insertionPoint - 1).coerceIn(0, lines.lastIndex)
        }
    }

    /**
     * Slices only the single active line [lineIndex] up to [typedCharCount] in O(1) time.
     */
    fun sliceActiveLine(lineIndex: Int, typedCharCount: Int): AnnotatedString {
        if (lines.isEmpty()) return AnnotatedString("")
        val safeIdx = lineIndex.coerceIn(0, lines.lastIndex)
        val fullLine = lines[safeIdx]
        val lineStart = lineStartOffsets[safeIdx]
        val charsInLine = (typedCharCount - lineStart).coerceIn(0, fullLine.length)
        if (charsInLine >= fullLine.length) return fullLine
        if (charsInLine <= 0) return AnnotatedString("")
        return fullLine.subSequence(0, charsInLine)
    }

    /**
     * Returns (completedLines, activeLineSlice) in O(log N) time with zero list copying.
     */
    fun sliceAt(typedCharCount: Int): Pair<List<AnnotatedString>, AnnotatedString> {
        if (lines.isEmpty()) return emptyList<AnnotatedString>() to AnnotatedString("")
        val activeIdx = findActiveLineIndex(typedCharCount)
        val completed = if (activeIdx > 0) lines.subList(0, activeIdx) else emptyList()
        val activeSlice = sliceActiveLine(activeIdx, typedCharCount)
        return completed to activeSlice
    }
}

private data class HighlightCacheKey(
    val code: String,
    val tab: CodeTab,
    val palette: EditorColorPalette
)

object SyntaxHighlighter {

    private val htmlTagRegex = Regex("""</?[a-zA-Z][a-zA-Z0-9-]*|/?>""")
    private val htmlAttrRegex = Regex("""\b([a-zA-Z-]+)(?=\s*=)""")
    private val cssSelectorRegex = Regex("""(?m)^([^{};\n]+)(?=\s*\{)""")
    private val cssPropertyRegex = Regex("""\b([a-zA-Z-]+)(?=\s*:)""")
    private val jsKeywordRegex = Regex(
        """\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|new|throw|try|catch|finally|typeof|instanceof|true|false|null|undefined|async|await|class|extends|this|console|document|window|Error|Math|JSON)\b"""
    )
    private val functionCallRegex = Regex("""\b([a-zA-Z_$][a-zA-Z0-9_$]*)(?=\s*\()""")
    private val numberAndHexRegex = Regex("""#[0-9a-fA-F]{3,8}\b|\b\d+(?:\.\d+)?(?:px|rem|em|vh|vw|%|s|ms)?\b""")
    private val stringOrCommentRegex = Regex("""<!--[\s\S]*?-->|/\*[\s\S]*?\*/|(?<!:)//[^\n]*|"[^"\n]*"|'[^'\n]*'|`[^`]*`""")
    private val unclosedHtmlTagRegex = Regex("""</?[a-zA-Z][a-zA-Z0-9-]*$""")

    private const val MAX_CACHE_ENTRIES = 12

    private val highlightCache = object : LinkedHashMap<HighlightCacheKey, AnnotatedString>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<HighlightCacheKey, AnnotatedString>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    private val precomputedCache = object : LinkedHashMap<HighlightCacheKey, PrecomputedHighlightedCode>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<HighlightCacheKey, PrecomputedHighlightedCode>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    fun highlight(
        code: String,
        tab: CodeTab,
        palette: EditorColorPalette = LightEditorPalette
    ): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        val key = HighlightCacheKey(code, tab, palette)
        synchronized(highlightCache) {
            highlightCache[key]?.let { return it }
        }

        val computed = buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = palette.plainText), 0, code.length)

            when (tab) {
                CodeTab.INDEX_HTML -> {
                    htmlTagRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.tag, fontWeight = FontWeight.SemiBold),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                    unclosedHtmlTagRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.tag, fontWeight = FontWeight.SemiBold),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                    htmlAttrRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.attribute, fontWeight = FontWeight.Medium),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                    numberAndHexRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.number),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                }

                CodeTab.STYLE_CSS -> {
                    cssSelectorRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.function, fontWeight = FontWeight.SemiBold),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                    cssPropertyRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.attribute, fontWeight = FontWeight.Medium),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                    numberAndHexRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.number, fontWeight = FontWeight.Medium),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                }

                CodeTab.SCRIPT_JS -> {
                    numberAndHexRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.number),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                    functionCallRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.function, fontWeight = FontWeight.Medium),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                    jsKeywordRegex.findAll(code).forEach { match ->
                        addStyle(
                            SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                }
            }

            stringOrCommentRegex.findAll(code).forEach { match ->
                val token = match.value
                val isString = token.startsWith("\"") || token.startsWith("'") || token.startsWith("`")
                if (isString) {
                    addStyle(
                        SpanStyle(color = palette.string),
                        match.range.first,
                        match.range.last + 1
                    )
                } else {
                    addStyle(
                        SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }

            // O(N) unclosed block comment check (avoids O(N^2) negative-lookahead regex backtracking on large code)
            val lastHtmlCommentOpen = code.lastIndexOf("<!--")
            if (lastHtmlCommentOpen >= 0 && code.indexOf("-->", lastHtmlCommentOpen + 4) < 0) {
                addStyle(
                    SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic),
                    lastHtmlCommentOpen,
                    code.length
                )
            }
            val lastBlockCommentOpen = code.lastIndexOf("/*")
            if (lastBlockCommentOpen >= 0 && code.indexOf("*/", lastBlockCommentOpen + 2) < 0) {
                addStyle(
                    SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic),
                    lastBlockCommentOpen,
                    code.length
                )
            }
        }

        synchronized(highlightCache) {
            highlightCache[key] = computed
        }
        return computed
    }

    /**
     * Pre-computes all highlighted lines and their starting character offsets ONCE per tab.
     * Uses a bounded multi-tab LRU cache so AnimatedContent transitions between HTML, CSS, and JS
     * never thrash or recompute syntax highlighting.
     */
    fun precomputeLines(
        code: String,
        tab: CodeTab,
        palette: EditorColorPalette = LightEditorPalette
    ): PrecomputedHighlightedCode {
        if (code.isEmpty()) {
            return PrecomputedHighlightedCode(
                fullText = "",
                lines = listOf(AnnotatedString("")),
                lineStartOffsets = intArrayOf(0)
            )
        }

        val key = HighlightCacheKey(code, tab, palette)
        synchronized(precomputedCache) {
            precomputedCache[key]?.let { return it }
        }

        val fullAnnotated = highlight(code, tab, palette)
        val lines = ArrayList<AnnotatedString>(64)
        val offsets = ArrayList<Int>(64)
        var lineStart = 0

        for (i in code.indices) {
            if (code[i] == '\n') {
                offsets.add(lineStart)
                lines.add(fullAnnotated.subSequence(lineStart, i))
                lineStart = i + 1
            }
        }
        if (lineStart <= code.length) {
            offsets.add(lineStart)
            lines.add(fullAnnotated.subSequence(lineStart, code.length))
        }

        val precomputed = PrecomputedHighlightedCode(
            fullText = code,
            lines = lines,
            lineStartOffsets = offsets.toIntArray()
        )
        synchronized(precomputedCache) {
            precomputedCache[key] = precomputed
        }
        return precomputed
    }
}

class SyntaxHighlightVisualTransformation(
    private val tab: CodeTab,
    private val palette: EditorColorPalette
) : VisualTransformation {
    private var lastRawText: String? = null
    private var lastTransformed: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val cached = lastTransformed
        if (raw == lastRawText && cached != null) {
            return cached
        }
        val fresh = TransformedText(
            text = SyntaxHighlighter.highlight(raw, tab, palette),
            offsetMapping = OffsetMapping.Identity
        )
        lastRawText = raw
        lastTransformed = fresh
        return fresh
    }
}
