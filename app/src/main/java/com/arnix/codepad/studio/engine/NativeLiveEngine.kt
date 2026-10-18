package com.arnix.codepad.studio.engine

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class LogLevel(val badge: String) {
    LOG("LOG"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERR"),
    SYSTEM("SYS")
}

data class ConsoleLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date()),
    val level: LogLevel,
    val message: String,
    val sourceLocation: String = "runtime",
    val stackTrace: String? = null
)

data class ComputedNodeStyle(
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
    val borderColor: Color? = null,
    val borderRadiusDp: Float = 0f,
    val paddingDp: Float = 0f,
    val fontSizeSp: Float? = null,
    val isBold: Boolean = false
)

data class NativeDomNode(
    val uid: String,
    val tag: String,
    val id: String = "",
    val classes: List<String> = emptyList(),
    val text: String = "",
    val onClickFunction: String? = null,
    val progressValue: Float? = null,
    val progressMax: Float = 100f,
    val placeholder: String = "",
    val inputType: String = "text",
    val inputValue: String = "",
    val style: ComputedNodeStyle = ComputedNodeStyle(),
    val children: List<NativeDomNode> = emptyList()
)

data class JsFunctionDef(
    val name: String,
    val startLine: Int,
    val bodyLines: List<Pair<Int, String>>
)

data class NativeRuntimeSnapshot(
    val rootNodes: List<NativeDomNode>,
    val bodyStyle: ComputedNodeStyle,
    val functions: Map<String, JsFunctionDef>,
    val variables: Map<String, String>,
    val logs: List<ConsoleLogEntry>
)

/**
 * Native HTML + CSS + JS AST/DOM Evaluator & Fallback Engine.
 * Works alongside WebSandboxEngine (real Chromium WebView) and provides synchronous
 * DOM/CSS/JS evaluation for instant state snapshots and JVM/Robolectric unit tests.
 */
object NativeLiveEngine {

    private val cssCommentRegex = Regex("""/\*[\s\S]*?\*/""")
    private val cssKeyframesRegex = Regex("""(?i)@(?:-[a-z]+-)?keyframes\s+[^{]+\{(?:[^{}]*\{[^}]*\})*[^}]*\}""")
    private val cssFontFaceRegex = Regex("""(?i)@font-face\s*\{[^}]*\}""")
    private val cssMediaWrapperRegex = Regex("""(?i)@(?:media|supports|layer)[^{]+\{([\s\S]*?)\}\s*\}""")
    private val cssVarDeclRegex = Regex("""(--[a-zA-Z0-9_-]+)\s*:\s*([^;\}]+)""")
    private val cssVarUseRegex = Regex("""var\(\s*(--[a-zA-Z0-9_-]+)(?:\s*,\s*([^)]+))?\s*\)""")
    private val cssBlockRegex = Regex("""([^{}@]+)\{([^}]*)\}""")
    private val htmlCommentRegex = Regex("""<!--[\s\S]*?-->""")
    private val htmlStyleExtractRegex = Regex("""(?i)<style\b[^>]*>([\s\S]*?)</style>""")
    private val htmlScriptExtractRegex = Regex("""(?i)<script\b([^>]*)>([\s\S]*?)</script>""")
    private val htmlHeadOrScriptRegex = Regex("""(?i)<(head|style|script|title)\b[^>]*>[\s\S]*?</\1>""")
    private val htmlDoctypeRegex = Regex("""(?i)<!doctype[^>]*>""")
    private val htmlTokenRegex = Regex("""(<(?:!--[\s\S]*?--|/?[a-zA-Z0-9!_-]+(?:"[^"]*"|'[^']*'|[^'">])*)>|[^<]+)""")
    private val htmlAttrRegex = Regex("""([a-zA-Z0-9_:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+))""")
    private val whitespaceSplitRegex = Regex("\\s+")
    private val jsFnMatchRegex = Regex("""^(?:export\s+)?(?:async\s+)?function\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\([^)]*\)\s*\{?""")
    private val jsArrowFnMatchRegex = Regex(
        """^(?:export\s+)?(?:const|let|var)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*(?:async\s*)?(?:\([^)]*\)|[a-zA-Z_$][a-zA-Z0-9_$]*)\s*=>\s*\{?"""
    )
    private val jsThrowRegex = Regex("""^throw\s+(?:new\s+[a-zA-Z0-9_$]*Error\s*\(\s*)?(.+?)\)?$""")
    private val jsConsoleRegex = Regex("""^console\.(log|info|warn|error|debug|table|dir)\s*\(([\s\S]*)\)$""")
    private val jsDomTextRegex = Regex(
        """^(?:window\.)?document\.(?:getElementById|querySelector)\(\s*["']#?([^"']+)["']\s*\)\.(?:innerText|textContent|innerHTML|value)\s*=\s*(.+)$"""
    )
    private val jsDomColorRegex = Regex(
        """^(?:window\.)?document\.(?:(?:getElementById|querySelector)\(\s*["']#?([^"']+)["']\s*\)|(body))\.style\.color\s*=\s*(.+)$"""
    )
    private val jsDomBgColorRegex = Regex(
        """^(?:window\.)?document\.(?:(?:getElementById|querySelector)\(\s*["']#?([^"']+)["']\s*\)|(body))\.style\.(?:backgroundColor|background)\s*=\s*(.+)$"""
    )
    private val jsIncDecRegex = Regex("""^(?:([a-zA-Z_$][a-zA-Z0-9_$]*)\s*(\+\+|--)|(\+\+|--)\s*([a-zA-Z_$][a-zA-Z0-9_$]*))$""")
    private val jsCompoundAssignRegex = Regex("""^([a-zA-Z_$][a-zA-Z0-9_$]*)\s*(\+=|-=|\*=|/=)\s*(.+)$""")
    private val jsVarRegex = Regex("""^(?:(?:let|const|var)\s+)?([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*([^=].*)$""")
    private val jsDirectCallRegex = Regex("""^([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\(([^)]*)\)$""")
    private val templateVarRegex = Regex("""\$\{\s*([^}]+)\s*\}""")
    private val rgbColorRegex = Regex("""rgba?\(\s*(\d{1,3})\s*[\s,]\s*(\d{1,3})\s*[\s,]\s*(\d{1,3})""")
    private val hslColorRegex = Regex("""hsla?\(\s*(\d+(?:\.\d+)?)(?:deg)?\s*[\s,]\s*(\d+(?:\.\d+)?)%\s*[\s,]\s*(\d+(?:\.\d+)?)%""")
    private val eightDigitHexRegex = Regex("""#([0-9a-fA-F]{8})\b""")
    private val sixDigitHexRegex = Regex("""#([0-9a-fA-F]{6})\b""")
    private val threeDigitHexRegex = Regex("""#([0-9a-fA-F]{3})\b""")
    private val numberPxRegex = Regex("""(-?\d+(?:\.\d+)?)""")

