package com.olegbelyanin.expensetracker.ui.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.olegbelyanin.expensetracker.R

fun shareExportedDocument(context: Context, uri: String, mimeType: String) {
    val parsed = Uri.parse(uri)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, parsed)
            clipData = ClipData.newRawUri("", parsed)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    val chooser =
        Intent.createChooser(send, context.getString(R.string.share)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(chooser)
}
