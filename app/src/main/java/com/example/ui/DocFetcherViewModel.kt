package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.IndexedDocument
import com.example.data.model.IndexedScope
import com.example.data.model.SearchHistory
import com.example.data.repository.DocumentRepository
import com.example.data.repository.IndexingProgress
import com.example.search.SearchEngine
import com.example.search.SearchMode
import com.example.search.SearchResultItem
import com.example.search.SortOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    SEARCH,
    PREVIEW,
    INDEX_MANAGER,
    STATS
}

class DocFetcherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = DocumentRepository(database, application)
    }

    private val _currentScreen = MutableStateFlow(AppScreen.SEARCH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.ALL_WORDS)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _selectedFormats = MutableStateFlow<Set<String>>(emptySet())
    val selectedFormats: StateFlow<Set<String>> = _selectedFormats.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _caseSensitive = MutableStateFlow(false)
    val caseSensitive: StateFlow<Boolean> = _caseSensitive.asStateFlow()

    private val _indexingProgress = MutableStateFlow<IndexingProgress>(IndexingProgress.Idle)
    val indexingProgress: StateFlow<IndexingProgress> = _indexingProgress.asStateFlow()

    private val _selectedDocument = MutableStateFlow<IndexedDocument?>(null)
    val selectedDocument: StateFlow<IndexedDocument?> = _selectedDocument.asStateFlow()

    val allDocuments: StateFlow<List<IndexedDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScopes: StateFlow<List<IndexedScope>> = repository.allScopes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentQueries: StateFlow<List<SearchHistory>> = repository.recentQueries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentCount: StateFlow<Int> = repository.documentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalWordCount: StateFlow<Long> = repository.totalWordCount
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalFileSizeBytes: StateFlow<Long> = repository.totalFileSizeBytes
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private data class SearchCriteria(
        val query: String,
        val mode: SearchMode,
        val formats: Set<String>,
        val sort: SortOption,
        val caseSensitive: Boolean
    )

    private val searchCriteria = combine(
        _searchQuery,
        _searchMode,
        _selectedFormats,
        _sortOption,
        _caseSensitive
    ) { query, mode, formats, sort, caseSens ->
        SearchCriteria(query, mode, formats, sort, caseSens)
    }

    // Reactive search results combination
    val searchResults: StateFlow<List<SearchResultItem>> = combine(
        allDocuments,
        searchCriteria
    ) { docs, criteria ->
        if (criteria.query.isBlank()) {
            emptyList()
        } else {
            SearchEngine.executeSearch(
                documents = docs,
                query = criteria.query,
                searchMode = criteria.mode,
                selectedFormats = criteria.formats,
                sortOption = criteria.sort,
                caseSensitive = criteria.caseSensitive
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var debounceJob: Job? = null

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        debounceJob?.cancel()
        if (newQuery.isNotBlank()) {
            debounceJob = viewModelScope.launch {
                delay(1200)
                repository.addSearchQuery(newQuery)
            }
        }
    }

    fun submitSearch(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.addSearchQuery(query)
            }
        }
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
    }

    fun toggleFormat(format: String) {
        val current = _selectedFormats.value.toMutableSet()
        if (current.contains(format)) {
            current.remove(format)
        } else {
            current.add(format)
        }
        _selectedFormats.value = current
    }

    fun clearFormatFilters() {
        _selectedFormats.value = emptySet()
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleCaseSensitive() {
        _caseSensitive.value = !_caseSensitive.value
    }

    fun removeSearchQuery(query: String) {
        viewModelScope.launch {
            repository.removeSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            if (_selectedDocument.value?.id == id) {
                _selectedDocument.value = null
                if (_currentScreen.value == AppScreen.PREVIEW) {
                    _currentScreen.value = AppScreen.SEARCH
                }
            }
        }
    }

    fun deleteScope(scope: IndexedScope) {
        viewModelScope.launch {
            repository.deleteScope(scope)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedDocument.value = null
            _currentScreen.value = AppScreen.SEARCH
        }
    }

    fun indexSampleKnowledgeBase() {
        viewModelScope.launch {
            repository.indexSampleKnowledgeBase { progress ->
                _indexingProgress.value = progress
            }
        }
    }

    fun indexFolder(uri: Uri) {
        viewModelScope.launch {
            repository.indexFolderUri(uri) { progress ->
                _indexingProgress.value = progress
            }
        }
    }

    fun indexFiles(uris: List<Uri>) {
        viewModelScope.launch {
            repository.indexIndividualFiles(uris) { progress ->
                _indexingProgress.value = progress
            }
        }
    }

    fun dismissIndexingProgress() {
        _indexingProgress.value = IndexingProgress.Idle
    }

    fun openDocumentPreview(document: IndexedDocument) {
        _selectedDocument.value = document
        _currentScreen.value = AppScreen.PREVIEW
    }

    fun closeDocumentPreview() {
        _selectedDocument.value = null
        _currentScreen.value = AppScreen.SEARCH
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }
}
