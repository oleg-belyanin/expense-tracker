package com.olegbelyanin.expensetracker.data.files

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

interface SettingsDocumentStore {
    suspend fun writeText(uri: String, text: String)

    suspend fun readText(uri: String): String
}

class AndroidSettingsDocumentStore(
    private val resolver: ContentResolver,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : SettingsDocumentStore {
    override suspend fun writeText(uri: String, text: String) {
        withContext(io) {
            val parsed = Uri.parse(uri)
            resolver.openOutputStream(parsed, "wt")?.use { stream ->
                stream.writer(StandardCharsets.UTF_8).use { writer ->
                    writer.write(text)
                }
            } ?: error("missing-output")
            takePersistableRead(parsed)
        }
    }

    override suspend fun readText(uri: String): String = withContext(io) {
        val parsed = Uri.parse(uri)
        resolver.openInputStream(parsed)?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
            reader.readText()
        } ?: error("missing-input")
    }

    private fun takePersistableRead(uri: Uri) {
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }
}
