package com.baijum.ukufretboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

/**
 * Platform-specific sharing and clipboard utilities.
 *
 * Extracted from [com.baijum.ukufretboard.domain.ChordSheetFormatter]
 * so that the domain package remains free of Android framework imports.
 */
object ShareUtils {

    /**
     * Shares text content via Android's share sheet.
     *
     * @param context Android context.
     * @param title Title for the share chooser.
     * @param text The text content to share.
     */
    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    /**
     * Copies text to the Android clipboard.
     *
     * @param context Android context.
     * @param label Label for the clipboard entry.
     * @param text The text to copy.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
            as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
