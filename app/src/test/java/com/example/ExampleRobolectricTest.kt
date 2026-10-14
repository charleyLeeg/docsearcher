package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.IndexedDocument
import com.example.parser.DocumentParser
import com.example.search.SearchEngine
import com.example.search.SearchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DocFetcher", appName)
    }

    @Test
    fun `test word count logic`() {
        val text = "DocFetcher provides fast full-text search across documents."
        val count = DocumentParser.countWords(text)
        assertEquals(7, count)
    }

    @Test
    fun `test search engine all words matching`() {
        val doc1 = IndexedDocument(
            id = 1,
            uri = "file://doc1.txt",
            fileName = "Project_Specs.txt",
            folderName = "Projects",
            fileExtension = "txt",
            fileSizeBytes = 1024,
            lastModified = System.currentTimeMillis(),
            content = "This document describes the offline full-text search engine architecture.",
            wordCount = 9
        )
        val doc2 = IndexedDocument(
            id = 2,
            uri = "file://doc2.md",
            fileName = "Shopping_List.md",
            folderName = "Personal",
            fileExtension = "md",
            fileSizeBytes = 512,
            lastModified = System.currentTimeMillis(),
            content = "Apples, bananas, and oranges.",
            wordCount = 4
        )

        val results = SearchEngine.executeSearch(
            documents = listOf(doc1, doc2),
            query = "search engine",
            searchMode = SearchMode.ALL_WORDS
        )

        assertEquals(1, results.size)
        assertEquals("Project_Specs.txt", results[0].document.fileName)
        assertTrue(results[0].snippets.isNotEmpty())
    }
}
