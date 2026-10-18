package com.arnix.codepad.studio.engine

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    BUNDLED_HTML("html", "text/html", "Single Bundled .html"),
    ZIP_ARCHIVE("zip", "application/zip", "Project .zip Archive")
}

data class ExportResult(
    val format: ExportFormat,
    val fileName: String,
    val sizeBytes: Int,
    val savedPathOrUri: String,
    val bundledPreviewContent: String
)

object ProjectExportEngine {

    /**
     * Merges index.html, style.css, and script.js into a single self-contained .html bundle.
     */
    fun buildSingleBundledHtml(
        projectName: String,
        htmlCode: String,
        cssCode: String,
        jsCode: String
    ): String {
        val (cleanHtml, cleanCss, cleanJs) = WebSandboxEngine.resolveEffectiveCode(htmlCode, cssCode, jsCode)
        val normalizedCss = WebSandboxEngine.normalizeUserCss(cleanCss)
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>$projectName — Bundled by CodePad Studio</title>
              <style>
            ${normalizedCss.prependIndent("    ")}
              </style>
            </head>
            <body>
            ${cleanHtml.prependIndent("  ")}
              <script>
            ${cleanJs.prependIndent("    ")}
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Packages index.html, style.css, script.js, and README.md into a valid .zip archive in memory.
     */
    fun buildZipArchiveBytes(
        projectName: String,
        htmlCode: String,
        cssCode: String,
        jsCode: String
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun addEntry(name: String, content: String) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addEntry("index.html", htmlCode)
            addEntry("style.css", cssCode)
            addEntry("script.js", jsCode)
            addEntry(
                "bundle.html",
                buildSingleBundledHtml(projectName, htmlCode, cssCode, jsCode)
            )
            addEntry(
                "README.md",
                """
                # $projectName
                Exported from **CodePad Studio & Auto Write Studio** (Pure Native Android IDE).

                ## Included Files
                - `index.html` — Main DOM structure
                - `style.css` — Cyberpunk stylesheet
                - `script.js` — Interactive controller script
                - `bundle.html` — Standalone single-file build
                """.trimIndent()
            )
        }
        return baos.toByteArray()
    }

    fun exportToLocalAppDirectory(
        context: Context,
        projectName: String,
        htmlCode: String,
        cssCode: String,
        jsCode: String,
        format: ExportFormat
    ): ExportResult {
        val safeName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "codepad_project" }
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val bundledHtml = buildSingleBundledHtml(projectName, htmlCode, cssCode, jsCode)

        return when (format) {
            ExportFormat.BUNDLED_HTML -> {
                val fileName = "${safeName}_bundle.html"
                val outFile = File(exportDir, fileName)
                val bytes = bundledHtml.toByteArray(Charsets.UTF_8)
                outFile.writeBytes(bytes)
                ExportResult(
                    format = format,
                    fileName = fileName,
                    sizeBytes = bytes.size,
                    savedPathOrUri = outFile.absolutePath,
                    bundledPreviewContent = bundledHtml
                )
            }

            ExportFormat.ZIP_ARCHIVE -> {
                val fileName = "${safeName}_archive.zip"
                val outFile = File(exportDir, fileName)
                val bytes = buildZipArchiveBytes(projectName, htmlCode, cssCode, jsCode)
                outFile.writeBytes(bytes)
                ExportResult(
                    format = format,
                    fileName = fileName,
                    sizeBytes = bytes.size,
                    savedPathOrUri = outFile.absolutePath,
                    bundledPreviewContent = bundledHtml
                )
            }
        }
    }

    fun writeToDocumentUri(
        context: Context,
        uri: Uri,
        projectName: String,
        htmlCode: String,
        cssCode: String,
        jsCode: String,
        format: ExportFormat
    ): ExportResult {
        val safeName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "codepad_project" }
        val bundledHtml = buildSingleBundledHtml(projectName, htmlCode, cssCode, jsCode)
        val bytes = when (format) {
            ExportFormat.BUNDLED_HTML -> bundledHtml.toByteArray(Charsets.UTF_8)
            ExportFormat.ZIP_ARCHIVE -> buildZipArchiveBytes(projectName, htmlCode, cssCode, jsCode)
        }

        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
            output.flush()
        }

        return ExportResult(
            format = format,
            fileName = "$safeName.${format.extension}",
            sizeBytes = bytes.size,
            savedPathOrUri = uri.toString(),
            bundledPreviewContent = bundledHtml
        )
    }
}