    private val voidHtmlTags = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
        "path", "circle", "rect", "line", "polyline", "polygon", "ellipse", "stop", "use"
    )

    private var cachedCssInput: String? = null
    private var cachedCssStyleMap: Map<String, ComputedNodeStyle> = emptyMap()

    fun normalizeProgressiveHtml(html: String): String {
        if (html.isEmpty()) return ""
        val lastOpenBracket = html.lastIndexOf('<')
        val lastCloseBracket = html.lastIndexOf('>')
        return if (lastOpenBracket > lastCloseBracket) {
            html.substring(0, lastOpenBracket).trimEnd()
        } else {
            html
        }
    }

    fun evaluateProject(
        html: String,
        css: String,
        js: String,
        existingVariables: Map<String, String> = emptyMap(),
        domTextOverrides: Map<String, String> = emptyMap(),
        domColorOverrides: Map<String, Color> = emptyMap(),
        domBgOverrides: Map<String, Color> = emptyMap(),
        runTopLevelJs: Boolean = true
    ): NativeRuntimeSnapshot {
        val (resolvedHtml, resolvedCss, resolvedJs) = WebSandboxEngine.resolveEffectiveCode(html, css, js)

        // Extract any inline <style> or <script> blocks embedded inside index.html so single-file HTML snippets work
        val inlineStyles = htmlStyleExtractRegex.findAll(resolvedHtml)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        val inlineScripts = htmlScriptExtractRegex.findAll(resolvedHtml)
            .mapNotNull { match ->
                val attrs = match.groupValues[1]
                val body = match.groupValues[2].trim()
                if (!attrs.contains("src=", ignoreCase = true) && body.isNotEmpty()) body else null
            }
            .joinToString("\n")

        val combinedCss = listOf(inlineStyles, resolvedCss).filter { it.isNotBlank() }.joinToString("\n")
        val combinedJs = listOf(inlineScripts, resolvedJs).filter { it.isNotBlank() }.joinToString("\n")

        val logs = mutableListOf<ConsoleLogEntry>()
        val styleMap = parseCssRules(combinedCss)
        val defaultBodyStyle = ComputedNodeStyle(
            backgroundColor = Color(0xFFFFFFFF),
            textColor = Color(0xFF0F172A),
            paddingDp = 12f
        )
        val htmlRootStyle = styleMap["html"] ?: ComputedNodeStyle()
        val bodyRuleStyle = styleMap["body"] ?: ComputedNodeStyle()
        val bodyStyle = mergeStyles(mergeStyles(defaultBodyStyle, htmlRootStyle), bodyRuleStyle)

        val rawNodes = parseHtmlNodes(
            html = resolvedHtml,
            styleMap = styleMap,
            domTextOverrides = domTextOverrides,
            domColorOverrides = domColorOverrides,
            domBgOverrides = domBgOverrides
        )

        val variables = existingVariables.toMutableMap()
        val functions = mutableMapOf<String, JsFunctionDef>()
        val mutableTextOverrides = domTextOverrides.toMutableMap()
        val mutableColorOverrides = domColorOverrides.toMutableMap()
        val mutableBgOverrides = domBgOverrides.toMutableMap()

        parseAndRunJs(
            js = combinedJs,
            executeTopLevel = runTopLevelJs,
            variables = variables,
            functions = functions,
            textOverrides = mutableTextOverrides,
            colorOverrides = mutableColorOverrides,
            bgOverrides = mutableBgOverrides,
            logs = logs
        )

        val finalNodes = if (
            mutableTextOverrides != domTextOverrides ||
            mutableColorOverrides != domColorOverrides ||
            mutableBgOverrides != domBgOverrides
        ) {
            parseHtmlNodes(
                html = resolvedHtml,
                styleMap = styleMap,
                domTextOverrides = mutableTextOverrides,
                domColorOverrides = mutableColorOverrides,
                domBgOverrides = mutableBgOverrides
            )
        } else {
            rawNodes
        }

        val finalBodyStyle = bodyStyle.copy(
            backgroundColor = mutableBgOverrides["body"] ?: bodyStyle.backgroundColor,
            textColor = mutableColorOverrides["body"] ?: bodyStyle.textColor
        )

        return NativeRuntimeSnapshot(
            rootNodes = finalNodes,
            bodyStyle = finalBodyStyle,
            functions = functions,
            variables = variables,
            logs = logs
        )
    }

    fun invokeJsFunction(
        functionName: String,
        functions: Map<String, JsFunctionDef>,
        variables: MutableMap<String, String>,
        textOverrides: MutableMap<String, String>,
        colorOverrides: MutableMap<String, Color>,
        bgOverrides: MutableMap<String, Color> = mutableMapOf()
    ): List<ConsoleLogEntry> {
        val logs = mutableListOf<ConsoleLogEntry>()
        val cleanName = functionName.substringBefore("(").trim()
        val fn = functions[cleanName]
        if (fn == null) {
            logs.add(
                ConsoleLogEntry(
                    level = LogLevel.ERROR,
                    message = "ReferenceError: $cleanName is not defined",
                    sourceLocation = "index.html:onclick",
                    stackTrace = "ReferenceError: $cleanName is not defined\n    at HTMLButtonElement.onclick (index.html:1:1)"
                )
            )
            return logs
        }

        for ((lineNum, rawStmt) in fn.bodyLines) {
            val stmt = rawStmt.trim()
            if (stmt.isEmpty() || stmt.startsWith("//")) continue
            val halted = executeSingleStatement(
                stmt = stmt,
                lineNum = lineNum,
                scopeName = "$cleanName()",
                variables = variables,
                functions = functions,
                textOverrides = textOverrides,
                colorOverrides = colorOverrides,
                bgOverrides = bgOverrides,
                logs = logs
            )
            if (halted) break
        }
        return logs
    }

    @Synchronized
    private fun parseCssRules(css: String): Map<String, ComputedNodeStyle> {
        if (css == cachedCssInput) {
            return cachedCssStyleMap
        }
        val result = mutableMapOf<String, ComputedNodeStyle>()
        val normalized = WebSandboxEngine.normalizeUserCss(css)
            .replace(cssCommentRegex, "")
            .replace(cssKeyframesRegex, "")
            .replace(cssFontFaceRegex, "")

        // Extract CSS custom properties (--var-name: value)
        val cssVars = mutableMapOf<String, String>()
        cssVarDeclRegex.findAll(normalized).forEach { m ->
            cssVars[m.groupValues[1].trim()] = m.groupValues[2].trim()
        }

        // Unwrap simple @media blocks so responsive rules still contribute styles
        val unwrapped = cssMediaWrapperRegex.replace(normalized) { m -> m.groupValues[1] + "}" }

        val openBraces = unwrapped.count { it == '{' }
        val closeBraces = unwrapped.count { it == '}' }
        val cleaned = if (openBraces > closeBraces) {
            unwrapped + "}".repeat(openBraces - closeBraces)
        } else {
            unwrapped
        }

        cssBlockRegex.findAll(cleaned).forEach { match ->
            val selectorGroup = match.groupValues[1].trim()
            val declarations = match.groupValues[2].trim()
            val computed = parseCssDeclarations(declarations, cssVars)

            selectorGroup.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { rawSel ->
                // Normalize pseudo-classes and descendant selectors to also index the target token
                val baseSel = rawSel.substringBefore(":").trim()
                if (baseSel.isNotEmpty()) {
                    val prev = result[baseSel] ?: ComputedNodeStyle()
                    result[baseSel] = mergeStyles(prev, computed)
                    val lastSegment = baseSel.split(whitespaceSplitRegex).lastOrNull()?.trim() ?: ""
                    if (lastSegment.isNotEmpty() && lastSegment != baseSel) {
                        val prevLast = result[lastSegment] ?: ComputedNodeStyle()
                        result[lastSegment] = mergeStyles(prevLast, computed)
                    }
                }
            }
        }
        cachedCssInput = css
        cachedCssStyleMap = result
        return result
    }

    private fun resolveCssVariables(rawValue: String, cssVars: Map<String, String>): String {
        if (!rawValue.contains("var(")) return rawValue
        return cssVarUseRegex.replace(rawValue) { m ->
            val varName = m.groupValues[1].trim()
            val fallback = m.groupValues[2].trim()
            cssVars[varName] ?: fallback.ifEmpty { rawValue }
        }
    }

    private fun parseCssDeclarations(
        declarations: String,
        cssVars: Map<String, String>
    ): ComputedNodeStyle {
        var bg: Color? = null
        var fg: Color? = null
        var border: Color? = null
        var radius = 0f
        var padding = 0f
        var fontSize: Float? = null
        var isBold = false

        declarations.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { decl ->
            val parts = decl.split(":", limit = 2)
            if (parts.size == 2) {
                val prop = parts[0].trim().lowercase()
                val rawVal = parts[1].trim().removeSuffix("!important").trim()
                val value = resolveCssVariables(rawVal, cssVars)
                if (value.isNotEmpty()) {
                    when (prop) {
                        "background-color", "background" -> parseHexColor(value)?.let { bg = it }
                        "color" -> parseHexColor(value)?.let { fg = it }
                        "border-color", "border" -> parseHexColor(value)?.let { border = it }
                        "border-radius" -> {
                            val num = parseNumberPxOrNull(value)
                            if (num != null) {
                                radius = if (value.contains("rem", ignoreCase = true) || value.contains("em", ignoreCase = true)) {
                                    num * 16f
                                } else {
                                    num
                                }
                            }
                        }
                        "padding", "padding-top", "padding-bottom", "padding-left", "padding-right" -> {
                            val num = parseNumberPxOrNull(value)
                            if (num != null) {
                                padding = if (value.contains("rem", ignoreCase = true) || value.contains("em", ignoreCase = true)) {
                                    num * 16f
                                } else {
                                    num
                                }
                            }
                        }
                        "font-size" -> {
                            val parsed = parseNumberPxOrNull(value)
                            if (parsed != null && parsed > 0f) {
                                val pxVal = if (value.contains("rem", ignoreCase = true) || value.contains("em", ignoreCase = true)) {
                                    parsed * 16f
                                } else {
                                    parsed
                                }
                                if (pxVal >= 8f || value.contains("px", true) || value.contains("rem", true) || value.contains("em", true) || value.contains("sp", true)) {
                                    fontSize = pxVal.coerceIn(8f, 48f)
                                }
                            }
                        }
                        "font-weight" -> isBold = value.contains("bold", ignoreCase = true) ||
                            value == "700" || value == "600" || value == "800" || value == "900"
                    }
                }
            }
        }
        return ComputedNodeStyle(
            backgroundColor = bg,
            textColor = fg,
            borderColor = border,
            borderRadiusDp = radius,
            paddingDp = padding,
            fontSizeSp = fontSize,
            isBold = isBold
        )
    }

    private fun mergeStyles(base: ComputedNodeStyle, override: ComputedNodeStyle): ComputedNodeStyle {
        return ComputedNodeStyle(
            backgroundColor = override.backgroundColor ?: base.backgroundColor,
            textColor = override.textColor ?: base.textColor,
            borderColor = override.borderColor ?: base.borderColor,
            borderRadiusDp = if (override.borderRadiusDp > 0f) override.borderRadiusDp else base.borderRadiusDp,
            paddingDp = if (override.paddingDp > 0f) override.paddingDp else base.paddingDp,
            fontSizeSp = override.fontSizeSp ?: base.fontSizeSp,
            isBold = override.isBold || base.isBold
        )
    }

    private fun parseHtmlNodes(
        html: String,
        styleMap: Map<String, ComputedNodeStyle>,
        domTextOverrides: Map<String, String>,
        domColorOverrides: Map<String, Color>,
        domBgOverrides: Map<String, Color>
    ): List<NativeDomNode> {
        val withoutComments = html
            .replace(htmlCommentRegex, "")
            .replace(htmlDoctypeRegex, "")
            .replace(htmlHeadOrScriptRegex, "")
            .trim()
        val lastOpenBracket = withoutComments.lastIndexOf('<')
        val lastCloseBracket = withoutComments.lastIndexOf('>')
        val cleaned = if (lastOpenBracket > lastCloseBracket) {
            withoutComments.substring(0, lastOpenBracket).trim()
        } else {
            withoutComments
        }
        if (cleaned.isEmpty()) return emptyList()

        val stack = ArrayDeque<MutableDomBuilder>()
        val rootBuilder = MutableDomBuilder(uid = "root", tag = "root")
        stack.addLast(rootBuilder)

        var nodeCounter = 0
        htmlTokenRegex.findAll(cleaned).forEach { match ->
            val token = match.value.trim()
            if (token.isEmpty()) return@forEach

            if (token.startsWith("<!") || token.startsWith("<?")) {
                return@forEach
            } else if (token.startsWith("</")) {
                val closeTag = token.removePrefix("</").removeSuffix(">").trim().lowercase()
                if (closeTag == "html" || closeTag == "body" || closeTag == "head") {
                    return@forEach
                }
                val matchIdx = stack.indexOfLast { it.tag == closeTag }
                if (matchIdx > 0) {
                    while (stack.size > matchIdx) {
                        val finished = stack.removeLast().build(styleMap, domTextOverrides, domColorOverrides, domBgOverrides)
                        stack.last().children.add(finished)
                    }
                }
            } else if (token.startsWith("<")) {
                nodeCounter++
                val selfClosing = token.endsWith("/>")
                val inner = token.removePrefix("<").removeSuffix("/>").removeSuffix(">").trim()
                val tag = inner.takeWhile { !it.isWhitespace() && it != '/' }.lowercase()
                if (tag.isEmpty() || tag == "html" || tag == "body" || tag == "head") {
                    return@forEach
                }
                val attrPart = inner.drop(tag.length).trim()
                val attrs = parseAttributes(attrPart)
                val inlineStyle = attrs["style"]?.let { parseCssDeclarations(it, emptyMap()) }

                val builder = MutableDomBuilder(
                    uid = "node_$nodeCounter",
                    tag = tag,
                    id = attrs["id"] ?: "",
                    classes = (attrs["class"] ?: "").split(whitespaceSplitRegex).filter { it.isNotEmpty() },
                    onClickFunction = attrs["onclick"],
                    progressValue = attrs["value"]?.toFloatOrNull(),
                    progressMax = attrs["max"]?.toFloatOrNull() ?: 100f,
                    placeholder = attrs["placeholder"] ?: "",
                    inputType = attrs["type"] ?: "text",
                    inputValue = attrs["value"] ?: "",
                    inlineStyle = inlineStyle
                )

                val isVoid = selfClosing || tag in voidHtmlTags
                if (isVoid) {
                    stack.last().children.add(
                        builder.build(styleMap, domTextOverrides, domColorOverrides, domBgOverrides)
                    )
                } else {
                    stack.addLast(builder)
                }
            } else {
                val decoded = decodeHtmlEntities(token)
                val current = stack.last()
                current.textBuffer.append(if (current.textBuffer.isEmpty()) decoded else " $decoded")
            }
        }

        while (stack.size > 1) {
            val unclosed = stack.removeLast()
            stack.last().children.add(
                unclosed.build(styleMap, domTextOverrides, domColorOverrides, domBgOverrides)
            )
        }

        return rootBuilder.children
    }

    private fun decodeHtmlEntities(raw: String): String {
        if (!raw.contains('&')) return raw
        return raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&bull;", "•")
            .replace("&copy;", "©")
            .replace("&rarr;", "→")
            .replace("&larr;", "←")
            .replace("&times;", "×")
            .replace("&divide;", "÷")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
    }

    private class MutableDomBuilder(
        val uid: String,
        val tag: String,
        val id: String = "",
        val classes: List<String> = emptyList(),
        val onClickFunction: String? = null,
        val progressValue: Float? = null,
        val progressMax: Float = 100f,
        val placeholder: String = "",
        val inputType: String = "text",
        val inputValue: String = "",
        val inlineStyle: ComputedNodeStyle? = null
    ) {
        val textBuffer = StringBuilder()
        val children = mutableListOf<NativeDomNode>()

        fun build(
            styleMap: Map<String, ComputedNodeStyle>,
            domTextOverrides: Map<String, String>,
            domColorOverrides: Map<String, Color>,
            domBgOverrides: Map<String, Color>
        ): NativeDomNode {
            var style = ComputedNodeStyle()
            styleMap["*"]?.let { style = mergeStyles(style, it) }
            styleMap[tag]?.let { style = mergeStyles(style, it) }
            classes.forEach { cls ->
                styleMap[".$cls"]?.let { style = mergeStyles(style, it) }
                styleMap["$tag.$cls"]?.let { style = mergeStyles(style, it) }
            }
            if (id.isNotEmpty()) {
                styleMap["#$id"]?.let { style = mergeStyles(style, it) }
                styleMap["$tag#$id"]?.let { style = mergeStyles(style, it) }
            }
            inlineStyle?.let { style = mergeStyles(style, it) }
            if (id.isNotEmpty()) {
                domColorOverrides[id]?.let { overrideColor ->
                    style = style.copy(textColor = overrideColor)
                }
                domBgOverrides[id]?.let { overrideBg ->
                    style = style.copy(backgroundColor = overrideBg)
                }
            }
            val finalText = when {
                id.isNotEmpty() && domTextOverrides.containsKey(id) -> domTextOverrides[id] ?: textBuffer.toString()
                classes.any { domTextOverrides.containsKey(it) } -> {
                    val key = classes.first { domTextOverrides.containsKey(it) }
                    domTextOverrides[key] ?: textBuffer.toString()
                }
                else -> textBuffer.toString()
            }

            return NativeDomNode(
                uid = uid,
                tag = tag,
                id = id,
                classes = classes,
                text = finalText,
                onClickFunction = onClickFunction,
                progressValue = progressValue,
                progressMax = progressMax,
                placeholder = placeholder,
                inputType = inputType,
                inputValue = if (id.isNotEmpty() && domTextOverrides.containsKey(id)) finalText else inputValue,
                style = style,
                children = children.toList()
            )
        }
    }

    private fun parseAttributes(attrString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        htmlAttrRegex.findAll(attrString).forEach { m ->
            val key = m.groupValues[1].lowercase()
            val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            map[key] = value
        }
        return map
    }

    private fun parseAndRunJs(
        js: String,
        executeTopLevel: Boolean,
        variables: MutableMap<String, String>,
        functions: MutableMap<String, JsFunctionDef>,
        textOverrides: MutableMap<String, String>,
        colorOverrides: MutableMap<String, Color>,
        bgOverrides: MutableMap<String, Color>,
        logs: MutableList<ConsoleLogEntry>
    ) {
        val cleanedJs = js.replace(cssCommentRegex, "")
        val lines = cleanedJs.lines()
        var currentFnName: String? = null
        var currentFnStart = 1
        var braceDepth = 0
        val currentFnBody = mutableListOf<Pair<Int, String>>()

        // First pass: register all top-level function declarations (JS hoisting)
        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue

            if (currentFnName == null) {
                val fnMatch = jsFnMatchRegex.find(trimmed) ?: jsArrowFnMatchRegex.find(trimmed)
                if (fnMatch != null) {
                    currentFnName = fnMatch.groupValues[1]
                    currentFnStart = lineNum
                    currentFnBody.clear()
                    braceDepth = countBracesOutsideStrings(trimmed)
                    if (braceDepth <= 0 && trimmed.endsWith("}")) {
                        functions[currentFnName] = JsFunctionDef(currentFnName, currentFnStart, emptyList())
                        currentFnName = null
                    }
                }
            } else {
                braceDepth += countBracesOutsideStrings(trimmed)
                if (braceDepth <= 0) {
                    val beforeBrace = trimmed.removeSuffix(";").removeSuffix("}").trim()
                    if (beforeBrace.isNotEmpty()) {
                        currentFnBody.add(lineNum to beforeBrace)
                    }
                    functions[currentFnName] = JsFunctionDef(
                        name = currentFnName,
                        startLine = currentFnStart,
                        bodyLines = currentFnBody.toList()
                    )
                    currentFnName = null
                } else {
                    currentFnBody.add(lineNum to trimmed)
                }
            }
        }

        if (!executeTopLevel) return

        // Second pass: execute top-level statements
        var skipDepth = 0
        var insideFn = false
        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue

            if (!insideFn) {
                val fnMatch = jsFnMatchRegex.find(trimmed) ?: jsArrowFnMatchRegex.find(trimmed)
                if (fnMatch != null) {
                    val delta = countBracesOutsideStrings(trimmed)
                    if (delta > 0) {
                        insideFn = true
                        skipDepth = delta
                    }
                    continue
                }

                val halted = executeSingleStatement(
                    stmt = trimmed,
                    lineNum = lineNum,
                    scopeName = "<anonymous>",
                    variables = variables,
                    functions = functions,
                    textOverrides = textOverrides,
                    colorOverrides = colorOverrides,
                    bgOverrides = bgOverrides,
                    logs = logs
                )
                if (halted) break
            } else {
                skipDepth += countBracesOutsideStrings(trimmed)
                if (skipDepth <= 0) {
                    insideFn = false
                    skipDepth = 0
                }
            }
        }
    }

    private fun countBracesOutsideStrings(line: String): Int {
        var delta = 0
        var inSingle = false
        var inDouble = false
        var inBacktick = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '\\') {
                i += 2
                continue
            }
            when {
                inSingle -> if (ch == '\'') inSingle = false
                inDouble -> if (ch == '"') inDouble = false
                inBacktick -> if (ch == '`') inBacktick = false
                ch == '\'' -> inSingle = true
                ch == '"' -> inDouble = true
                ch == '`' -> inBacktick = true
                ch == '/' && i + 1 < line.length && line[i + 1] == '/' -> break
                ch == '{' -> delta++
                ch == '}' -> delta--
            }
            i++
        }
        return delta
    }

    private fun executeSingleStatement(
        stmt: String,
        lineNum: Int,
        scopeName: String,
        variables: MutableMap<String, String>,
        functions: Map<String, JsFunctionDef>,
        textOverrides: MutableMap<String, String>,
        colorOverrides: MutableMap<String, Color>,
        bgOverrides: MutableMap<String, Color>,
        logs: MutableList<ConsoleLogEntry>
    ): Boolean {
        val clean = stmt.removeSuffix(";").trim()
        if (clean.isEmpty() || clean == "{" || clean == "}" || clean.startsWith("//")) return false

        // 1. Check throw new Error(...)
        val throwMatch = jsThrowRegex.find(clean)
        if (throwMatch != null) {
            val rawMsg = evaluateExpression(throwMatch.groupValues[1], variables)
            logs.add(
                ConsoleLogEntry(
                    level = LogLevel.ERROR,
                    message = "Uncaught $rawMsg",
                    sourceLocation = "script.js:$lineNum",
                    stackTrace = "Uncaught $rawMsg\n    at $scopeName (script.js:$lineNum:5)"
                )
            )
            return true
        }

        // 2. Check console.log / info / warn / error / debug / table / dir
        val consoleMatch = jsConsoleRegex.find(clean)
        if (consoleMatch != null) {
            val method = consoleMatch.groupValues[1]
            val argExpr = consoleMatch.groupValues[2]
            val evaluated = evaluateMultiArgs(argExpr, variables)
            val level = when (method) {
                "info" -> LogLevel.INFO
                "warn" -> LogLevel.WARN
                "error" -> LogLevel.ERROR
                else -> LogLevel.LOG
            }
            logs.add(
                ConsoleLogEntry(
                    level = level,
                    message = evaluated,
                    sourceLocation = "script.js:$lineNum",
                    stackTrace = if (level == LogLevel.ERROR) {
                        "ConsoleError: $evaluated\n    at $scopeName (script.js:$lineNum:1)"
                    } else null
                )
            )
            return false
        }

        // 3. Check document.getElementById/querySelector("...").innerText / textContent / innerHTML / value = ...
        val domTextMatch = jsDomTextRegex.find(clean)
        if (domTextMatch != null) {
            val targetSelector = domTextMatch.groupValues[1].removePrefix("#").removePrefix(".")
            val newValue = evaluateExpression(domTextMatch.groupValues[2], variables)
            textOverrides[targetSelector] = newValue
            return false
        }

        // 4. Check document.getElementById/querySelector("...").style.color = ...
        val domColorMatch = jsDomColorRegex.find(clean)
        if (domColorMatch != null) {
            val targetId = domColorMatch.groupValues[1].ifEmpty { domColorMatch.groupValues[2] }.removePrefix("#").removePrefix(".")
            val colorStr = evaluateExpression(domColorMatch.groupValues[3], variables)
            parseHexColor(colorStr)?.let { colorOverrides[targetId] = it }
            return false
        }

        // 4b. Check document.getElementById/querySelector("...").style.backgroundColor = ...
        val domBgMatch = jsDomBgColorRegex.find(clean)
        if (domBgMatch != null) {
            val targetId = domBgMatch.groupValues[1].ifEmpty { domBgMatch.groupValues[2] }.removePrefix("#").removePrefix(".")
            val colorStr = evaluateExpression(domBgMatch.groupValues[3], variables)
            parseHexColor(colorStr)?.let { bgOverrides[targetId] = it }
            return false
        }

        // 5a. Check increment / decrement (count++ or ++count)
        val incDecMatch = jsIncDecRegex.find(clean)
        if (incDecMatch != null) {
            val varName = incDecMatch.groupValues[1].ifEmpty { incDecMatch.groupValues[4] }
            val op = incDecMatch.groupValues[2].ifEmpty { incDecMatch.groupValues[3] }
            val currentVal = variables[varName]?.toDoubleOrNull() ?: 0.0
            val nextVal = if (op == "++") currentVal + 1.0 else currentVal - 1.0
            variables[varName] = if (nextVal % 1.0 == 0.0) nextVal.toInt().toString() else nextVal.toString()
            return false
        }

        // 5b. Check compound assignment (count += 5, count -= 1, count *= 2, count /= 2)
        val compoundMatch = jsCompoundAssignRegex.find(clean)
        if (compoundMatch != null) {
            val varName = compoundMatch.groupValues[1]
            val op = compoundMatch.groupValues[2]
            val rhs = evaluateExpression(compoundMatch.groupValues[3], variables)
            val lhsNum = variables[varName]?.toDoubleOrNull()
            val rhsNum = rhs.toDoubleOrNull()
            if (lhsNum != null && rhsNum != null) {
                val res = when (op) {
                    "+=" -> lhsNum + rhsNum
                    "-=" -> lhsNum - rhsNum
                    "*=" -> lhsNum * rhsNum
                    "/=" -> if (rhsNum != 0.0) lhsNum / rhsNum else 0.0
                    else -> lhsNum
                }
                variables[varName] = if (res % 1.0 == 0.0) res.toInt().toString() else res.toString()
            } else if (op == "+=") {
                variables[varName] = (variables[varName] ?: "") + rhs
            }
            return false
        }

        // 5c. Check direct call to a defined JS function (e.g. init(); or updateUI();)
        val callMatch = jsDirectCallRegex.find(clean)
        if (callMatch != null) {
            val fnName = callMatch.groupValues[1]
            val fnDef = functions[fnName]
            if (fnDef != null && fnName != scopeName.removeSuffix("()")) {
                for ((fnLine, fnStmt) in fnDef.bodyLines) {
                    val halted = executeSingleStatement(
                        stmt = fnStmt,
                        lineNum = fnLine,
                        scopeName = "$fnName()",
                        variables = variables,
                        functions = functions,
                        textOverrides = textOverrides,
                        colorOverrides = colorOverrides,
                        bgOverrides = bgOverrides,
                        logs = logs
                    )
                    if (halted) return true
                }
            }
            return false
        }

        // 5d. Check variable declaration or assignment
        val varMatch = jsVarRegex.find(clean)
        if (varMatch != null && !clean.startsWith("if") && !clean.startsWith("for") && !clean.startsWith("while")) {
            val varName = varMatch.groupValues[1]
            val expr = varMatch.groupValues[2]
            variables[varName] = evaluateExpression(expr, variables)
            return false
        }

        // Valid browser/ES6 statements (addEventListener, querySelectorAll, loops, conditionals, canvas, timers, etc.)
        // are executed natively by the Chromium WebView engine — never emit false SyntaxErrors here!
        return false
    }

    private fun evaluateMultiArgs(argExpr: String, variables: Map<String, String>): String {
        val args = mutableListOf<String>()
        val sb = StringBuilder()
        var inSingle = false
        var inDouble = false
        var inTemplate = false
        var parenDepth = 0
        var bracketDepth = 0
        var braceDepth = 0

        for (i in argExpr.indices) {
            val ch = argExpr[i]
            if (i > 0 && argExpr[i - 1] == '\\') {
                sb.append(ch)
                continue
            }
            when {
                inSingle -> {
                    if (ch == '\'') inSingle = false
                    sb.append(ch)
                }
                inDouble -> {
                    if (ch == '"') inDouble = false
                    sb.append(ch)
                }
                inTemplate -> {
                    if (ch == '`') inTemplate = false
                    sb.append(ch)
                }
                ch == '\'' -> {
                    inSingle = true
                    sb.append(ch)
                }
                ch == '"' -> {
                    inDouble = true
                    sb.append(ch)
                }
                ch == '`' -> {
                    inTemplate = true
                    sb.append(ch)
                }
                ch == '(' -> {
                    parenDepth++
                    sb.append(ch)
                }
                ch == ')' -> {
                    parenDepth = (parenDepth - 1).coerceAtLeast(0)
                    sb.append(ch)
                }
                ch == '[' -> {
                    bracketDepth++
                    sb.append(ch)
                }
                ch == ']' -> {
                    bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                    sb.append(ch)
                }
                ch == '{' -> {
                    braceDepth++
                    sb.append(ch)
                }
                ch == '}' -> {
                    braceDepth = (braceDepth - 1).coerceAtLeast(0)
                    sb.append(ch)
                }
                ch == ',' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0 -> {
                    args.add(evaluateExpression(sb.toString(), variables))
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            args.add(evaluateExpression(sb.toString(), variables))
        }
        return args.joinToString(" ")
    }

    private fun evaluateExpression(expr: String, variables: Map<String, String>): String {
        val trimmedExpr = expr.trim()
        // Support ES6 template literals: `Hello ${name}!`
        if (trimmedExpr.startsWith("`") && trimmedExpr.endsWith("`") && trimmedExpr.length >= 2) {
            val inner = trimmedExpr.substring(1, trimmedExpr.length - 1)
            return templateVarRegex.replace(inner) { m ->
                val innerExpr = m.groupValues[1].trim()
                variables[innerExpr] ?: evaluateExpression(innerExpr, variables)
            }
        }

        val parts = trimmedExpr.split("+").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ""

        // Check if purely numeric addition
        val numericValues = parts.map { token ->
            val resolved = variables[token] ?: token
            resolved.toDoubleOrNull()
        }
        if (parts.size > 1 && numericValues.all { it != null }) {
            val sum = numericValues.sumOf { it ?: 0.0 }
            return if (sum % 1.0 == 0.0) sum.toInt().toString() else sum.toString()
        }

        return parts.joinToString("") { token ->
            when {
                (token.startsWith("\"") && token.endsWith("\"") && token.length >= 2) ||
                    (token.startsWith("'") && token.endsWith("'") && token.length >= 2) -> {
                    token.substring(1, token.length - 1)
                }
                token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                    val inner = token.substring(1, token.length - 1)
                    templateVarRegex.replace(inner) { m ->
                        val k = m.groupValues[1].trim()
                        variables[k] ?: k
                    }
                }
                variables.containsKey(token) -> variables[token] ?: ""
                else -> token
            }
        }
    }

    fun parseHexColor(raw: String): Color? {
        val trimmed = raw.trim().lowercase()
        when (trimmed) {
            "transparent", "none" -> return Color.Transparent
            "white", "#fff", "#ffffff" -> return Color.White
            "black", "#000", "#000000" -> return Color.Black
            "red", "crimson" -> return Color(0xFFEF4444)
            "green", "emerald", "lime" -> return Color(0xFF10B981)
            "blue" -> return Color(0xFF3B82F6)
            "cyan", "sky", "aqua" -> return Color(0xFF38BDF8)
            "yellow", "amber", "gold" -> return Color(0xFFF59E0B)
            "orange", "coral" -> return Color(0xFFF97316)
            "purple", "violet", "magenta" -> return Color(0xFF8B5CF6)
            "indigo" -> return Color(0xFF6366F1)
            "pink", "rose" -> return Color(0xFFF43F5E)
            "teal" -> return Color(0xFF14B8A6)
            "gray", "grey", "slate", "silver" -> return Color(0xFF64748B)
        }

        val hslMatch = hslColorRegex.find(raw)
        if (hslMatch != null) {
            val h = (hslMatch.groupValues[1].toFloatOrNull() ?: 0f) % 360f
            val s = ((hslMatch.groupValues[2].toFloatOrNull() ?: 100f) / 100f).coerceIn(0f, 1f)
            val l = ((hslMatch.groupValues[3].toFloatOrNull() ?: 50f) / 100f).coerceIn(0f, 1f)
            return hslToColor(if (h < 0) h + 360f else h, s, l)
        }

        val rgbMatch = rgbColorRegex.find(raw)
        if (rgbMatch != null) {
            val r = rgbMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 255) ?: 0
            val g = rgbMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 255) ?: 0
            val b = rgbMatch.groupValues[3].toIntOrNull()?.coerceIn(0, 255) ?: 0
            return Color(r, g, b)
        }

        val eightDigitMatch = eightDigitHexRegex.find(raw)
        if (eightDigitMatch != null) {
            return try {
                val rgba = eightDigitMatch.groupValues[1].toLong(16)
                val r = ((rgba shr 24) and 0xFF).toInt()
                val g = ((rgba shr 16) and 0xFF).toInt()
                val b = ((rgba shr 8) and 0xFF).toInt()
                val a = (rgba and 0xFF).toInt()
                Color(r, g, b, a)
            } catch (_: Exception) {
                null
            }
        }

        val sixDigitMatch = sixDigitHexRegex.find(raw)
        if (sixDigitMatch != null) {
            return try {
                val rgb = sixDigitMatch.groupValues[1].toLong(16)
                Color(0xFF000000 or rgb)
            } catch (_: Exception) {
                null
            }
        }
        val threeDigitMatch = threeDigitHexRegex.find(raw) ?: return null
        val hex = threeDigitMatch.groupValues[1]
        val fullHex = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
        return try {
            val rgb = fullHex.toLong(16)
            Color(0xFF000000 or rgb)
        } catch (_: Exception) {
            null
        }
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255),
            green = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255),
            blue = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        )
    }

    private fun parseNumberPxOrNull(raw: String): Float? {
        val numMatch = numberPxRegex.find(raw) ?: return null
        return numMatch.groupValues[1].toFloatOrNull()
    }
}
