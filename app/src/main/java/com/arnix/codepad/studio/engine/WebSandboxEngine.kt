package com.arnix.codepad.studio.engine

import com.arnix.codepad.studio.data.ProjectTemplates
import org.json.JSONObject

enum class PreviewScaleMode(val label: String, val scaleFactor: Float) {
    AUTO_FIT("Fit", 0f),
    SCALE_100("100%", 1.0f),
    SCALE_75("75%", 0.75f),
    SCALE_50("50%", 0.50f);

    fun next(): PreviewScaleMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

data class SplitHtmlResult(
    val cleanedHtml: String,
    val extractedCss: String,
    val extractedJs: String,
    val didSplit: Boolean
)

/**
 * Production-grade Web Sandbox & Bundler Engine for CodePad Studio and Auto Write Studio.
 * - Supports both 3-tab snippets (index.html + style.css + script.js) and full single-file HTML documents.
 * - Auto-detects popular CodePen/JSFiddle CDN libraries (FontAwesome, Boxicons, RemixIcon, Material Icons,
 *   Tailwind CDN, GSAP, jQuery, Three.js, Anime.js, Chart.js, Confetti, VanillaTilt, Swiper, Lucide, etc.).
 * - Normalizes SCSS-style `//` comments in CSS and hoists `@import` rules so Google Fonts always load.
 * - Sanitizes invisible clipboard characters (zero-width spaces) that cause false JS SyntaxErrors.
 * - Provides O(N) JS syntax completeness verification so mid-typing states never spam false errors.
 */
object WebSandboxEngine {

    private val zeroWidthRegex = Regex("""[\u200B\u200C\u200D\uFEFF]""")
    private val doctypeRegex = Regex("""(?i)<!doctype[^>]*>""")
    private val htmlTagOpenRegex = Regex("""(?i)<html\b([^>]*)>""")
    private val htmlTagCloseRegex = Regex("""(?i)</html>""")
    private val headBlockRegex = Regex("""(?i)<head\b[^>]*>([\s\S]*?)</head>""")
    private val bodyOpenRegex = Regex("""(?i)<body\b([^>]*)>""")
    private val bodyCloseRegex = Regex("""(?i)</body>""")
    private val inlineStyleBlockRegex = Regex("""(?i)<style\b[^>]*>([\s\S]*?)</style>""")
    private val inlineScriptBlockRegex = Regex("""(?i)<script\b([^>]*)>([\s\S]*?)</script>""")
    private val scriptSrcAttrRegex = Regex("""(?i)\bsrc\s*=""")
    private val scriptTypeAttrRegex = Regex("""(?i)\btype\s*=\s*["']([^"']+)["']""")
    private val cssImportRegex = Regex("""(?m)^\s*@import\s+[^;\n]+;?""")
    private val esModuleSyntaxRegex = Regex("""(?m)^\s*(?:import\s+[\s\S]+?from\s+["']|import\s+["']|export\s+(?:default|const|let|var|function|class|\{))""")

    private val presetCssSet: Set<String> by lazy {
        ProjectTemplates.PRESETS.map { it.css.trim() }.toSet()
    }

    private val presetCssSignatures: Map<String, List<String>> by lazy {
        mapOf(
            ProjectTemplates.PORTFOLIO_CSS.trim() to listOf("hero-title", "btn-hello", "btn-work", "status-text"),
            ProjectTemplates.CYBERPUNK_CSS.trim() to listOf("cyber-title", "cyber-status", "cyber-card"),
            ProjectTemplates.POMODORO_CSS.trim() to listOf("pomo-timer", "pomo-task", "pomo-card"),
            ProjectTemplates.CRYPTO_CSS.trim() to listOf("eth-price", "eth-gas", "swap-card"),
            ProjectTemplates.COUNTER_CSS.trim() to listOf("counter-title", "counter-status", "counter-card"),
            ProjectTemplates.SAAS_DASHBOARD_CSS.trim() to listOf("dash-title", "dash-sub", "dash-card")
        )
    }

    private val presetHtmlSet: Set<String> by lazy {
        ProjectTemplates.PRESETS.map { it.html.trim() }.toSet()
    }

    private val presetJsSignatures: Map<String, List<String>> by lazy {
        mapOf(
            ProjectTemplates.PORTFOLIO_JS.trim() to listOf("btn-hello", "btn-work", "sayHello", "showSkills"),
            ProjectTemplates.CYBERPUNK_JS.trim() to listOf("cyber-title", "cyber-status", "runBreachProtocol"),
            ProjectTemplates.POMODORO_JS.trim() to listOf("pomo-timer", "pomo-task", "completeSprint"),
            ProjectTemplates.CRYPTO_JS.trim() to listOf("eth-price", "eth-gas", "executeSwap"),
            ProjectTemplates.COUNTER_JS.trim() to listOf("counter-title", "counter-status", "addSession"),
            ProjectTemplates.SAAS_DASHBOARD_JS.trim() to listOf("dash-title", "dash-sub", "scaleCluster")
        )
    }

    /**
     * Removes invisible zero-width characters copied from blogs/online editors and normalizes line endings.
     */
    fun sanitizeCode(raw: String): String {
        if (raw.isEmpty()) return ""
        return raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(zeroWidthRegex, "")
            .replace('\u00A0', ' ')
    }

    /**
     * Detects if `index.html` contains embedded `<style>` or inline `<script>` blocks that can be split into tabs.
     */
    fun hasEmbeddedStyleOrScript(htmlCode: String): Boolean {
        if (htmlCode.isBlank()) return false
        if (inlineStyleBlockRegex.containsMatchIn(htmlCode)) return true
        return inlineScriptBlockRegex.findAll(htmlCode).any { match ->
            val attrs = match.groupValues[1]
            val body = match.groupValues[2].trim()
            !scriptSrcAttrRegex.containsMatchIn(attrs) && body.isNotEmpty()
        }
    }

