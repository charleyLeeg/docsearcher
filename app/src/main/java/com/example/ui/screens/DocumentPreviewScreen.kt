package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IndexedDocument
import com.example.ui.components.buildHighlightedString
import com.example.ui.components.formatDate
import com.example.ui.components.formatFileSize
import com.example.ui.components.getFormatColor
import com.example.ui.components.openFileExternally
import com.example.ui.components.shareDocumentText
import com.example.ui.theme.HighlightBgDark
import com.example.ui.theme.HighlightBgLight
import com.example.ui.theme.HighlightTextDark
import com.example.ui.theme.HighlightTextLight
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewScreen(
    document: IndexedDocument,
    searchQuery: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showInfoDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val highlightBg = if (isDark) HighlightBgDark else HighlightBgLight
    val highlightText = if (isDark) HighlightTextDark else HighlightTextLight

    // Find all match occurrences in the content
    val matchIndices = remember(document.content, searchQuery) {
        val indices = mutableListOf<Int>()
        val cleanQuery = searchQuery.trim()
        if (cleanQuery.isNotBlank() && document.content.isNotBlank()) {
            val tokens = cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (tokens.isNotEmpty()) {
                val pattern = tokens.joinToString("|") { Pattern.quote(it) }
                try {
                    val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(document.content)
                    while (matcher.find()) {
                        indices.add(matcher.start())
                    }
                } catch (_: Exception) {}
            }
        }
        indices
    }

    var currentMatchIndex by remember { mutableIntStateOf(0) }
    val totalMatches = matchIndices.size
    val scrollState = rememberScrollState()

    val annotatedContent = remember(document.content, searchQuery, isDark) {
        buildHighlightedString(
            text = document.content,
            query = searchQuery,
            highlightBg = highlightBg,
            highlightText = highlightText
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${document.fileExtension.uppercase()} • ${formatFileSize(document.fileSizeBytes)} • ${document.wordCount} 字",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Info dialog
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Document Info"
                        )
                    }
                    // Copy text
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(document.content))
                        Toast.makeText(context, "已复制文档全文到剪贴板", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text"
                        )
                    }
                    // Share
                    IconButton(onClick = { shareDocumentText(context, document) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                    // Open in external viewer
                    IconButton(onClick = { openFileExternally(context, document) }) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in external app"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Match navigator bar (if matches exist)
            if (totalMatches > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = highlightBg
                            ) {
                                Text(
                                    text = "匹配项: ${currentMatchIndex + 1} / $totalMatches",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = highlightText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "\"$searchQuery\"",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = {
                                    if (currentMatchIndex > 0) {
                                        currentMatchIndex--
                                    } else {
                                        currentMatchIndex = totalMatches - 1
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous match",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    if (currentMatchIndex < totalMatches - 1) {
                                        currentMatchIndex++
                                    } else {
                                        currentMatchIndex = 0
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next match",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Document Content Pane
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(
                    text = annotatedContent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        fontFamily = if (document.fileExtension in listOf("json", "csv", "xml", "log")) FontFamily.Monospace else FontFamily.Default
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(text = "文档详细信息", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("文件名: ${document.fileName}", fontWeight = FontWeight.Medium)
                    Text("文件格式: ${document.fileExtension.uppercase()}")
                    Text("所在目录: ${document.folderName}")
                    Text("文件大小: ${formatFileSize(document.fileSizeBytes)}")
                    Text("总字数: ${document.wordCount} 字")
                    Text("修改时间: ${formatDate(document.lastModified)}")
                    Text("索引时间: ${formatDate(document.indexedAt)}")
                    Text("URI: ${document.uri}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}
