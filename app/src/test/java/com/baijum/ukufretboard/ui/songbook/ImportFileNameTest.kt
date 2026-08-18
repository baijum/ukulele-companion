package com.baijum.ukufretboard.ui.songbook

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Covers filename resolution for documents picked via the Storage Access
 * Framework — the code path behind issue #500, where a OneDrive content URI
 * ended up in the song title.
 */
@RunWith(RobolectricTestRunner::class)
class ImportFileNameTest {
    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Robolectric.buildContentProvider(FakeDocumentsProvider::class.java).create(AUTHORITY)
    }

    @After
    fun tearDown() {
        FakeDocumentsProvider.reset()
    }

    // ── DISPLAY_NAME available ───────────────────────────────────────

    @Test
    fun usesDisplayNameWhenProviderSuppliesOne() {
        FakeDocumentsProvider.displayName = "Heart-of-Gold.pro"
        assertEquals("Heart-of-Gold.pro", resolveDisplayName(context, cloudUri()))
    }

    @Test
    fun displayNameWinsOverTheOpaqueDocumentId() {
        // The regression: lastPathSegment here is a meaningless ID, and
        // lastPathSegment decodes it, so it is not even the provider's own ID.
        FakeDocumentsProvider.displayName = "Heart-of-Gold.pro"
        val resolved = resolveDisplayName(context, cloudUri())
        assertEquals("Heart-of-Gold.pro", resolved)
        assertEquals(OPAQUE_ID, cloudUri().lastPathSegment)
    }

    // ── DISPLAY_NAME unavailable — fall back to the path ─────────────

    @Test
    fun fallsBackToPathWhenProviderReturnsNoRows() {
        FakeDocumentsProvider.returnEmptyCursor = true
        assertEquals(OPAQUE_ID, resolveDisplayName(context, cloudUri()))
    }

    @Test
    fun fallsBackToPathWhenProviderReturnsNullCursor() {
        FakeDocumentsProvider.displayName = null
        assertEquals(OPAQUE_ID, resolveDisplayName(context, cloudUri()))
    }

    @Test
    fun fallsBackToPathWhenDisplayNameIsBlank() {
        FakeDocumentsProvider.displayName = "   "
        assertEquals(OPAQUE_ID, resolveDisplayName(context, cloudUri()))
    }

    @Test
    fun fallsBackToPathWhenProviderThrows() {
        FakeDocumentsProvider.throwOnQuery = true
        assertEquals(OPAQUE_ID, resolveDisplayName(context, cloudUri()))
    }

    @Test
    fun fallbackStripsDocumentIdPathPrefix() {
        FakeDocumentsProvider.displayName = null
        val uri = Uri.parse("content://$AUTHORITY/document/primary%3ADownload%2FSong.pro")
        assertEquals("Song.pro", resolveDisplayName(context, uri))
    }

    // ── Non-content URIs ─────────────────────────────────────────────

    @Test
    fun fileUriUsesItsLastPathSegmentWithoutQueryingTheProvider() {
        assertEquals("Song.pro", resolveDisplayName(context, Uri.parse("file:///tmp/Song.pro")))
    }

    @Test
    fun uriWithoutAPathResolvesToNull() {
        FakeDocumentsProvider.displayName = null
        assertNull(resolveDisplayName(context, Uri.parse("content://$AUTHORITY")))
    }

    private fun cloudUri(): Uri = Uri.parse("content://$AUTHORITY/items/$ENCODED_ID")

    /** Stands in for a cloud DocumentsProvider such as OneDrive or Drive. */
    class FakeDocumentsProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? {
            if (throwOnQuery) throw UnsupportedOperationException("provider unavailable")
            val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
            if (returnEmptyCursor) return cursor
            val name = displayName ?: return null
            cursor.addRow(arrayOf(name))
            return cursor
        }

        override fun getType(uri: Uri): String? = "text/plain"

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        companion object {
            var displayName: String? = null
            var returnEmptyCursor: Boolean = false
            var throwOnQuery: Boolean = false

            fun reset() {
                displayName = null
                returnEmptyCursor = false
                throwOnQuery = false
            }
        }
    }

    private companion object {
        const val AUTHORITY = "com.microsoft.skydrive.content.metadata"

        /** Percent-encoded document ID as it appears in the URI. */
        const val ENCODED_ID = "4A2B9C1D%21107"

        /** The same ID as [Uri.getLastPathSegment] returns it — decoded. */
        const val OPAQUE_ID = "4A2B9C1D!107"
    }
}
