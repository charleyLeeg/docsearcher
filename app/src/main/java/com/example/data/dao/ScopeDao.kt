package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IndexedScope
import kotlinx.coroutines.flow.Flow

@Dao
interface ScopeDao {

    @Query("SELECT * FROM indexed_scopes ORDER BY lastIndexed DESC")
    fun getAllScopes(): Flow<List<IndexedScope>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScope(scope: IndexedScope): Long

    @Update
    suspend fun updateScope(scope: IndexedScope)

    @Query("DELETE FROM indexed_scopes WHERE id = :id")
    suspend fun deleteScopeById(id: Long)

    @Query("DELETE FROM indexed_scopes WHERE uri = :uri")
    suspend fun deleteScopeByUri(uri: String)

    @Query("DELETE FROM indexed_scopes")
    suspend fun clearAllScopes()
}
