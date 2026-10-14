package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["fileName"]),
        Index(value = ["fileExtension"]),
        Index(value = ["folderName"])
    ]
)
data class IndexedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val fileName: String,
    val folderName: String,
    val fileExtension: String,
    val fileSizeBytes: Long,
    val lastModified: Long,
    val content: String,
    val wordCount: Int,
    val indexedAt: Long = System.currentTimeMillis(),
    val isSample: Boolean = false
)
