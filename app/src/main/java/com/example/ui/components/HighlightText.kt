package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.search.SearchMatchSnippet
import com.example.ui.theme.HighlightBgDark
import com.example.ui.theme.HighlightBgLight
import com.example.ui.theme.HighlightTextDark
import com.example.ui.theme.HighlightTextLight
import java.util.regex.Pattern

@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    caseSensitive: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val isDark = isSystemInDarkTheme()
    val highlightBg = if (isDark) HighlightBgDark else HighlightBgLight
    val highlightTextColor = if (isDark) HighlightTextDark else HighlightTextLight

    val annotated = remember(text, query, caseSensitive, isDark) {
        buildHighlightedString(
            text = text,
            query = query,
            highlightBg = highlightBg,
            highlightText = highlightTextColor,
            caseSensitive = caseSensitive
        )
    }

    Text(
        text = annotated,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun SnippetItemView(
    snippet: SearchMatchSnippet,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val highlightBg = if (isDark) HighlightBgDark else HighlightBgLight
    val highlightTextColor = if (isDark) HighlightTextDark else HighlightTextLight

    val annotated = remember(snippet, isDark) {
        buildAnnotatedString {
            append(snippet.prefix)
            if (snippet.prefix.isNotEmpty() && !snippet.prefix.endsWith(" ")) {
                append(" ")
            }
            val startIdx = length
            append(snippet.matchedText)
            val endIdx = length

            addStyle(
                style = SpanStyle(
                    background = highlightBg,
                    color = highlightTextColor,
                    fontWeight = FontWeight.Bold
                ),
                start = startIdx,
                end = endIdx
            )

            if (snippet.suffix.isNotEmpty() && !snippet.suffix.startsWith(" ")) {
                append(" ")
            }
            append(snippet.suffix)
        }
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun buildHighlightedString(
    text: String,
    query: String,
    highlightBg: Color,
    highlightText: Color,
    caseSensitive: Boolean = false
): AnnotatedString {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank() || text.isBlank()) {
        return AnnotatedString(text)
    }

    val tokens = cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return AnnotatedString(text)

    val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
    val pattern = tokens.joinToString("|") { Pattern.quote(it) }
    val compiled = try {
        Pattern.compile(pattern, flags)
    } catch (_: Exception) {
        return AnnotatedString(text)
    }

    val matcher = compiled.matcher(text)
    return buildAnnotatedString {
        append(text)
        while (matcher.find()) {
            addStyle(
                style = SpanStyle(
                    background = highlightBg,
                    color = highlightText,
                    fontWeight = FontWeight.Bold
                ),
                start = matcher.start(),
                end = matcher.end()
            )
        }
    }
}
