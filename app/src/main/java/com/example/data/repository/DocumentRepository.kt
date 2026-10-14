package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.AppDatabase
import com.example.data.model.IndexedDocument
import com.example.data.model.IndexedScope
import com.example.data.model.SearchHistory
import com.example.parser.DocumentParser
import com.example.parser.SampleDocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

sealed class IndexingProgress {
    object Idle : IndexingProgress()
    data class Scanning(val currentFolder: String) : IndexingProgress()
    data class Indexing(val currentFile: String, val currentCount: Int, val totalCount: Int) : IndexingProgress()
    data class Completed(val indexedCount: Int, val elapsedMillis: Long) : IndexingProgress()
    data class Error(val message: String) : IndexingProgress()
}

class DocumentRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val documentDao = database.documentDao()
    private val scopeDao = database.scopeDao()
    private val searchHistoryDao = database.searchHistoryDao()

    val allDocuments: Flow<List<IndexedDocument>> = documentDao.getAllDocuments()
    val allScopes: Flow<List<IndexedScope>> = scopeDao.getAllScopes()
    val recentQueries: Flow<List<SearchHistory>> = searchHistoryDao.getRecentQueries()
    val documentCount: Flow<Int> = documentDao.getDocumentCount()
    val totalWordCount: Flow<Long?> = documentDao.getTotalWordCount()
    val totalFileSizeBytes: Flow<Long?> = documentDao.getTotalFileSizeBytes()

    suspend fun getDocumentById(id: Long): IndexedDocument? {
        return withContext(Dispatchers.IO) {
            documentDao.getDocumentByIdOnce(id)
        }
    }

    suspend fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        withContext(Dispatchers.IO) {
            searchHistoryDao.insertQuery(SearchHistory(query.trim()))
        }
    }

    suspend fun removeSearchQuery(query: String) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteQuery(query)
        }
    }

    suspend fun clearSearchHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.clearHistory()
        }
    }

    suspend fun deleteDocument(id: Long) {
        withContext(Dispatchers.IO) {
            documentDao.deleteDocumentById(id)
        }
    }

    suspend fun deleteScope(scope: IndexedScope) {
        withContext(Dispatchers.IO) {
            documentDao.deleteByScopeUriPrefix(scope.uri)
            scopeDao.deleteScopeById(scope.id)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            documentDao.clearAll()
            scopeDao.clearAllScopes()
            searchHistoryDao.clearHistory()
        }
    }

    suspend fun indexSampleKnowledgeBase(
        onProgress: (IndexingProgress) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            onProgress(IndexingProgress.Scanning("Sample Knowledge Base"))

            val sampleFiles = SampleDocumentGenerator.generateSampleDocuments(context)
            var count = 0

            val indexedDocs = mutableListOf<IndexedDocument>()
            for (file in sampleFiles) {
                count++
                onProgress(IndexingProgress.Indexing(file.name, count, sampleFiles.size))

                val ext = DocumentParser.getExtension(file.name)
                val content = try {
                    file.inputStream().use { stream ->
                        DocumentParser.parseStream(stream, ext)
                    }
                } catch (e: Exception) {
                    "Error extracting content: ${e.message}"
                }
                val wordCount = DocumentParser.countWords(content)

                indexedDocs.add(
                    IndexedDocument(
                        uri = file.toURI().toString(),
                        fileName = file.name,
                        folderName = "Sample Knowledge Base",
                        fileExtension = ext,
                        fileSizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        content = content,
                        wordCount = wordCount,
                        isSample = true
                    )
                )
            }

            documentDao.insertDocuments(indexedDocs)

            // Register scope
            scopeDao.insertScope(
                IndexedScope(
                    uri = "sample://knowledge_base",
                    name = "Sample Knowledge Base",
                    isFolder = true,
                    documentCount = indexedDocs.size,
                    lastIndexed = System.currentTimeMillis()
                )
            )

            val elapsed = System.currentTimeMillis() - startTime
            onProgress(IndexingProgress.Completed(indexedDocs.size, elapsed))
        }
    }

    suspend fun indexFolderUri(
        treeUri: Uri,
        onProgress: (IndexingProgress) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                // Request persistable permission
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                } catch (_: Exception) {}

                val folderName = getFolderDisplayName(treeUri)
                onProgress(IndexingProgress.Scanning(folderName))

                // Traverse folder
                val foundFiles = mutableListOf<DiscoveredFile>()
                traverseDirectory(treeUri, DocumentsContract.getTreeDocumentId(treeUri), folderName, foundFiles)

                var count = 0
                val total = foundFiles.size
                val indexedDocs = mutableListOf<IndexedDocument>()

                for (fileInfo in foundFiles) {
                    count++
                    onProgress(IndexingProgress.Indexing(fileInfo.name, count, total))

                    val extracted = DocumentParser.parse(context, fileInfo.uri, fileInfo.name)
                    indexedDocs.add(
                        IndexedDocument(
                            uri = fileInfo.uri.toString(),
                            fileName = fileInfo.name,
                            folderName = fileInfo.parentFolder,
                            fileExtension = extracted.fileExtension,
                            fileSizeBytes = fileInfo.size,
                            lastModified = fileInfo.lastModified,
                            content = extracted.content,
                            wordCount = extracted.wordCount,
                            isSample = false
                        )
                    )
                }

                if (indexedDocs.isNotEmpty()) {
                    documentDao.insertDocuments(indexedDocs)
                }

                scopeDao.insertScope(
                    IndexedScope(
                        uri = treeUri.toString(),
                        name = folderName,
                        isFolder = true,
                        documentCount = indexedDocs.size,
                        lastIndexed = System.currentTimeMillis()
                    )
                )

                val elapsed = System.currentTimeMillis() - startTime
                onProgress(IndexingProgress.Completed(indexedDocs.size, elapsed))
            } catch (e: Exception) {
                onProgress(IndexingProgress.Error(e.localizedMessage ?: "Failed to index folder"))
            }
        }
    }

    suspend fun indexIndividualFiles(
        uris: List<Uri>,
        onProgress: (IndexingProgress) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val total = uris.size
            var count = 0
            val indexedDocs = mutableListOf<IndexedDocument>()

            for (uri in uris) {
                count++
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) {}

                val fileInfo = getSingleFileInfo(uri)
                onProgress(IndexingProgress.Indexing(fileInfo.name, count, total))

                val extracted = DocumentParser.parse(context, uri, fileInfo.name)
                indexedDocs.add(
                    IndexedDocument(
                        uri = uri.toString(),
                        fileName = fileInfo.name,
                        folderName = "Selected Files",
                        fileExtension = extracted.fileExtension,
                        fileSizeBytes = fileInfo.size,
                        lastModified = fileInfo.lastModified,
                        content = extracted.content,
                        wordCount = extracted.wordCount,
                        isSample = false
                    )
                )
            }

            if (indexedDocs.isNotEmpty()) {
                documentDao.insertDocuments(indexedDocs)
                scopeDao.insertScope(
                    IndexedScope(
                        uri = "files://" + System.currentTimeMillis(),
                        name = "File Batch (${indexedDocs.size} files)",
                        isFolder = false,
                        documentCount = indexedDocs.size,
                        lastIndexed = System.currentTimeMillis()
                    )
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            onProgress(IndexingProgress.Completed(indexedDocs.size, elapsed))
        }
    }

    private fun traverseDirectory(
        treeUri: Uri,
        parentDocumentId: String,
        parentFolderName: String,
        resultList: MutableList<DiscoveredFile>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
        cursor?.use {
            val idIdx = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modIdx = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (it.moveToNext()) {
                val docId = if (idIdx != -1) it.getString(idIdx) else null ?: continue
                val name = if (nameIdx != -1) it.getString(nameIdx) else null ?: "Unknown"
                val mime = if (mimeIdx != -1) it.getString(mimeIdx) else null ?: ""
                val size = if (sizeIdx != -1 && !it.isNull(sizeIdx)) it.getLong(sizeIdx) else 0L
                val lastMod = if (modIdx != -1 && !it.isNull(modIdx)) it.getLong(modIdx) else System.currentTimeMillis()

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    // Recurse subfolder
                    traverseDirectory(treeUri, docId, name, resultList)
                } else {
                    val ext = DocumentParser.getExtension(name)
                    if (DocumentParser.isSupported(ext)) {
                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        resultList.add(
                            DiscoveredFile(
                                uri = fileDocUri,
                                name = name,
                                parentFolder = parentFolderName,
                                size = size,
                                lastModified = lastMod
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getFolderDisplayName(uri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val parts = docId.split(":")
            if (parts.size > 1 && parts[1].isNotBlank()) {
                parts[1].substringAfterLast('/')
            } else {
                "Selected Folder"
            }
        } catch (_: Exception) {
            "Selected Folder"
        }
    }

    private fun getSingleFileInfo(uri: Uri): DiscoveredFile {
        var name = "Document"
        var size = 0L
        var lastModified = System.currentTimeMillis()

        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex) ?: name
                    }
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !it.isNull(sizeIndex)) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        return DiscoveredFile(
            uri = uri,
            name = name,
            parentFolder = "Selected Files",
            size = size,
            lastModified = lastModified
        )
    }

    private data class DiscoveredFile(
        val uri: Uri,
        val name: String,
        val parentFolder: String,
        val size: Long,
        val lastModified: Long
    )
}
