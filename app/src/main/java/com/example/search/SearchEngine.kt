package com.example.search

import com.example.data.model.IndexedDocument
import java.util.Locale
import java.util.regex.Pattern

enum class SearchMode(val displayName: String) {
    ALL_WORDS("All Words (AND)"),
    ANY_WORD("Any Word (OR)"),
    EXACT_PHRASE("Exact Phrase"),
    PREFIX("Prefix Wildcard"),
    REGEX("Regular Expression")
}

enum class SortOption(val displayName: String) {
    RELEVANCE("Best Match"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    SIZE_DESC("Largest File"),
    SIZE_ASC("Smallest File"),
    NAME_ASC("Name (A to Z)")
}

data class SearchMatchSnippet(
    val prefix: String,
    val matchedText: String,
    val suffix: String,
    val startIndex: Int,
    val endIndex: Int
)

data class SearchResultItem(
    val document: IndexedDocument,
    val totalMatches: Int,
    val score: Float,
    val snippets: List<SearchMatchSnippet>,
    val nameMatches: Boolean = false
)

object SearchEngine {

    fun executeSearch(
        documents: List<IndexedDocument>,
        query: String,
        searchMode: SearchMode = SearchMode.ALL_WORDS,
        selectedFormats: Set<String> = emptySet(),
        minSizeBytes: Long? = null,
        maxSizeBytes: Long? = null,
        sortOption: SortOption = SortOption.RELEVANCE,
        caseSensitive: Boolean = false
    ): List<SearchResultItem> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        // Filter by file format, min/max size
        val candidateDocs = documents.filter { doc ->
            val formatMatches = if (selectedFormats.isEmpty() || selectedFormats.contains("all")) {
                true
            } else {
                selectedFormats.contains(doc.fileExtension.lowercase(Locale.ROOT))
            }

            val minSizeMatches = minSizeBytes == null || doc.fileSizeBytes >= minSizeBytes
            val maxSizeMatches = maxSizeBytes == null || doc.fileSizeBytes <= maxSizeBytes

            formatMatches && minSizeMatches && maxSizeMatches
        }

        val results = mutableListOf<SearchResultItem>()

        for (doc in candidateDocs) {
            val content = doc.content
            val fileName = doc.fileName

            val (matchCount, snippets, nameMatches) = evaluateDocument(
                content = content,
                fileName = fileName,
                query = cleanQuery,
                mode = searchMode,
                caseSensitive = caseSensitive
            )

            if (matchCount > 0 || nameMatches) {
                val score = calculateScore(doc, matchCount, nameMatches)
                results.add(
                    SearchResultItem(
                        document = doc,
                        totalMatches = matchCount,
                        score = score,
                        snippets = snippets,
                        nameMatches = nameMatches
                    )
                )
            }
        }

        // Sort results
        return when (sortOption) {
            SortOption.RELEVANCE -> results.sortedWith(
                compareByDescending<SearchResultItem> { it.nameMatches }
                    .thenByDescending { it.score }
                    .thenByDescending { it.totalMatches }
            )
            SortOption.DATE_DESC -> results.sortedByDescending { it.document.lastModified }
            SortOption.DATE_ASC -> results.sortedBy { it.document.lastModified }
            SortOption.SIZE_DESC -> results.sortedByDescending { it.document.fileSizeBytes }
            SortOption.SIZE_ASC -> results.sortedBy { it.document.fileSizeBytes }
            SortOption.NAME_ASC -> results.sortedBy { it.document.fileName.lowercase(Locale.ROOT) }
        }
    }

    private fun evaluateDocument(
        content: String,
        fileName: String,
        query: String,
        mode: SearchMode,
        caseSensitive: Boolean
    ): Triple<Int, List<SearchMatchSnippet>, Boolean> {
        val regex = try {
            buildRegex(query, mode, caseSensitive)
        } catch (_: Exception) {
            return Triple(0, emptyList(), false)
        }

        val nameMatches = regex.containsMatchIn(fileName)

        val matches = regex.findAll(content).toList()
        val totalMatches = matches.size

        if (totalMatches == 0 && !nameMatches) {
            return Triple(0, emptyList(), false)
        }

        // For ALL_WORDS mode, verify every word token was found
        if (mode == SearchMode.ALL_WORDS) {
            val tokens = splitTokens(query)
            if (tokens.size > 1) {
                val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
                for (token in tokens) {
                    val p = Pattern.compile(Pattern.quote(token), flags)
                    if (!p.matcher(content).find() && !p.matcher(fileName).find()) {
                        return Triple(0, emptyList(), false)
                    }
                }
            }
        }

        // Extract snippets
        val snippets = extractSnippets(content, matches, maxSnippets = 4)

        return Triple(totalMatches, snippets, nameMatches)
    }

