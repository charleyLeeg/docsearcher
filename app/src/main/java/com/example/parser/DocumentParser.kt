package com.example.parser

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.Locale

object DocumentParser {

    val SUPPORTED_EXTENSIONS = setOf(
        "txt",
        "md",
        "markdown",
        "docx",
        "pdf",
        "json",
        "csv",
        "tsv",
        "xml",
        "html",
        "htm",
        "log",
        "java",
        "kt",
        "py",
        "c",
        "cpp",
        "h",
        "js",
        "ts"
    )

    fun isSupported(extension: String): Boolean {
        return SUPPORTED_EXTENSIONS.contains(extension.lowercase(Locale.ROOT))
    }

    fun parse(context: Context, uri: Uri, fileName: String): ExtractedDocumentData {
        val extension = getExtension(fileName)
        var content = ""

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                content = parseStream(stream, extension)
            }
        } catch (e: Exception) {
            content = "Error reading document: ${e.localizedMessage ?: "Unknown error"}"
        }

        val wordCount = countWords(content)

        return ExtractedDocumentData(
            content = content,
            wordCount = wordCount,
            fileExtension = extension
        )
    }

    fun parseStream(stream: InputStream, extension: String): String {
        return when (extension.lowercase(Locale.ROOT)) {
            "docx" -> DocxParser.extractText(stream)
            "pdf" -> PdfParser.extractText(stream)
            "html", "htm" -> {
                val rawHtml = stream.bufferedReader(Charsets.UTF_8).readText()
                stripHtmlTags(rawHtml)
            }
            "xml" -> {
                val rawXml = stream.bufferedReader(Charsets.UTF_8).readText()
                stripXmlTags(rawXml)
            }
            else -> {
                // txt, md, markdown, json, csv, log, code files
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
        }
    }

    fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else {
            "txt"
        }
    }

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        // Count words considering both whitespace-separated words and CJK characters
        var count = 0
        var inWord = false
        for (char in text) {
            if (Character.isWhitespace(char)) {
                inWord = false
            } else if (isCjk(char)) {
                count++
                inWord = false
            } else {
                if (!inWord) {
                    count++
                    inWord = true
                }
            }
        }
        return count
    }

    private fun isCjk(c: Char): Boolean {
        val ub = Character.UnicodeBlock.of(c)
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub == Character.UnicodeBlock.GENERAL_PUNCTUATION ||
                ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
    }

    private fun stripHtmlTags(html: String): String {
        val noScripts = html.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
        val stripped = noScripts.replace(Regex("<[^>]+>"), " ")
        return stripped
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun stripXmlTags(xml: String): String {
        val stripped = xml.replace(Regex("<[^>]+>"), " ")
        return stripped
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}

data class ExtractedDocumentData(
    val content: String,
    val wordCount: Int,
    val fileExtension: String
)
