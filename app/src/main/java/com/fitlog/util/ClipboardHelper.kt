package com.fitlog.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun copyToClipboard(label: String, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }
}
