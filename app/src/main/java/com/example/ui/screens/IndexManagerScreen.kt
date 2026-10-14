package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IndexedDocument
import com.example.data.model.IndexedScope
import com.example.data.repository.IndexingProgress
import com.example.ui.DocFetcherViewModel
import com.example.ui.components.formatDate
import com.example.ui.components.formatFileSize
import com.example.ui.components.getFormatColor

@Composable
fun IndexManagerScreen(
    viewModel: DocFetcherViewModel,
    scopes: List<IndexedScope>,
    documents: List<IndexedDocument>,
    indexingProgress: IndexingProgress,
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var scopeToDelete by remember { mutableStateOf<IndexedScope?>(null) }
    var docToDelete by remember { mutableStateOf<IndexedDocument?>(null) }

    // SAF Launchers
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.indexFolder(uri)
        }
    }

    val filesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.indexFiles(uris)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Live Progress or Status Card
        if (indexingProgress !is IndexingProgress.Idle) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("indexing_progress_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = when (indexingProgress) {
                            is IndexingProgress.Completed -> MaterialTheme.colorScheme.primaryContainer
                            is IndexingProgress.Error -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (indexingProgress) {
                            is IndexingProgress.Scanning -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "正在扫描目录结构: ${indexingProgress.currentFolder}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            is IndexingProgress.Indexing -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "正在解析并建立索引 (${indexingProgress.currentCount}/${indexingProgress.totalCount})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = indexingProgress.currentFile,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                val progress = if (indexingProgress.totalCount > 0) {
                                    indexingProgress.currentCount.toFloat() / indexingProgress.totalCount
                                } else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            is IndexingProgress.Completed -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "索引构建完成！",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "已建立 ${indexingProgress.indexedCount} 个文档索引 (耗时 ${indexingProgress.elapsedMillis} ms)",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.dismissIndexingProgress() }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss")
                                    }
                                }
                            }
                            is IndexingProgress.Error -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "索引错误: ${indexingProgress.message}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    IconButton(onClick = { viewModel.dismissIndexingProgress() }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Section: Action Buttons (Add Folder, Add Files, Load Sample)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "添加搜索范围 (Search Scope)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DocFetcher 通过预先解析本地文档并建立全文索引提供毫秒级搜索体验。你可以选择手机内部存储文件夹或单个/多个文档进行索引。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f).testTag("add_folder_scope_button")
                        ) {
                            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("添加文件夹", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { filesPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f).testTag("add_files_scope_button")
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("添加文件", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.indexSampleKnowledgeBase() },
                        modifier = Modifier.fillMaxWidth().testTag("add_sample_docs_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("一键载入示例知识库 (TXT/MD/DOCX/PDF/CSV)")
                    }
                }
            }
        }

        // Section: Indexed Scopes List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已配置的搜索范围 (${scopes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (scopes.isNotEmpty() || documents.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("clear_all_index_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("清空全部索引", fontSize = 12.sp)
                    }
                }
            }
        }

        if (scopes.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "暂无索引范围，请点击上方按钮添加文件夹或载入示例文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(scopes) { scope ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (scope.isFolder) Icons.Default.Folder else Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = scope.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${scope.documentCount} 个文档 • 最近索引: ${formatDate(scope.lastIndexed)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { scopeToDelete = scope }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete scope",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Section: Indexed Documents Detail List
        if (documents.isNotEmpty()) {
            item {
                Text(
                    text = "索引文件库清单 (${documents.size} 个文档)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(documents) { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = getFormatColor(doc.fileExtension),
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                Text(
                                    text = doc.fileExtension.uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${doc.folderName} • ${formatFileSize(doc.fileSizeBytes)} • ${doc.wordCount} 字",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row {
                            TextButton(onClick = { viewModel.openDocumentPreview(doc) }) {
                                Text("预览", fontSize = 12.sp)
                            }
                            IconButton(onClick = { docToDelete = doc }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete file",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("清空全部索引？") },
            text = { Text("这将移除所有已索引的文件数据和搜索范围，但不会删除您设备上的原始文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    scopeToDelete?.let { scope ->
        AlertDialog(
            onDismissRequest = { scopeToDelete = null },
            title = { Text("移除搜索范围？") },
            text = { Text("确认从索引中移除 \"${scope.name}\"？该目录下的文档将被从搜索索引中删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScope(scope)
                        scopeToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { scopeToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("从索引中移除文档？") },
            text = { Text("确认从索引中删除 \"${doc.fileName}\"？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocument(doc.id)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}
