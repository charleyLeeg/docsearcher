package com.example.parser

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleDocumentGenerator {

    fun generateSampleDocuments(context: Context): List<File> {
        val sampleDir = File(context.filesDir, "sample_documents")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
        }

        val files = mutableListOf<File>()

        // 1. Markdown file
        val mdFile = File(sampleDir, "Android_Architecture_Guide.md")
        mdFile.writeText(
            """
            # Android Architecture Guide & DocFetcher Indexing

            ## Overview
            DocFetcher for Android provides fast desktop-grade full-text search across documents stored locally.
            It indexes text files, PDFs, Word DOCX documents, and Markdown notes into an on-device Room database.

            ## Key Features
            - **Zero Cloud Leakage**: All document indexing and keyword extraction occurs locally on the device.
            - **Format Diversity**: Seamlessly parse `.txt`, `.md`, `.docx`, `.pdf`, `.json`, and `.csv`.
            - **Snippet Highlighting**: Highlights search queries directly within surrounding context sentences.
            - **Boolean Search**: Supports AND, OR, exact phrases, and wildcard prefixes.

            ## Best Practices for Search Engines
            When building mobile search engines, memory usage must be tightly constrained.
            Streaming parsers prevent Out-Of-Memory (OOM) crashes when indexing large books or reports.
            Coroutines ensure the UI remains smooth at 60 FPS while background indexing processes hundreds of files.

            ## Conclusion
            Modern Android applications benefit enormously from offline instant search capabilities.
            """.trimIndent()
        )
        files.add(mdFile)

        // 2. Plain Text file
        val txtFile = File(sampleDir, "Project_Alpha_Meeting_Notes.txt")
        txtFile.writeText(
            """
            PROJECT ALPHA - WEEKLY ENGINEERING SYNC
            Date: October 14, 2026
            Attendees: Sarah Jenkins, David Chen, Alex Rivera, Maria Rossi

            Agenda & Discussion:
            1. Search Indexing Performance:
               David reported that full-text search query latency is now under 15 milliseconds across 5,000 documents.
               The SQLite indexing strategy uses inverted word tables and prefix caching.

            2. Document Parser Enhancements:
               Alex updated the DOCX extractor to handle inline tables and footnotes.
               Maria added support for PDF FlateDecode streams to uncompress embedded text blocks.

            3. Security and Storage Access Framework:
               The app requests persistent URI permissions via SAF without demanding broad external storage permissions.
               This ensures full Google Play Store policy compliance and protects sensitive user documents.

            Action Items:
            - David to benchmark indexing speed on low-end test devices.
            - Sarah to refine Material 3 dark mode theme and snippet highlight contrast.
            - Alex to verify Word document paragraph breaks.
            """.trimIndent()
        )
        files.add(txtFile)

        // 3. Real Valid DOCX file (Zip with word/document.xml)
        val docxFile = File(sampleDir, "Quarterly_Business_Review.docx")
        createValidDocx(
            docxFile,
            title = "Quarterly Business Review - Enterprise Search Solutions",
            paragraphs = listOf(
                "Executive Summary: During the third quarter of 2026, enterprise adoption of local on-device document indexing solutions increased by 42%.",
                "Customer Feedback Highlights: Users emphasized the critical need for rapid search inside PDF manuals, DOCX legal contracts, and developer Markdown notes.",
                "Financial Results: Total operational expenditures decreased by 18% due to edge computing and local device query processing, removing recurring server costs.",
                "Risk Analysis: Large document sizes can cause latency during initial indexing. Asynchronous batching and background coroutine workers mitigate this risk.",
                "Strategic Next Steps: Deploy DocFetcher mobile client to field technicians requiring offline document access in remote environments."
            )
        )
        files.add(docxFile)

        // 4. Real Valid PDF file (with PDF text stream)
        val pdfFile = File(sampleDir, "Machine_Learning_Whitepaper.pdf")
        createValidPdf(
            pdfFile,
            title = "Machine Learning & Natural Language Information Retrieval",
            lines = listOf(
                "Machine Learning and Natural Language Information Retrieval",
                "Abstract: Information retrieval engines combine lexical matching and semantic vector representations.",
                "Full text search systems like DocFetcher tokenize input text and compute relevance scores.",
                "BM25 and TF-IDF remain foundational algorithms for local document scoring.",
                "High performance mobile search requires efficient inverted indices and snippet extraction.",
                "Published by AI Studio Research Group, 2026."
            )
        )
        files.add(pdfFile)

        // 5. CSV file
        val csvFile = File(sampleDir, "Department_Staff_Directory.csv")
        csvFile.writeText(
            """
            ID,Name,Department,Role,Email,Specialization
            101,Sarah Jenkins,Engineering,Lead Architect,sarah.jenkins@company.local,Android & Jetpack Compose
            102,David Chen,Engineering,Database Specialist,david.chen@company.local,Room Database & Full-Text Search
            103,Alex Rivera,Research,NLP Engineer,alex.rivera@company.local,Text Parsers & PDF Extraction
            104,Maria Rossi,Design,Product Designer,maria.rossi@company.local,Material Design 3 & Accessibility
            105,Lucas Vance,Operations,Security Officer,lucas.vance@company.local,Storage Access Framework & Zero-Trust
            """.trimIndent()
        )
        files.add(csvFile)

        // 6. JSON file
        val jsonFile = File(sampleDir, "Search_Engine_Settings.json")
        jsonFile.writeText(
            """
            {
              "engine": "DocFetcher Android",
              "version": "1.0.0",
              "indexing": {
                "maxFileSizeMb": 50,
                "batchSize": 20,
                "supportedExtensions": ["txt", "md", "docx", "pdf", "csv", "json", "html"],
                "enableSnippetContext": true,
                "snippetContextChars": 80
              },
              "performance": {
                "backgroundWorkerThreads": 4,
                "cacheSizeKb": 8192,
                "instantSearchDebounceMs": 200
              }
            }
            """.trimIndent()
        )
        files.add(jsonFile)

        return files
    }

    private fun createValidDocx(file: File, title: String, paragraphs: List<String>) {
        val zipOut = ZipOutputStream(FileOutputStream(file))

        // [Content_Types].xml
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        zipOut.write(contentTypesXml.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // _rels/.rels
        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        zipOut.write(relsXml.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // word/document.xml
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>
        <w:p><w:r><w:rPr><w:b/></w:rPr><w:t>""").append(escapeXml(title)).append("""</w:t></w:r></w:p>
""")
        for (p in paragraphs) {
            sb.append("        <w:p><w:r><w:t>").append(escapeXml(p)).append("</w:t></w:r></w:p>\n")
        }
        sb.append("""    </w:body>
</w:document>""")

        zipOut.putNextEntry(ZipEntry("word/document.xml"))
        zipOut.write(sb.toString().toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        zipOut.close()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun createValidPdf(file: File, title: String, lines: List<String>) {
        // Construct a standard minimal valid PDF file with text operators
        val streamContent = java.lang.StringBuilder()
        streamContent.append("BT\n")
        streamContent.append("/F1 16 Tf\n")
        streamContent.append("50 750 Td\n")
        streamContent.append("(")
        streamContent.append(escapePdf(title))
        streamContent.append(") Tj\n")

        var yOffset = -25
        streamContent.append("/F1 12 Tf\n")
        for (line in lines) {
            streamContent.append("0 ")
            streamContent.append(yOffset)
            streamContent.append(" Td\n")
            streamContent.append("(")
            streamContent.append(escapePdf(line))
            streamContent.append(") Tj\n")
            yOffset = -20
        }
        streamContent.append("ET\n")

        val streamBytes = streamContent.toString().toByteArray(Charsets.ISO_8859_1)

        val out = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()

        // PDF Header
        out.write("%PDF-1.4\n".toByteArray(Charsets.ISO_8859_1))

        // Object 1: Catalog
        offsets.add(out.size())
        out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".toByteArray(Charsets.ISO_8859_1))

        // Object 2: Pages
        offsets.add(out.size())
        out.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".toByteArray(Charsets.ISO_8859_1))

        // Object 3: Page
        offsets.add(out.size())
        out.write(
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
                .toByteArray(Charsets.ISO_8859_1)
        )

        // Object 4: Stream Contents
        offsets.add(out.size())
        out.write("4 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n".toByteArray(Charsets.ISO_8859_1))
        out.write(streamBytes)
        out.write("\nendstream\nendobj\n".toByteArray(Charsets.ISO_8859_1))

        // Object 5: Font
        offsets.add(out.size())
        out.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".toByteArray(Charsets.ISO_8859_1))

        // xref table
        val startXref = out.size()
        out.write("xref\n0 6\n0000000000 65535 f \n".toByteArray(Charsets.ISO_8859_1))
        for (offset in offsets) {
            val offsetStr = String.format("%010d 00000 n \n", offset)
            out.write(offsetStr.toByteArray(Charsets.ISO_8859_1))
        }

        // trailer
        out.write("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$startXref\n%%EOF\n".toByteArray(Charsets.ISO_8859_1))

        file.writeBytes(out.toByteArray())
    }

    private fun escapePdf(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }
}