    private fun buildRegex(query: String, mode: SearchMode, caseSensitive: Boolean): Regex {
        val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)

        return when (mode) {
            SearchMode.EXACT_PHRASE -> {
                Regex(Pattern.quote(query), options)
            }
            SearchMode.ALL_WORDS, SearchMode.ANY_WORD -> {
                val tokens = splitTokens(query)
                if (tokens.isEmpty()) {
                    Regex(Pattern.quote(query), options)
                } else {
                    val pattern = tokens.joinToString("|") { Pattern.quote(it) }
                    Regex(pattern, options)
                }
            }
            SearchMode.PREFIX -> {
                val tokens = splitTokens(query)
                if (tokens.isEmpty()) {
                    Regex(Pattern.quote(query), options)
                } else {
                    val pattern = tokens.joinToString("|") { "\\b" + Pattern.quote(it) + "\\w*" }
                    Regex(pattern, options)
                }
            }
            SearchMode.REGEX -> {
                Regex(query, options)
            }
        }
    }

    private fun splitTokens(query: String): List<String> {
        return query.split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    private fun extractSnippets(
        content: String,
        matches: List<MatchResult>,
        maxSnippets: Int
    ): List<SearchMatchSnippet> {
        if (matches.isEmpty() || content.isEmpty()) return emptyList()

        val snippets = mutableListOf<SearchMatchSnippet>()
        val selectedMatches = if (matches.size <= maxSnippets) {
            matches
        } else {
            // Pick evenly distributed matches across document
            val step = matches.size.toFloat() / maxSnippets
            (0 until maxSnippets).map { matches[(it * step).toInt()] }
        }

        val contextChars = 70

        for (match in selectedMatches) {
            val start = match.range.first
            val end = match.range.last + 1
            val matchedText = match.value

            var prefixStart = maxOf(0, start - contextChars)
            var suffixEnd = minOf(content.length, end + contextChars)

            // Adjust prefix to sentence or word boundary
            if (prefixStart > 0) {
                val prevNewline = content.lastIndexOf('\n', start - 1)
                val prevPeriod = content.lastIndexOf('.', start - 1)
                val bestBoundary = maxOf(prevNewline, prevPeriod)
                if (bestBoundary in prefixStart..(start - 10)) {
                    prefixStart = bestBoundary + 1
                } else {
                    val prevSpace = content.indexOf(' ', prefixStart)
                    if (prevSpace != -1 && prevSpace < start) {
                        prefixStart = prevSpace + 1
                    }
                }
            }

            // Adjust suffix to sentence or word boundary
            if (suffixEnd < content.length) {
                val nextNewline = content.indexOf('\n', end)
                val nextPeriod = content.indexOf('.', end)
                val candidates = listOf(nextNewline, nextPeriod).filter { it in end..suffixEnd }
                if (candidates.isNotEmpty()) {
                    suffixEnd = candidates.minOrNull()!! + 1
                } else {
                    val nextSpace = content.lastIndexOf(' ', suffixEnd)
                    if (nextSpace > end) {
                        suffixEnd = nextSpace
                    }
                }
            }

            val rawPrefix = content.substring(prefixStart, start).trimStart()
            val rawSuffix = content.substring(end, suffixEnd).trimEnd()

            val prefixFormatted = if (prefixStart > 0) "… $rawPrefix" else rawPrefix
            val suffixFormatted = if (suffixEnd < content.length) "$rawSuffix …" else rawSuffix

            snippets.add(
                SearchMatchSnippet(
                    prefix = prefixFormatted.replace("\n", " "),
                    matchedText = matchedText.replace("\n", " "),
                    suffix = suffixFormatted.replace("\n", " "),
                    startIndex = start,
                    endIndex = end
                )
            )
        }

        return snippets
    }

    private fun calculateScore(doc: IndexedDocument, matchCount: Int, nameMatches: Boolean): Float {
        var score = matchCount.toFloat()
        if (doc.wordCount > 0) {
            // Term frequency ratio
            score += (matchCount.toFloat() / doc.wordCount) * 100f
        }
        if (nameMatches) {
            score += 25f
        }
        return score
    }
}