    /**
     * Splits a single-file HTML document containing `<style>` and `<script>` into separate HTML, CSS, and JS strings.
     * Preserves external `<script src="...">` and `<link>` tags inside the HTML.
     */
    fun splitEmbeddedHtml(
        htmlCode: String,
        existingCss: String = "",
        existingJs: String = ""
    ): SplitHtmlResult {
        val cleanInput = sanitizeCode(htmlCode)
        val extractedStyles = mutableListOf<String>()
        val extractedScripts = mutableListOf<String>()

        var workingHtml = inlineStyleBlockRegex.replace(cleanInput) { match ->
            val styleBody = match.groupValues[1].trim()
            if (styleBody.isNotEmpty()) {
                extractedStyles.add(styleBody)
            }
            ""
        }

        workingHtml = inlineScriptBlockRegex.replace(workingHtml) { match ->
            val attrs = match.groupValues[1]
            val scriptBody = match.groupValues[2].trim()
            val typeAttr = scriptTypeAttrRegex.find(attrs)?.groupValues?.get(1)?.lowercase() ?: ""
            val isExecutableJs = !scriptSrcAttrRegex.containsMatchIn(attrs) &&
                (typeAttr.isEmpty() || typeAttr == "text/javascript" || typeAttr == "application/javascript" || typeAttr == "module")
            if (isExecutableJs && scriptBody.isNotEmpty()) {
                extractedScripts.add(scriptBody)
                ""
            } else {
                match.value
            }
        }

        if (extractedStyles.isEmpty() && extractedScripts.isEmpty()) {
            return SplitHtmlResult(
                cleanedHtml = htmlCode,
                existingCss,
                existingJs,
                didSplit = false
            )
        }

        // Keep any external <link> or <script src="..."> from <head>, and unwrap <html>/<body> if simple
        val headLinksAndScripts = mutableListOf<String>()
        headBlockRegex.findAll(workingHtml).forEach { headMatch ->
            val headInner = headMatch.groupValues[1]
            Regex("""(?i)<link\b[^>]*>|<script\b[^>]*\bsrc\s*=[^>]*>[\s\S]*?</script>""")
                .findAll(headInner)
                .forEach { tagMatch -> headLinksAndScripts.add(tagMatch.value.trim()) }
        }

        val bodyOpenMatch = bodyOpenRegex.find(workingHtml)
        val bodyCloseMatch = bodyCloseRegex.findAll(workingHtml).lastOrNull()
        val bodyAttrs = bodyOpenMatch?.groupValues?.get(1)?.trim() ?: ""

        var finalHtml = if (bodyOpenMatch != null && bodyCloseMatch != null && bodyCloseMatch.range.first > bodyOpenMatch.range.last) {
            val innerBody = workingHtml.substring(bodyOpenMatch.range.last + 1, bodyCloseMatch.range.first).trim()
            if (bodyAttrs.isNotEmpty()) {
                "<body $bodyAttrs>\n$innerBody\n</body>"
            } else {
                innerBody
            }
        } else {
            workingHtml
                .replace(doctypeRegex, "")
                .replace( headBlockRegex, "")
                .replace(htmlTagOpenRegex, "")
                .replace(htmlTagCloseRegex, "")
                .replace(bodyOpenRegex, "")
                .replace(bodyCloseRegex, "")
                .trim()
        }

        if (headLinksAndScripts.isNotEmpty()) {
            finalHtml = (headLinksAndScripts.joinToString("\n") + "\n\n" + finalHtml).trim()
        }

        val isExistingCssPreset = existingCss.trim() in presetCssSet
        val isExistingJsPreset = existingJs.trim() in presetJsSignatures.keys

        val mergedCss = when {
            extractedStyles.isEmpty() -> existingCss
            existingCss.isBlank() || isExistingCssPreset -> extractedStyles.joinToString("\n\n")
            else -> existingCss.trimEnd() + "\n\n" + extractedStyles.joinToString("\n\n")
        }

        val mergedJs = when {
            extractedScripts.isEmpty() -> existingJs
            existingJs.isBlank() || isExistingJsPreset -> extractedScripts.joinToString("\n\n")
            else -> existingJs.trimEnd() + "\n\n" + extractedScripts.joinToString("\n\n")
        }

        return SplitHtmlResult(
            cleanedHtml = finalHtml.replace(Regex("\n{3,}"), "\n\n").trim(),
            extractedCss = mergedCss,
            extractedJs = mergedJs,
            didSplit = true
        )
    }

    /**
     * Resolves effective (HTML, CSS, JS) by filtering out untouched template residue when a user
     * pastes custom code into index.html without clearing the default template's CSS/JS tabs.
     */
    fun resolveEffectiveCode(
        htmlCode: String,
        cssCode: String,
        jsCode: String
    ): Triple<String, String, String> {
        val cleanHtml = sanitizeCode(htmlCode)
        val cleanCss = sanitizeCode(cssCode)
        val cleanJs = sanitizeCode(jsCode)

        val trimmedHtml = cleanHtml.trim()
        val trimmedCss = cleanCss.trim()
        val trimmedJs = cleanJs.trim()

        val isCustomHtml = trimmedHtml.isNotEmpty() && trimmedHtml !in presetHtmlSet

        // If user pasted custom HTML, and style.css is still an untouched starter preset whose target IDs/classes
        // are absent from the custom HTML, skip the leftover starter preset CSS so it doesn't override their UI!
        val presetCssMarkers = presetCssSignatures[trimmedCss]
        val effectiveCss = if (isCustomHtml && presetCssMarkers != null && presetCssMarkers.none { cleanHtml.contains(it) }) {
            ""
        } else {
            cleanCss
        }

        // If user pasted custom HTML, and script.js is still an untouched starter preset whose target IDs/functions
        // are absent from the custom HTML, skip the leftover starter preset JS so it doesn't log or error.
        val presetMarkers = presetJsSignatures[trimmedJs]
        val effectiveJs = if (isCustomHtml && presetMarkers != null && presetMarkers.none { cleanHtml.contains(it) }) {
            ""
        } else {
            cleanJs
        }

        return Triple(cleanHtml, effectiveCss, effectiveJs)
    }

