package com.baijum.ukufretboard.ui.songbook

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Resolves a human-readable file name for a document picked via the Storage
 * Access Framework.
 *
 * [Uri.lastPathSegment] is a provider-specific document ID, not a file name —
 * cloud providers such as OneDrive and Google Drive return opaque IDs that carry
 * no extension. Querying [OpenableColumns.DISPLAY_NAME] is the only reliable way
 * to recover the original name (issue #500).
 *
 * @param context Used to reach the [ContentResolver].
 * @param uri The document URI returned by the picker.
 * @return The display name, a best-effort name derived from the URI path, or
 *   `null` when neither yields anything usable.
 */
internal fun resolveDisplayName(
    context: Context,
    uri: Uri,
): String? {
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        val displayName = queryDisplayName(context, uri)
        if (!displayName.isNullOrBlank()) return displayName
    }
    // Fall back to the URI path. Document IDs like "primary:Download/Song.pro"
    // still yield a usable name once the path prefix is stripped.
    return uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() }
}

/**
 * Reads [OpenableColumns.DISPLAY_NAME] for a content URI.
 *
 * @param context Used to reach the [ContentResolver].
 * @param uri The document URI to query.
 * @return The display name, or `null` if the provider does not supply one.
 */
private fun queryDisplayName(
    context: Context,
    uri: Uri,
): String? =
    runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }
    }.getOrNull()
