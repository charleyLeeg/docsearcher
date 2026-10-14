package com.example.parser

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.InflaterInputStream

object PdfParser {

    /**
     * Extracts text from PDF bytes by parsing content streams and decompressing
     * FlateDecode streams, then extracting text strings between BT and ET operators.
     */
    fun extractText(inputStream: InputStream): String {
        val pdfBytes = inputStream.readBytes()
        val textBuilder = StringBuilder()

        try {
            // Find streams in the PDF
            val streams = extractStreams(pdfBytes)
            for (streamBytes in streams) {
                val streamText = extractTextFromStream(streamBytes)
                if (streamText.isNotBlank()) {
                    textBuilder.append(streamText).append("\n")
                }
            }

            // If stream extraction produced very little, fallback to literal string extraction
            if (textBuilder.length < 50) {
                val fallbackText = fallbackExtractAsciiStrings(pdfBytes)
                if (fallbackText.isNotBlank()) {
                    textBuilder.append(fallbackText)
                }
            }
        } catch (_: Exception) {
            val fallbackText = fallbackExtractAsciiStrings(pdfBytes)
            textBuilder.append(fallbackText)
        }

        return textBuilder.toString().trim()
    }

    private fun extractStreams(pdfBytes: ByteArray): List<ByteArray> {
        val streams = mutableListOf<ByteArray>()
        val content = String(pdfBytes, Charsets.ISO_8859_1)

        val streamKeyword = "stream"
        val endStreamKeyword = "endstream"

        var searchIndex = 0
        while (searchIndex < content.length) {
            val streamPos = content.indexOf(streamKeyword, searchIndex)
            if (streamPos == -1) break

            val endStreamPos = content.indexOf(endStreamKeyword, streamPos)
            if (endStreamPos == -1) break

            // Check dictionary preceding "stream" to see if it's FlateDecode
            val dictStart = content.lastIndexOf("<<", streamPos)
            val isFlate = if (dictStart != -1 && dictStart > streamPos - 400) {
                val dict = content.substring(dictStart, streamPos)
                dict.contains("FlateDecode")
            } else {
                false
            }

            // Advance past "stream\r\n" or "stream\n"
            var startData = streamPos + streamKeyword.length
            if (startData < pdfBytes.size && pdfBytes[startData] == '\r'.code.toByte()) {
                startData++
            }
            if (startData < pdfBytes.size && pdfBytes[startData] == '\n'.code.toByte()) {
                startData++
            }

            var endData = endStreamPos
            if (endData > startData && pdfBytes[endData - 1] == '\n'.code.toByte()) {
                endData--
            }
            if (endData > startData && pdfBytes[endData - 1] == '\r'.code.toByte()) {
                endData--
            }

            if (startData in 0..endData && endData <= pdfBytes.size) {
                val rawStream = pdfBytes.copyOfRange(startData, endData)
                if (isFlate) {
                    try {
                        val inflated = inflate(rawStream)
                        streams.add(inflated)
                    } catch (_: Exception) {
                        streams.add(rawStream)
                    }
                } else {
                    streams.add(rawStream)
                }
            }

            searchIndex = endStreamPos + endStreamKeyword.length
        }

        return streams
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inStream = InflaterInputStream(ByteArrayInputStream(data))
        val outStream = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var count: Int
        while (inStream.read(buffer).also { count = it } != -1) {
            outStream.write(buffer, 0, count)
        }
        return outStream.toByteArray()
    }

    private fun extractTextFromStream(streamBytes: ByteArray): String {
        val streamContent = String(streamBytes, Charsets.ISO_8859_1)
        val sb = StringBuilder()

        // Match BT ... ET blocks or direct text operators
        val btEtRegex = Regex("BT[\\s\\S]*?ET")
        val blocks = btEtRegex.findAll(streamContent).map { it.value }.toList()

        val targets = if (blocks.isNotEmpty()) blocks else listOf(streamContent)

        for (block in targets) {
            // Find text strings in (...) Tj or [...] TJ or (...) ' or (...) "
            var i = 0
            while (i < block.length) {
                val ch = block[i]
                if (ch == '(') {
                    // Parse parenthesized PDF string with escape characters
                    val (extractedString, nextIdx) = parsePdfLiteralString(block, i)
                    i = nextIdx

                    // Look ahead for operator like Tj, TJ, ' or "
                    val lookAhead = block.substring(i, minOf(block.length, i + 10)).trim()
                    if (lookAhead.startsWith("Tj") || lookAhead.startsWith("'") || lookAhead.startsWith("\"") || lookAhead.startsWith("TJ")) {
                        sb.append(extractedString).append(" ")
                    } else {
                        sb.append(extractedString).append(" ")
                    }
                } else if (ch == '<' && i + 1 < block.length && block[i + 1] != '<') {
                    // Hex string <...>
                    val endIdx = block.indexOf('>', i)
                    if (endIdx != -1) {
                        val hex = block.substring(i + 1, endIdx).replace("\\s".toRegex(), "")
                        val decoded = decodeHexString(hex)
                        if (decoded.isNotBlank()) {
                            sb.append(decoded).append(" ")
                        }
                        i = endIdx + 1
                    } else {
                        i++
                    }
                } else if (ch == '\n' || ch == '\r') {
                    sb.append(" ")
                    i++
                } else {
                    i++
                }
            }
            sb.append("\n")
        }

        return cleanExtractedText(sb.toString())
    }

    private fun parsePdfLiteralString(content: String, startIndex: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var depth = 1
        var i = startIndex + 1
        var escaped = false

        while (i < content.length && depth > 0) {
            val c = content[i]
            if (escaped) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    '(' -> sb.append('(')
                    ')' -> sb.append(')')
                    '\\' -> sb.append('\\')
                    in '0'..'7' -> {
                        // Octal sequence \ddd
                        val octal = StringBuilder().append(c)
                        if (i + 1 < content.length && content[i + 1] in '0'..'7') {
                            octal.append(content[++i])
                            if (i + 1 < content.length && content[i + 1] in '0'..'7') {
                                octal.append(content[++i])
                            }
                        }
                        try {
                            sb.append(octal.toString().toInt(8).toChar())
                        } catch (_: Exception) {
                            sb.append(c)
                        }
                    }
                    else -> sb.append(c)
                }
                escaped = false
            } else {
                if (c == '\\') {
                    escaped = true
                } else if (c == '(') {
                    depth++
                    sb.append(c)
                } else if (c == ')') {
                    depth--
                    if (depth > 0) sb.append(c)
                } else {
                    sb.append(c)
                }
            }
            i++
        }
        return Pair(sb.toString(), i)
    }

    private fun decodeHexString(hex: String): String {
        return try {
            val bytes = ByteArray(hex.length / 2)
            for (i in 0 until hex.length - 1 step 2) {
                bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
            }
            // Check for UTF-16BE BOM: 0xFE 0xFF
            if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            } else {
                String(bytes, Charsets.ISO_8859_1)
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun fallbackExtractAsciiStrings(bytes: ByteArray): String {
        val sb = StringBuilder()
        val minLen = 4
        var current = StringBuilder()

        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c.isLetterOrDigit() || c in " .,;:!?'\"-_\n\r\t/@#()[]{}") {
                current.append(c)
            } else {
                if (current.length >= minLen) {
                    sb.append(current.toString().trim()).append(" ")
                }
                current = StringBuilder()
            }
        }
        if (current.length >= minLen) {
            sb.append(current.toString().trim())
        }
        return cleanExtractedText(sb.toString())
    }

    private fun cleanExtractedText(text: String): String {
        return text
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), " ")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