    /**
     * Converts standalone SCSS-style single-line `//` comments into valid `/* ... */` CSS comments
     * (without altering URLs like `https://` or strings) and hoists `@import` statements to the top.
     */
    fun normalizeUserCss(rawCss: String): String {
        val sanitized = sanitizeCode(rawCss)
        if (sanitized.isBlank()) return ""

        val sb = StringBuilder(sanitized.length + 32)
        var i = 0
        val len = sanitized.length
        var inSingleQuote = false
        var inDoubleQuote = false
        var inBlockComment = false
        var parenDepth = 0

        while (i < len) {
            val ch = sanitized[i]
            val next = if (i + 1 < len) sanitized[i + 1] else '\u0000'

            if (inBlockComment) {
                sb.append(ch)
                if (ch == '*' && next == '/') {
                    sb.append('/')
                    i += 2
                    inBlockComment = false
                    continue
                }
                i++
                continue
            }

            if (inSingleQuote) {
                sb.append(ch)
                if (ch == '\\' && i + 1 < len) {
                    sb.append(sanitized[i + 1])
                    i += 2
                    continue
                }
                if (ch == '\'') inSingleQuote = false
                i++
                continue
            }

            if (inDoubleQuote) {
                sb.append(ch)
                if (ch == '\\' && i + 1 < len) {
                    sb.append(sanitized[i + 1])
                    i += 2
                    continue
                }
                if (ch == '"') inDoubleQuote = false
                i++
                continue
            }

            when {
                ch == '\'' -> {
                    inSingleQuote = true
                    sb.append(ch)
                    i++
                }
                ch == '"' -> {
                    inDoubleQuote = true
                    sb.append(ch)
                    i++
                }
                ch == '(' -> {
                    parenDepth++
                    sb.append(ch)
                    i++
                }
                ch == ')' -> {
                    parenDepth = (parenDepth - 1).coerceAtLeast(0)
                    sb.append(ch)
                    i++
                }
                ch == '/' && next == '*' -> {
                    inBlockComment = true
                    sb.append("/*")
                    i += 2
                }
                ch == '/' && next == '/' && parenDepth == 0 -> {
                    val prevChar = if (i > 0) sanitized[i - 1] else '\n'
                    if (prevChar != ':') {
                        // Convert single-line // comment to /* ... */
                        val lineEnd = sanitized.indexOf('\n', i + 2).let { if (it < 0) len else it }
                        val commentContent = sanitized.substring(i + 2, lineEnd).replace("*/", "* /")
                        sb.append("/*").append(commentContent).append("*/")
                        i = lineEnd
                    } else {
                        sb.append(ch)
                        i++
                    }
                }
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }

        val commentNormalized = sb.toString()
        val imports = mutableListOf<String>()
        val withoutImports = cssImportRegex.replace(commentNormalized) { match ->
            val imp = match.value.trim().let { if (it.endsWith(";")) it else "$it;" }
            imports.add(imp)
            ""
        }

        return if (imports.isEmpty()) {
            commentNormalized
        } else {
            imports.joinToString("\n") + "\n" + withoutImports.trimStart()
        }
    }

    /**
     * O(N) lexer check that determines whether a JavaScript snippet is syntactically balanced
     * and ready to execute without throwing mid-typing `SyntaxError: Unexpected end of input`.
     */
    fun isJsSyntacticallyComplete(rawJs: String): Boolean {
        val js = sanitizeCode(rawJs).trim()
        if (js.isEmpty()) return true

        var braceDepth = 0
        var parenDepth = 0
        var bracketDepth = 0
        var inSingleQuote = false
        var inDoubleQuote = false
        var inTemplate = false
        var inLineComment = false
        var inBlockComment = false
        var i = 0
        val len = js.length

        while (i < len) {
            val ch = js[i]
            val next = if (i + 1 < len) js[i + 1] else '\u0000'

            if (inLineComment) {
                if (ch == '\n') inLineComment = false
                i++
                continue
            }

            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    inBlockComment = false
                    i += 2
                    continue
                }
                i++
                continue
            }

            if (inSingleQuote) {
                if (ch == '\\') {
                    i += 2
                    continue
                }
                if (ch == '\n') return false // Unclosed single-line string literal
                if (ch == '\'') inSingleQuote = false
                i++
                continue
            }

            if (inDoubleQuote) {
                if (ch == '\\') {
                    i += 2
                    continue
                }
                if (ch == '\n') return false // Unclosed double-line string literal
                if (ch == '"') inDoubleQuote = false
                i++
                continue
            }

            if (inTemplate) {
                if (ch == '\\') {
                    i += 2
                    continue
                }
                if (ch == '`') {
                    inTemplate = false
                }
                i++
                continue
            }

            when {
                ch == '/' && next == '/' -> {
                    inLineComment = true
                    i += 2
                }
                ch == '/' && next == '*' -> {
                    inBlockComment = true
                    i += 2
                }
                ch == '\'' -> {
                    inSingleQuote = true
                    i++
                }
                ch == '"' -> {
                    inDoubleQuote = true
                    i++
                }
                ch == '`' -> {
                    inTemplate = true
                    i++
                }
                ch == '{' -> {
                    braceDepth++
                    i++
                }
                ch == '}' -> {
                    braceDepth--
                    if (braceDepth < 0) return false
                    i++
                }
                ch == '(' -> {
                    parenDepth++
                    i++
                }
                ch == ')' -> {
                    parenDepth--
                    if (parenDepth < 0) return false
                    i++
                }
                ch == '[' -> {
                    bracketDepth++
                    i++
                }
                ch == ']' -> {
                    bracketDepth--
                    if (bracketDepth < 0) return false
                    i++
                }
                else -> i++
            }
        }

