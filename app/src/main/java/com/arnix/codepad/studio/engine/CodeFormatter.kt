package com.arnix.codepad.studio.engine

import com.arnix.codepad.studio.data.CodeTab
import kotlin.math.max

object CodeFormatter {

    private val voidHtmlTags = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
        "path", "circle", "rect", "line", "polyline", "polygon", "ellipse", "stop", "use"
    )

    fun format(code: String, tab: CodeTab): String {
        if (code.isBlank()) return code
        val sanitized = WebSandboxEngine.sanitizeCode(code)
        return when (tab) {
            CodeTab.INDEX_HTML -> formatHtml(sanitized)
            CodeTab.STYLE_CSS -> formatBraceLanguage(sanitized, isCss = true)
            CodeTab.SCRIPT_JS -> formatBraceLanguage(sanitized, isCss = false)
        }
    }

    private fun formatHtml(raw: String): String {
        val rawLines = raw
            .replace(Regex(">\\s+<"), ">\n<")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var indent = 0
        val out = StringBuilder()

        for (line in rawLines) {
            val isClosingTag = line.startsWith("</")
            val isCommentOrDecl = line.startsWith("<!--") || line.startsWith("<!") || line.startsWith("<?")
            if (isClosingTag) {
                indent = max(0, indent - 1)
            }

            out.append("  ".repeat(indent)).append(line).append("\n")

            if (!isClosingTag && !isCommentOrDecl && line.startsWith("<") && !line.endsWith("/>")) {
                val tagName = line.removePrefix("<")
                    .takeWhile { it.isLetterOrDigit() || it == '-' }
                    .lowercase()
                val closesOnSameLine = line.contains("</$tagName>", ignoreCase = true)
                if (tagName.isNotEmpty() && tagName !in voidHtmlTags && !closesOnSameLine) {
                    indent += 1
                }
            }
        }
        return out.toString().trimEnd()
    }

    private fun formatBraceLanguage(raw: String, isCss: Boolean): String {
        val normalized = if (isCss) {
            normalizeCssLineBreaksSafe(raw)
        } else {
            raw
        }

        val lines = normalized.lines()
        var indent = 0
        val out = StringBuilder()
        var previousBlank = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                if (!previousBlank && out.isNotEmpty()) {
                    out.append("\n")
                    previousBlank = true
                }
                continue
            }
            previousBlank = false

            val leadingClosers = line.takeWhile { it == '}' || it == ']' || it == ')' }.count { it == '}' || it == ']' }
            val lineIndent = max(0, indent - leadingClosers)

            out.append("  ".repeat(lineIndent)).append(line).append("\n")

            val (openCount, closeCount) = countStructuralBracesOutsideStrings(line)
            indent = max(0, indent + (openCount - closeCount))
        }
        return out.toString().trimEnd()
    }

    private fun normalizeCssLineBreaksSafe(raw: String): String {
        val sb = StringBuilder(raw.length + 64)
        var inSingle = false
        var inDouble = false
        var inComment = false
        var parenDepth = 0
        var i = 0
        val len = raw.length

        while (i < len) {
            val ch = raw[i]
            val next = if (i + 1 < len) raw[i + 1] else '\u0000'

            if (inComment) {
                sb.append(ch)
                if (ch == '*' && next == '/') {
                    sb.append('/')
                    i += 2
                    inComment = false
                    while (i < len && (raw[i] == ' ' || raw[i] == '\t')) i++
                    if (i < len && raw[i] != '\n') sb.append('\n')
                    continue
                }
                i++
                continue
            }
            if (inSingle) {
                sb.append(ch)
                if (ch == '\\' && i + 1 < len) {
                    sb.append(raw[i + 1])
                    i += 2
                    continue
                }
                if (ch == '\'') inSingle = false
                i++
                continue
            }
            if (inDouble) {
                sb.append(ch)
                if (ch == '\\' && i + 1 < len) {
                    sb.append(raw[i + 1])
                    i += 2
                    continue
                }
                if (ch == '"') inDouble = false
                i++
                continue
            }

            when {
                ch == '\'' -> {
                    inSingle = true
                    sb.append(ch)
                    i++
                }
                ch == '"' -> {
                    inDouble = true
                    sb.append(ch)
                    i++
                }
                ch == '/' && next == '*' -> {
                    inComment = true
                    sb.append("/*")
                    i += 2
                }
                ch == '(' -> {
                    parenDepth++
                    sb.append(ch)
                    i++
                }
                ch == ')' -> {
                    parenDepth = max(0, parenDepth - 1)
                    sb.append(ch)
                    i++
                }
                ch == '{' && parenDepth == 0 -> {
                    while (sb.isNotEmpty() && (sb.last() == ' ' || sb.last() == '\t')) {
                        sb.deleteCharAt(sb.lastIndex)
                    }
                    if (sb.isNotEmpty() && sb.last() != '\n') sb.append(' ')
                    sb.append("{\n")
                    i++
                    while (i < len && (raw[i] == ' ' || raw[i] == '\t' || raw[i] == '\n')) i++
                }
                ch == ';' && parenDepth == 0 -> {
                    sb.append(";\n")
                    i++
                    while (i < len && (raw[i] == ' ' || raw[i] == '\t')) i++
                    if (i < len && raw[i] == '\n') i++
                }
                ch == '}' && parenDepth == 0 -> {
                    if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
                    sb.append("}\n\n")
                    i++
                    while (i < len && (raw[i] == ' ' || raw[i] == '\t' || raw[i] == '\n')) i++
                }
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun countStructuralBracesOutsideStrings(line: String): Pair<Int, Int> {
        var open = 0
        var close = 0
        var inSingle = false
        var inDouble = false
        var inTemplate = false
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
                inTemplate -> if (ch == '`') inTemplate = false
                ch == '\'' -> inSingle = true
                ch == '"' -> inDouble = true
                ch == '`' -> inTemplate = true
                ch == '/' && i + 1 < line.length && line[i + 1] == '/' -> break
                ch == '{' || ch == '[' -> open++
                ch == '}' || ch == ']' -> close++
            }
            i++
        }
        return open to close
    }
}
