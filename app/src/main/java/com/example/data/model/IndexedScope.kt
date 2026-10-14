package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "indexed_scopes",
    indices = [
        Index(value = ["uri"], unique = true)
    ]
)
data class IndexedScope(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val name: String,
    val isFolder: Boolean,
    val documentCount: Int = 0,
    val lastIndexed: Long = System.currentTimeMillis()
)