        if (inSingleQuote || inDoubleQuote || inTemplate || inBlockComment) return false
        if (braceDepth != 0 || parenDepth != 0 || bracketDepth != 0) return false

        // Strip trailing comments to inspect the final meaningful token
        val codeWithoutComments = js
            .replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .replace(Regex("""(?m)//[^\n]*$"""), "")
            .trim()

        if (codeWithoutComments.isEmpty()) return true

        // Check for dangling binary/member operators at the very end
        if (codeWithoutComments.endsWith("++") || codeWithoutComments.endsWith("--")) {
            return true
        }
        val lastChar = codeWithoutComments.last()
        if (lastChar in charArrayOf('=', '+', '-', '*', '/', '%', '&', '|', '^', '<', '>', '!', '?', ':', '.', ',', '\\')) {
            return false
        }

        // Check if code ends with a bare keyword that requires a following expression/block
        val trailingWord = codeWithoutComments.takeLastWhile { it.isLetterOrDigit() || it == '_' || it == '$' }
        if (trailingWord in setOf(
                "const", "let", "var", "function", "class", "new", "typeof", "void",
                "delete", "await", "yield", "case", "extends", "import", "export",
                "from", "return", "throw", "if", "else", "for", "while", "switch", "try", "catch", "finally", "do"
            )
        ) {
            return false
        }

        return true
    }

    /**
     * Auto-detects external CSS/JS libraries frequently enabled in CodePen / JSFiddle settings
     * so copied snippets work out of the box without missing icon fonts or `ReferenceError: gsap is not defined`.
     */
    private fun detectAutoCdnTags(html: String, css: String, js: String): List<String> {
        val combinedHtmlCss = "$html\n$css"
        val lowerHtml = html.lowercase()
        val tags = mutableListOf<String>()

        // 1. FontAwesome Icons
        if (!lowerHtml.contains("font-awesome") && !lowerHtml.contains("fontawesome") &&
            Regex("""\b(?:fa-(?:solid|regular|brands|light|thin|duotone|sharp|[a-z0-9-]+)|fas|far|fab|fal|fad)\b""").containsMatchIn(combinedHtmlCss)
        ) {
            tags.add("""<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" />""")
        }

        // 2. Boxicons
        if (!lowerHtml.contains("boxicons") &&
            Regex("""\bbx(?:s|l)?-[a-z0-9-]+\b""").containsMatchIn(combinedHtmlCss)
        ) {
            tags.add("""<link rel="stylesheet" href="https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css" />""")
        }

        // 3. RemixIcon
        if (!lowerHtml.contains("remixicon") &&
            Regex("""\bri-[a-z0-9-]+-(?:line|fill)\b""").containsMatchIn(combinedHtmlCss)
        ) {
            tags.add("""<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/remixicon@4.2.0/fonts/remixicon.css" />""")
        }

        // 4. Bootstrap Icons
        if (!lowerHtml.contains("bootstrap-icons") &&
            Regex("""\bbi-[a-z0-9-]+\b""").containsMatchIn(combinedHtmlCss)
        ) {
            tags.add("""<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" />""")
        }

        // 5. Google Material Icons / Symbols
        if (!lowerHtml.contains("fonts.googleapis.com/icon") && !lowerHtml.contains("material-symbols")) {
            if (combinedHtmlCss.contains("material-icons")) {
                tags.add("""<link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons|Material+Icons+Outlined|Material+Icons+Round" />""")
            }
            if (combinedHtmlCss.contains("material-symbols-outlined") || combinedHtmlCss.contains("material-symbols-rounded")) {
                tags.add("""<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0&family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,0,0" />""")
            }
        }

        // 6. Tailwind CSS CDN (when HTML uses multiple Tailwind utility classes and CSS doesn't define them)
        if (!lowerHtml.contains("tailwindcss")) {
            val tailwindHits = Regex("""\b(?:bg-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose|black|white)|text-(?:xs|sm|base|lg|xl|2xl|3xl|4xl|white|slate|gray)|rounded-(?:md|lg|xl|2xl|3xl|full)|flex-(?:col|row|1)|items-(?:center|start|end|stretch)|justify-(?:center|between|around|evenly)|px-\d+|py-\d+|gap-\d+|shadow-(?:md|lg|xl|2xl)|backdrop-blur)\b""")
                .findAll(html).count()
            if (tailwindHits >= 4) {
                tags.add("""<script src="https://cdn.tailwindcss.com"></script>""")
            }
        }

        val combinedJs = "$html\n$js"

        // 7. GSAP + ScrollTrigger
        if (!lowerHtml.contains("gsap") &&
            Regex("""\b(?:gsap|TweenMax|TweenLite|TimelineMax|TimelineLite)\b""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/gsap.min.js"></script>""")
            if (combinedJs.contains("ScrollTrigger")) {
                tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/ScrollTrigger.min.js"></script>""")
            }
        }

        // 8. jQuery
        if (!lowerHtml.contains("jquery") &&
            Regex("""(?:\$\s*\(\s*["'a-zA-Z0-9_#.<]|jQuery\s*\(|\$\.(?:ajax|get|post|each|fn)\b)""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>""")
        }

        // 9. Anime.js
        if (!lowerHtml.contains("anime.min.js") && !lowerHtml.contains("animejs") &&
            Regex("""\banime(?:\s*\(|\s*\.\s*timeline\b|\s*\.\s*stagger\b)""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/animejs/3.2.2/anime.min.js"></script>""")
        }

        // 10. Three.js
        if (!lowerHtml.contains("three.min.js") && !lowerHtml.contains("three.module") &&
            Regex("""\bTHREE\s*\.""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>""")
        }

        // 11. Chart.js
        if (!lowerHtml.contains("chart.js") && !lowerHtml.contains("chart.umd") &&
            Regex("""\bnew\s+Chart\s*\(|\bChart\.register\b""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.2/dist/chart.umd.min.js"></script>""")
        }

