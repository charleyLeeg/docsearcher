package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.DocFetcherViewModel
import com.example.ui.screens.DocumentPreviewScreen
import com.example.ui.screens.IndexManagerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StatsSettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DocFetcherApp()
            }
        }
    }
}

@Composable
fun DocFetcherApp(
    viewModel: DocFetcherViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allDocuments by viewModel.allDocuments.collectAsStateWithLifecycle()
    val allScopes by viewModel.allScopes.collectAsStateWithLifecycle()
    val recentQueries by viewModel.recentQueries.collectAsStateWithLifecycle()
    val documentCount by viewModel.documentCount.collectAsStateWithLifecycle()
    val totalWordCount by viewModel.totalWordCount.collectAsStateWithLifecycle()
    val totalSizeBytes by viewModel.totalFileSizeBytes.collectAsStateWithLifecycle()
    val indexingProgress by viewModel.indexingProgress.collectAsStateWithLifecycle()
    val selectedDocument by viewModel.selectedDocument.collectAsStateWithLifecycle()

    if (currentScreen == AppScreen.PREVIEW && selectedDocument != null) {
        DocumentPreviewScreen(
            document = selectedDocument!!,
            searchQuery = searchQuery,
            onBack = { viewModel.closeDocumentPreview() }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.SEARCH,
                        onClick = { viewModel.navigateTo(AppScreen.SEARCH) },
                        icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("搜索") },
                        modifier = Modifier.testTag("nav_search_tab"),
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.INDEX_MANAGER,
                        onClick = { viewModel.navigateTo(AppScreen.INDEX_MANAGER) },
                        icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "Scopes") },
                        label = { Text("搜索范围") },
                        modifier = Modifier.testTag("nav_scopes_tab"),
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.STATS,
                        onClick = { viewModel.navigateTo(AppScreen.STATS) },
                        icon = { Icon(imageVector = Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text("统计与指标") },
                        modifier = Modifier.testTag("nav_stats_tab"),
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        AppScreen.SEARCH -> SearchScreen(
                            viewModel = viewModel,
                            query = searchQuery,
                            results = searchResults,
                            allDocuments = allDocuments,
                            recentQueries = recentQueries,
                            documentCount = documentCount
                        )
                        AppScreen.INDEX_MANAGER -> IndexManagerScreen(
                            viewModel = viewModel,
                            scopes = allScopes,
                            documents = allDocuments,
                            indexingProgress = indexingProgress
                        )
                        AppScreen.STATS -> StatsSettingsScreen(
                            viewModel = viewModel,
                            documents = allDocuments,
                            totalWords = totalWordCount,
                            totalBytes = totalSizeBytes
                        )
                        AppScreen.PREVIEW -> {
                            // Handled by outer condition
                        }
                    }
                }
            }
        }
    }
}
