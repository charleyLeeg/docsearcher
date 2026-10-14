package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IndexedDocument
import com.example.search.SearchResultItem
import com.example.ui.theme.FormatCsv
import com.example.ui.theme.FormatDocx
import com.example.ui.theme.FormatJson
import com.example.ui.theme.FormatMarkdown
import com.example.ui.theme.FormatOther
import com.example.ui.theme.FormatPdf
import com.example.ui.theme.FormatTxt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentCard(
    result: SearchResultItem,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val doc = result.document
    val context = LocalContext.current
    val formatColor = getFormatColor(doc.fileExtension)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("document_card_${doc.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Format Badge, Title, Matches Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = formatColor,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        text = doc.fileExtension.uppercase(Locale.ROOT),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // File Name
                Column(modifier = Modifier.weight(1f)) {
                    HighlightedText(
                        text = doc.fileName,
                        query = query,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Path / Folder Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = doc.folderName,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Matches count badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${result.totalMatches} 处匹配",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Snippets List
            if (result.snippets.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    result.snippets.take(2).forEach { snippet ->
                        SnippetItemView(
                            snippet = snippet,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Metadata footer & Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // File info: Size, Words, Date
                Text(
                    text = "${formatFileSize(doc.fileSizeBytes)} • ${doc.wordCount} 字 • ${formatDate(doc.lastModified)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Open in external app action
                    IconButton(
                        onClick = { openFileExternally(context, doc) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in external app",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Share action
                    IconButton(
                        onClick = { shareDocumentText(context, doc) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // View preview
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.padding(start = 4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "查看", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun getFormatColor(extension: String): Color {
    return when (extension.lowercase(Locale.ROOT)) {
        "pdf" -> FormatPdf
        "docx", "doc" -> FormatDocx
        "md", "markdown" -> FormatMarkdown
        "txt", "log" -> FormatTxt
        "json" -> FormatJson
        "csv", "tsv" -> FormatCsv
        else -> FormatOther
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes.toFloat() / (1024 * 1024))
        bytes >= 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes.toFloat() / 1024)
        else -> "$bytes B"
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun openFileExternally(context: Context, doc: IndexedDocument) {
    try {
        val uri = Uri.parse(doc.uri)
        val mimeType = when (doc.fileExtension.lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "md", "markdown" -> "text/markdown"
            "txt", "log" -> "text/plain"
            "json" -> "application/json"
            "csv" -> "text/csv"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "打开文档: ${doc.fileName}"))
    } catch (_: Exception) {
        // Fallback: share text
        shareDocumentText(context, doc)
    }
}

fun shareDocumentText(context: Context, doc: IndexedDocument) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, doc.fileName)
        putExtra(Intent.EXTRA_TEXT, "${doc.fileName}\n\n${doc.content.take(10000)}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享文档内容"))
}