        // 12. Canvas Confetti
        if (!lowerHtml.contains("confetti") &&
            Regex("""\bconfetti\s*\(""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdn.jsdelivr.net/npm/canvas-confetti@1.9.2/dist/confetti.browser.min.js"></script>""")
        }

        // 13. VanillaTilt
        if (!lowerHtml.contains("vanilla-tilt") &&
            (combinedJs.contains("VanillaTilt") || html.contains("data-tilt"))
        ) {
            tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/vanilla-tilt/1.8.1/vanilla-tilt.min.js"></script>""")
        }

        // 14. Swiper
        if (!lowerHtml.contains("swiper") &&
            (Regex("""\bnew\s+Swiper\s*\(""").containsMatchIn(combinedJs) || html.contains("swiper-wrapper"))
        ) {
            tags.add("""<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swiper@11/swiper-bundle.min.css" />""")
            tags.add("""<script src="https://cdn.jsdelivr.net/npm/swiper@11/swiper-bundle.min.js"></script>""")
        }

        // 15. Lucide Icons
        if (!lowerHtml.contains("lucide") &&
            (combinedJs.contains("lucide.") || html.contains("data-lucide"))
        ) {
            tags.add("""<script src="https://unpkg.com/lucide@latest"></script>""")
        }

        // 16. Particles.js
        if (!lowerHtml.contains("particles") &&
            Regex("""\bparticlesJS\s*\(""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdn.jsdelivr.net/npm/particles.js@2.0.0/particles.min.js"></script>""")
        }

        // 17. Matter.js
        if (!lowerHtml.contains("matter") &&
            Regex("""\bMatter\.(?:Engine|Render|World|Bodies|Composite)\b""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/matter-js/0.19.0/matter.min.js"></script>""")
        }

        // 18. D3.js
        if (!lowerHtml.contains("d3.") && !lowerHtml.contains("d3js") &&
            Regex("""\bd3\.(?:select|selectAll|scaleLinear|arc|pie|csv|json)\b""").containsMatchIn(combinedJs)
        ) {
            tags.add("""<script src="https://cdnjs.cloudflare.com/ajax/libs/d3/7.9.0/d3.min.js"></script>""")
        }

        return tags
    }

    /**
     * Builds the complete HTML5 document for WebView execution.
     * Supports both 3-tab snippets and full single-file HTML documents.
     */
    fun buildFullHtmlDocument(
        rawHtmlCode: String,
        rawCssCode: String,
        rawJsCode: String,
        runJs: Boolean = true,
        scaleMode: PreviewScaleMode = PreviewScaleMode.AUTO_FIT
    ): String {
        val (effectiveHtml, effectiveCss, effectiveJs) = resolveEffectiveCode(
            htmlCode = rawHtmlCode,
            cssCode = rawCssCode,
            jsCode = rawJsCode
        )

        val normalizedCss = normalizeUserCss(effectiveCss)
        val htmlTagAttrs = htmlTagOpenRegex.find(effectiveHtml)?.groupValues?.get(1)?.trim() ?: """lang="en""""
        val bodyTagAttrs = bodyOpenRegex.find(effectiveHtml)?.groupValues?.get(1)?.trim() ?: ""

        // Extract any <head> content from effectiveHtml if the user pasted a full document
        val extractedHeadContent = StringBuilder()
        headBlockRegex.findAll(effectiveHtml).forEach { match ->
            val inner = match.groupValues[1]
                // Remove duplicate charset/viewport meta tags since we provide standard ones
                .replace(Regex("""(?i)<meta\b[^>]*charset[^>]*>"""), "")
                .replace(Regex("""(?i)<meta\b[^>]*name\s*=\s*["']viewport["'][^>]*>"""), "")
                .trim()
            if (inner.isNotEmpty()) {
                extractedHeadContent.append(inner).append("\n")
            }
        }

        // Extract body inner content (or use effectiveHtml stripped of outer html/head/body wrappers)
        var bodyContent = run {
            val withoutDoctypeAndHead = effectiveHtml
                .replace(doctypeRegex, "")
                .replace(headBlockRegex, "")
            val bodyOpen = bodyOpenRegex.find(withoutDoctypeAndHead)
            val bodyClose = bodyCloseRegex.findAll(withoutDoctypeAndHead).lastOrNull()
            if (bodyOpen != null && bodyClose != null && bodyClose.range.first > bodyOpen.range.last) {
                withoutDoctypeAndHead.substring(bodyOpen.range.last + 1, bodyClose.range.first).trim()
            } else {
                withoutDoctypeAndHead
                    .replace(htmlTagOpenRegex, "")
                    .replace(htmlTagCloseRegex, "")
                    .replace(bodyOpenRegex, "")
                    .replace(bodyCloseRegex, "")
                    .trim()
            }
        }

        // If runJs is false (e.g., while typing HTML/CSS in AutoWrite before JS is ready),
        // strip inline executable <script> tags from bodyContent so half-typed inline scripts don't error.
        if (!runJs) {
            bodyContent = inlineScriptBlockRegex.replace(bodyContent) { match ->
                val attrs = match.groupValues[1]
                if (scriptSrcAttrRegex.containsMatchIn(attrs)) match.value else ""
            }
        }

        val autoCdnTags = detectAutoCdnTags(effectiveHtml, normalizedCss, effectiveJs)
        val isEsModule = esModuleSyntaxRegex.containsMatchIn(effectiveJs)
        val userScriptType = if (isEsModule) """type="module"""" else ""
        val jsToInject = if (runJs && effectiveJs.isNotBlank() && isJsSyntacticallyComplete(effectiveJs)) {
            effectiveJs
        } else {
            ""
        }

        // Safe escaping of </script> inside user JS
        val safeJs = jsToInject.replace(Regex("""(?i)</script>"""), "<\\/script>")
        val safeCss = normalizedCss.replace(Regex("""(?i)</style>"""), "<\\/style>")

        return """
<!DOCTYPE html>
<html $htmlTagAttrs>
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes" />
  <style id="__codepad_base_css__">
    *, *::before, *::after {
      box-sizing: border-box;
    }
    html {
      -webkit-text-size-adjust: 100%;
      text-size-adjust: 100%;
    }
    body {
      margin: 0;
      padding: 0;
      background-color: #ffffff;
      color: #0f172a;
      font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
      -webkit-font-smoothing: antialiased;
    }
    img, svg, video, canvas {
      max-width: 100%;
    }
  </style>
  <script>
    (function() {
      // 1. Safe in-memory Storage shim if WebView restricts localStorage/sessionStorage
      function createMemoryStorage() {
        var store = Object.create(null);
        return {
          getItem: function(k) { return Object.prototype.hasOwnProperty.call(store, k) ? store[k] : null; },
          setItem: function(k, v) { store[String(k)] = String(v); },
          removeItem: function(k) { delete store[String(k)]; },
          clear: function() { store = Object.create(null); },
          key: function(i) { var keys = Object.keys(store); return keys[i] || null; },
          get length() { return Object.keys(store).length; }
        };
      }
      try {
        var testKey = '__codepad_storage_test__';
        window.localStorage.setItem(testKey, '1');
        window.localStorage.removeItem(testKey);
      } catch (e) {
        try {
          var memLocal = createMemoryStorage();
          var memSession = createMemoryStorage();
          Object.defineProperty(window, 'localStorage', { get: function() { return memLocal; }, configurable: true });
          Object.defineProperty(window, 'sessionStorage', { get: function() { return memSession; }, configurable: true });
        } catch (_) {}
      }

      // 2. Timer & Animation Frame Registry for clean live reloads without leaks
      var activeIntervals = [];
      var activeTimeouts = [];
      var activeRafs = [];
      var origSetInterval = window.setInterval.bind(window);
      var origClearInterval = window.clearInterval.bind(window);
      var origSetTimeout = window.setTimeout.bind(window);
      var origClearTimeout = window.clearTimeout.bind(window);
      var origRaf = (window.requestAnimationFrame || function(cb) { return origSetTimeout(cb, 16); }).bind(window);
      var origCancelRaf = (window.cancelAnimationFrame || function(id) { origClearTimeout(id); }).bind(window);

      window.setInterval = function(fn, delay) {
        var args = Array.prototype.slice.call(arguments);
        var id = origSetInterval.apply(window, args);
        activeIntervals.push(id);
        return id;
      };
      window.setTimeout = function(fn, delay) {
        var args = Array.prototype.slice.call(arguments);
        var id = origSetTimeout.apply(window, args);
        activeTimeouts.push(id);
        return id;
      };
      window.requestAnimationFrame = function(cb) {
        var id = origRaf(cb);
        activeRafs.push(id);
        return id;
      };
      window.__codepadCleanupTimers = function() {
        while (activeIntervals.length) { origClearInterval(activeIntervals.pop()); }
        while (activeTimeouts.length) { origClearTimeout(activeTimeouts.pop()); }
        while (activeRafs.length) { origCancelRaf(activeRafs.pop()); }
      };

      // 3. Console & Error Bridge to Android Integrated Console Panel
      function formatArg(val, depth) {
        depth = depth || 0;
        if (val === null) return 'null';
        if (val === undefined) return 'undefined';
        if (typeof val === 'string') return depth === 0 ? val : JSON.stringify(val);
        if (typeof val === 'number' || typeof val === 'boolean' || typeof val === 'bigint' || typeof val === 'symbol') {
          return String(val);
        }
        if (val instanceof Error) {
          return val.stack || (val.name + ': ' + val.message);
        }
        if (typeof Element !== 'undefined' && val instanceof Element) {
          var idPart = val.id ? ('#' + val.id) : '';
          var clsPart = (val.className && typeof val.className === 'string')
            ? ('.' + val.className.trim().split(/\s+/).join('.'))
            : '';
          return '<' + val.tagName.toLowerCase() + idPart + clsPart + '>';
        }
        if (typeof val === 'function') {
          return 'ƒ ' + (val.name || 'anonymous') + '()';
        }
        try {
          var seen = [];
          return JSON.stringify(val, function(k, v) {
            if (typeof v === 'object' && v !== null) {
              if (seen.indexOf(v) !== -1) return '[Circular]';
              seen.push(v);
            }
            if (typeof v === 'function') return 'ƒ ' + (v.name || 'anonymous') + '()';
            return v;
          });
        } catch (err) {
          return Object.prototype.toString.call(val);
        }
      }

      function sendToBridge(level, args, source, stack) {
        try {
          var msg = Array.prototype.slice.call(args).map(function(a) { return formatArg(a, 0); }).join(' ');
          if (window.CodePadBridge && typeof window.CodePadBridge.postLog === 'function') {
            window.CodePadBridge.postLog(level, msg, source || 'script.js', stack || '');
          }
        } catch (_) {}
      }

      var origConsole = {
        log: console.log ? console.log.bind(console) : function() {},
        info: console.info ? console.info.bind(console) : function() {},
        warn: console.warn ? console.warn.bind(console) : function() {},
        error: console.error ? console.error.bind(console) : function() {},
        debug: console.debug ? console.debug.bind(console) : function() {},
        clear: console.clear ? console.clear.bind(console) : function() {}
      };

      console.log = function() { sendToBridge('LOG', arguments, 'script.js', ''); origConsole.log.apply(console, arguments); };
      console.info = function() { sendToBridge('INFO', arguments, 'script.js', ''); origConsole.info.apply(console, arguments); };
      console.debug = function() { sendToBridge('LOG', arguments, 'script.js', ''); origConsole.debug.apply(console, arguments); };
      console.warn = function() { sendToBridge('WARN', arguments, 'script.js', ''); origConsole.warn.apply(console, arguments); };
      console.error = function() {
        var firstErr = Array.prototype.slice.call(arguments).find(function(a) { return a instanceof Error; });
        sendToBridge('ERROR', arguments, 'script.js', firstErr && firstErr.stack ? firstErr.stack : '');
        origConsole.error.apply(console, arguments);
      };
      console.table = function(data) { sendToBridge('LOG', [data], 'script.js', ''); };
      console.dir = function(data) { sendToBridge('LOG', [data], 'script.js', ''); };
      console.assert = function(cond) {
        if (!cond) {
          var rest = Array.prototype.slice.call(arguments, 1);
          sendToBridge('ERROR', ['Assertion failed:'].concat(rest), 'script.js', '');
        }
      };
      console.clear = function() {
        try {
          if (window.CodePadBridge && typeof window.CodePadBridge.clearConsole === 'function') {
            window.CodePadBridge.clearConsole();
          }
        } catch (_) {}
        origConsole.clear();
      };

      window.addEventListener('error', function(event) {
        var msg = event && event.message ? String(event.message) : '';
        if (!msg || msg.indexOf('ResizeObserver loop') !== -1 || msg === 'Script error.') {
          return;
        }
        var line = (event && event.lineno) ? event.lineno : 1;
        var col = (event && event.colno) ? event.colno : 1;
        var src = 'script.js:' + line + ':' + col;
        var stack = (event && event.error && event.error.stack) ? String(event.error.stack) : (msg + '\n    at ' + src);
        if (window.CodePadBridge && typeof window.CodePadBridge.postLog === 'function') {
          window.CodePadBridge.postLog('ERROR', msg, 'script.js:' + line, stack);
        }
      });

      window.addEventListener('unhandledrejection', function(event) {
        var reason = event ? event.reason : 'Promise rejected';
        var msg = (reason instanceof Error) ? (reason.name + ': ' + reason.message) : formatArg(reason, 0);
        var stack = (reason instanceof Error && reason.stack) ? reason.stack : '';
        if (window.CodePadBridge && typeof window.CodePadBridge.postLog === 'function') {
          window.CodePadBridge.postLog('ERROR', 'Uncaught (in promise) ' + msg, 'script.js', stack);
        }
      });

      // 4. Non-blocking alert / confirm / prompt shims that also log to Console
      window.alert = function(msg) {
        sendToBridge('INFO', ['[alert] ' + String(msg !== undefined ? msg : '')], 'window.alert', '');
      };
      window.confirm = function(msg) {
        sendToBridge('INFO', ['[confirm] ' + String(msg !== undefined ? msg : '') + ' -> true'], 'window.confirm', '');
        return true;
      };
      window.prompt = function(msg, defVal) {
        var res = defVal !== undefined ? String(defVal) : '';
        sendToBridge('INFO', ['[prompt] ' + String(msg !== undefined ? msg : '') + ' -> ' + res], 'window.prompt', '');
        return res;
      };

      // 5. DOMContentLoaded & window.load readiness compatibility shim
      var origDocAddEvent = document.addEventListener.bind(document);
      document.addEventListener = function(type, listener, options) {
        if (type === 'DOMContentLoaded' && (document.readyState === 'interactive' || document.readyState === 'complete')) {
          if (typeof listener === 'function') {
            origSetTimeout(function() {
              try { listener(new Event('DOMContentLoaded')); } catch (e) { console.error(e); }
            }, 0);
          }
          return;
        }
        return origDocAddEvent(type, listener, options);
      };

      var origWinAddEvent = window.addEventListener.bind(window);
      window.addEventListener = function(type, listener, options) {
        if (type === 'load' && document.readyState === 'complete') {
          if (typeof listener === 'function') {
            origSetTimeout(function() {
              try { listener(new Event('load')); } catch (e) { console.error(e); }
            }, 0);
          }
          return;
        }
        return origWinAddEvent(type, listener, options);
      };

      // 6. Touch-to-Mouse Event Bridge for Desktop CodePen Snippets (Canvas, 3D Tilt, Sliders)
      function dispatchSyntheticMouse(type, touch, target) {
        if (!touch || !target) return;
        try {
          var evt = new MouseEvent(type, {
            bubbles: true,
            cancelable: true,
            view: window,
            clientX: touch.clientX,
            clientY: touch.clientY,
            screenX: touch.screenX,
            screenY: touch.screenY,
            button: 0,
            buttons: type === 'mouseup' ? 0 : 1
          });
          target.dispatchEvent(evt);
        } catch (_) {}
      }
      origDocAddEvent('touchstart', function(e) {
        if (e.touches && e.touches.length === 1) {
          var t = e.touches[0];
          var el = document.elementFromPoint(t.clientX, t.clientY) || e.target;
          if (el && (el.tagName === 'CANVAS' || el.hasAttribute('data-tilt'))) {
            dispatchSyntheticMouse('mouseenter', t, el);
            dispatchSyntheticMouse('mousedown', t, el);
          }
        }
      }, { passive: true });
      origDocAddEvent('touchmove', function(e) {
        if (e.touches && e.touches.length === 1) {
          var t = e.touches[0];
          var el = document.elementFromPoint(t.clientX, t.clientY) || e.target;
          dispatchSyntheticMouse('mousemove', t, el);
        }
      }, { passive: true });
      origDocAddEvent('touchend', function(e) {
        if (e.changedTouches && e.changedTouches.length === 1) {
          var t = e.changedTouches[0];
          var el = document.elementFromPoint(t.clientX, t.clientY) || e.target;
          if (el && (el.tagName === 'CANVAS' || el.hasAttribute('data-tilt'))) {
            dispatchSyntheticMouse('mouseup', t, el);
          }
        }
      }, { passive: true });

      // 7. Smart Viewport Scale & Overflow Guard (prevents 100vh flex clipping & fits wide desktop UIs)
      window.__codepadScaleMode = '${scaleMode.name}';
      window.__codepadApplyLayoutGuard = function(modeName) {
        if (modeName) window.__codepadScaleMode = modeName;
        var mode = window.__codepadScaleMode || 'AUTO_FIT';
        var body = document.body;
        var html = document.documentElement;
        if (!body || !html) return;

        // Reset any previous zoom to measure natural layout
        body.style.zoom = '';
        var cs = window.getComputedStyle(body);
        var hasCanvas = !!body.querySelector('canvas');

        // Fix CodePen `body { height: 100vh; overflow: hidden; display: flex; align-items: center; }` clipping
        // on compact mobile preview viewports when content is taller than the preview panel:
        if (!hasCanvas) {
          if (cs.overflowY === 'hidden' || cs.overflow === 'hidden') {
            body.style.overflowY = 'auto';
            body.style.overflowX = 'auto';
          }
          if (body.scrollHeight > window.innerHeight + 4 && cs.display.indexOf('flex') !== -1 && cs.alignItems === 'center') {
            body.style.height = 'auto';
            body.style.minHeight = '100vh';
            body.style.paddingTop = Math.max(12, parseFloat(cs.paddingTop) || 0) + 'px';
            body.style.paddingBottom = Math.max(12, parseFloat(cs.paddingBottom) || 0) + 'px';
          }
        }

        if (mode === 'SCALE_100') {
          body.style.zoom = '1';
        } else if (mode === 'SCALE_75') {
          body.style.zoom = '0.75';
        } else if (mode === 'SCALE_50') {
          body.style.zoom = '0.5';
        } else if (mode === 'AUTO_FIT') {
          var vw = Math.max(1, window.innerWidth);
          var sw = Math.max(vw, body.scrollWidth, html.scrollWidth);
          if (sw > vw + 8 && !hasCanvas) {
            var fitRatio = Math.max(0.5, Math.min(1.0, (vw - 8) / sw));
            body.style.zoom = String(fitRatio);
          } else {
            body.style.zoom = '1';
          }
        }
      };

      // 8. 60fps Zero-Flicker Live Updater for Auto Write Studio & Live CSS Edits
      window.__codepadUpdateCssOnly = function(newCss) {
        var styleEl = document.getElementById('__codepad_live_style__');
        if (!styleEl) {
          styleEl = document.createElement('style');
          styleEl.id = '__codepad_live_style__';
          document.head.appendChild(styleEl);
        }
        if (styleEl.textContent !== newCss) {
          styleEl.textContent = newCss;
        }
        window.__codepadApplyLayoutGuard();
      };

      window.__codepadUpdateDomAndCss = function(newBodyHtml, newCss) {
        window.__codepadUpdateCssOnly(newCss);
        if (document.body && document.body.innerHTML !== newBodyHtml) {
          document.body.innerHTML = newBodyHtml;
        }
        window.__codepadApplyLayoutGuard();
      };

      // 9. In-App Canvas & Video Recording Helper (MediaRecorder)
      window.__codepadCanvasRecorder = null;
      window.__codepadRecordedChunks = [];
      window.__startCanvasRecord = function(fps) {
        try {
          var canvas = document.querySelector('canvas');
          if (!canvas) {
            console.info('[ScreenCapture] No <canvas> detected in preview to stream directly.');
            return false;
          }
          var stream = canvas.captureStream ? canvas.captureStream(fps || 60) : null;
          if (!stream) {
            console.warn('[ScreenCapture] canvas.captureStream is not supported in this WebView.');
            return false;
          }
          window.__codepadRecordedChunks = [];
          var mime = (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported('video/webm;codecs=vp9')) ? 'video/webm;codecs=vp9' : 'video/webm';
          window.__codepadCanvasRecorder = new MediaRecorder(stream, { mimeType: mime });
          window.__codepadCanvasRecorder.ondataavailable = function(e) {
            if (e.data && e.data.size > 0) window.__codepadRecordedChunks.push(e.data);
          };
          window.__codepadCanvasRecorder.start(100);
          console.info('[ScreenCapture] Canvas video recording started at ' + (fps || 60) + ' FPS.');
          return true;
        } catch (err) {
          console.error('[ScreenCapture] Failed to start canvas record:', err);
          return false;
        }
      };

      window.__stopCanvasRecord = function() {
        try {
          if (!window.__codepadCanvasRecorder) return;
          window.__codepadCanvasRecorder.onstop = function() {
            var blob = new Blob(window.__codepadRecordedChunks, { type: 'video/webm' });
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = 'codepad_canvas_recording.webm';
            document.body.appendChild(a);
            a.click();
            setTimeout(function() {
              document.body.removeChild(a);
              URL.revokeObjectURL(url);
            }, 1000);
            console.info('[ScreenCapture] Canvas recording saved and downloaded successfully.');
          };
          window.__codepadCanvasRecorder.stop();
        } catch (err) {
          console.error('[ScreenCapture] Failed to stop canvas record:', err);
        }
      };

      origWinAddEvent('load', function() {
        window.__codepadApplyLayoutGuard();
        if (window.lucide && typeof window.lucide.createIcons === 'function') {
          try { window.lucide.createIcons(); } catch (_) {}
        }
      });
      origWinAddEvent('resize', function() {
        window.__codepadApplyLayoutGuard();
      });
    })();
  </script>
  ${autoCdnTags.joinToString("\n  ")}
  $extractedHeadContent
  <style id="__codepad_live_style__">
$safeCss
  </style>
</head>
<body $bodyTagAttrs>
$bodyContent
${if (safeJs.isNotBlank()) """  <script $userScriptType id="__codepad_user_script__">
$safeJs
  </script>""" else ""}
  <script>
    if (window.__codepadApplyLayoutGuard) {
      window.__codepadApplyLayoutGuard();
    }
  </script>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Escapes a Kotlin String into a safe JavaScript string literal (including surrounding quotes).
     */
    fun toJsStringLiteral(value: String): String {
        return JSONObject.quote(value)
    }
}
