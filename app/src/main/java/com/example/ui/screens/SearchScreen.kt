package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IndexedDocument
import com.example.data.model.SearchHistory
import com.example.search.SearchResultItem
import com.example.search.SortOption
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DocFetcherViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.components.SearchBarComponent

@Composable
fun SearchScreen(
    viewModel: DocFetcherViewModel,
    query: String,
    results: List<SearchResultItem>,
    allDocuments: List<IndexedDocument>,
    recentQueries: List<SearchHistory>,
    documentCount: Int,
    modifier: Modifier = Modifier
) {
    val searchMode by viewModel.searchMode.collectAsStateWithLifecycle()
    val selectedFormats by viewModel.selectedFormats.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val caseSensitive by viewModel.caseSensitive.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Sticky Search Bar & Format Filters
        SearchBarComponent(
            query = query,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            searchMode = searchMode,
            onSearchModeChange = { viewModel.setSearchMode(it) },
            selectedFormats = selectedFormats,
            onToggleFormat = { viewModel.toggleFormat(it) },
            onClearFormats = { viewModel.clearFormatFilters() },
            caseSensitive = caseSensitive,
            onToggleCaseSensitive = { viewModel.toggleCaseSensitive() }
        )

        if (query.isBlank()) {
            // Initial Empty Search State
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If 0 documents indexed, show prominent starter banner
                if (documentCount == 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("empty_index_starter_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "欢迎使用 DocFetcher 文件快搜",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Text(
                                    text = "当前本地索引为空。支持扫描系统文件/文件夹建立全文索引，或者立即生成包含 TXT、Markdown、Word DOCX、PDF、CSV 等格式的示例知识库进行测试！",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    lineHeight = 20.sp
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { viewModel.indexSampleKnowledgeBase() },
                                        modifier = Modifier.weight(1f).testTag("load_sample_kb_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("载入示例知识库", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Search Queries
                if (recentQueries.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "最近搜索",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.clearSearchHistory() },
                                    modifier = Modifier.testTag("clear_history_button")
                                ) {
                                    Text("清空", fontSize = 12.sp)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentQueries.forEach { history ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            viewModel.submitSearch(history.query)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = history.query,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        viewModel.removeSearchQuery(history.query)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Tips Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "搜索语法技巧 (Search Tips)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "• 全词与逻辑: 支持多词并列(AND)或任意词匹配(OR)\n" +
                                        "• 精准短语: 使用 Exact 模式搜索连贯长句\n" +
                                        "• 前缀通配: 支持词根前缀搜索，如搜索 'arch*' 匹配 architecture\n" +
                                        "• 正则匹配: 支持强大高级正则表达式 (Regex 模式)\n" +
                                        "• 格式支持: 原生深度解析 TXT、Markdown、DOCX、PDF、JSON、CSV",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // All Indexed Documents Overview
                if (allDocuments.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已索引文档 (${allDocuments.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "点击可直接预览",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    items(allDocuments) { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openDocumentPreview(doc) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = com.example.ui.components.getFormatColor(doc.fileExtension),
                                    modifier = Modifier.padding(end = 10.dp)
                                ) {
                                    Text(
                                        text = doc.fileExtension.uppercase(),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.fileName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${doc.folderName} • ${com.example.ui.components.formatFileSize(doc.fileSizeBytes)} • ${doc.wordCount} 字",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Search Results State
            Column(modifier = Modifier.fillMaxSize()) {
                // Results Header: Count & Sort Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalOccurrences = results.sumOf { it.totalMatches }
                    Text(
                        text = "找到 ${results.size} 个文件 (${totalOccurrences} 处匹配)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Sort Dropdown
                    Box {
                        TextButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = sortOption.displayName, fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (results.isEmpty()) {
                    // Empty Results State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "未找到包含 \"$query\" 的文档",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "建议尝试其他关键词，或者检查是否切换为 OR (任意词) 模式、通配符模式或重置格式筛选",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    // Results List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = results,
                            key = { it.document.id }
                        ) { resultItem ->
                            DocumentCard(
                                result = resultItem,
                                query = query,
                                onClick = { viewModel.openDocumentPreview(resultItem.document) }
                            )
                        }
                    }
                }
            }
        }
    }
}
