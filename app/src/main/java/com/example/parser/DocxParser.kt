package com.example.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object DocxParser {

    /**
     * Extracts all plain text from a .docx file stream.
     * Traverses the ZIP archive looking for word/document.xml, word/header*.xml,
     * and word/footer*.xml, then extracts paragraphs and text tokens.
     */
    fun extractText(inputStream: InputStream): String {
        val result = StringBuilder()
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry

        while (entry != null) {
            val name = entry.name
            if (name.startsWith("word/") && name.endsWith(".xml")) {
                if (name == "word/document.xml" || name.contains("header") || name.contains("footer")) {
                    val xmlText = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    val parsed = parseWordXml(xmlText)
                    if (parsed.isNotBlank()) {
                        result.append(parsed).append("\n\n")
                    }
                }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        return result.toString().trim()
    }

    private fun parseWordXml(xml: String): String {
        val sb = StringBuilder()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())

            var eventType = parser.eventType
            var inParagraph = false
            var inText = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "p" -> {
                                inParagraph = true
                            }
                            "t" -> {
                                inText = true
                            }
                            "tab" -> {
                                sb.append("\t")
                            }
                            "br", "cr" -> {
                                sb.append("\n")
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            sb.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "t" -> {
                                inText = false
                            }
                            "p" -> {
                                inParagraph = false
                                sb.append("\n")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Fallback: regex extraction of <w:t> content
            val textRegex = Regex("<w:t[^>]*>(.*?)</w:t>")
            textRegex.findAll(xml).forEach { match ->
                sb.append(decodeXmlEntities(match.groupValues[1]))
            }
        }
        return sb.toString()
    }

    private fun decodeXmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
