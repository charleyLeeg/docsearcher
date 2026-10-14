package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IndexedDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY lastModified DESC")
    fun getAllDocuments(): Flow<List<IndexedDocument>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Long): Flow<IndexedDocument?>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentByIdOnce(id: Long): IndexedDocument?

    @Query("SELECT * FROM documents WHERE uri = :uri LIMIT 1")
    suspend fun getDocumentByUri(uri: String): IndexedDocument?

    @Query("SELECT * FROM documents WHERE content LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%'")
    fun searchSimple(query: String): Flow<List<IndexedDocument>>

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT SUM(wordCount) FROM documents")
    fun getTotalWordCount(): Flow<Long?>

    @Query("SELECT SUM(fileSizeBytes) FROM documents")
    fun getTotalFileSizeBytes(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: IndexedDocument): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<IndexedDocument>)

    @Update
    suspend fun updateDocument(document: IndexedDocument)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM documents WHERE uri LIKE :scopePrefix || '%'")
    suspend fun deleteByScopeUriPrefix(scopePrefix: String)

    @Query("DELETE FROM documents WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM documents")
    suspend fun clearAll()
}
